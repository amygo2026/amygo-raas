package ai.amygo.raas.adapter.pudu;

import ai.amygo.raas.adapter.AdapterDescriptor;
import ai.amygo.raas.adapter.RobotAdapter;
import ai.amygo.raas.adapter.support.InProcessDeliveryExecutor;
import ai.amygo.raas.adapter.support.MockFaultMode;
import ai.amygo.raas.application.EventSequenceGuard;
import ai.amygo.raas.domain.robot.ConnectivityStatus;
import ai.amygo.raas.domain.robot.RobotSnapshot;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.CommandReceiptStatus;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.InMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

/**
 * PUDU Mock Adapter (support level: Mock).
 * Fault modes: none | timeout_unknown | disconnect_mid_mission | duplicate_callback.
 * No invented vendor REST/SDK fields.
 */
@Component
public class PuduMockRobotAdapter implements RobotAdapter {
    public static final String TYPE = "PUDU";

    private final InMemoryStore store;
    private final InProcessDeliveryExecutor executor;
    private final MockFaultMode faultMode;

    public PuduMockRobotAdapter(
            InMemoryStore store,
            EventSequenceGuard sequenceGuard,
            @Value("${raas.simulator.progress-delay-ms:150}") long progressDelayMs,
            @Value("${raas.pudu-mock.fault-mode:none}") String faultModeRaw
    ) {
        this.store = store;
        this.faultMode = MockFaultMode.from(faultModeRaw);
        this.executor = new InProcessDeliveryExecutor(
                "PUDU_MOCK",
                progressDelayMs,
                this.faultMode,
                id -> sequenceGuard.currentWatermark("PUDU_MOCK", id)
        );
        this.executor.subscribe(this::onInternalEvent);
    }

    public MockFaultMode faultMode() {
        return faultMode;
    }

    public String commandOutcome(String commandId) {
        return executor.commandOutcome(commandId);
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor(TYPE, "0.2.0-mock", "Mock");
    }

    @Override
    public Set<String> capabilities(String robotId) {
        return Set.of("navigation", "delivery", "cleaning", "docking");
    }

    @Override
    public RobotSnapshot getSnapshot(String robotId) {
        return store.findRobot(robotId)
                .map(r -> r.toSnapshot())
                .orElseThrow(() -> new IllegalArgumentException("Unknown robot " + robotId));
    }

    @Override
    public CommandReceipt submit(CommandEnvelope command) {
        if (command.expiresAt().isBefore(Instant.now())) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED, "COMMAND_EXPIRED", "expiresAt passed");
        }
        String prior = executor.rememberIdempotency(command.idempotencyKey(), command.commandId());
        if (prior != null) {
            String outcome = executor.commandOutcome(prior);
            if ("UNKNOWN".equals(outcome)) {
                return new CommandReceipt(prior, CommandReceiptStatus.UNKNOWN, "COMMAND_STATUS_UNKNOWN", "idempotent unknown replay");
            }
            return new CommandReceipt(prior, CommandReceiptStatus.ACCEPTED, "IDEMPOTENT_REPLAY", "same idempotencyKey");
        }
        if (!"CANCEL".equals(command.commandType()) && !capabilities(command.robotId()).contains(mapCapability(command.commandType()))) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                    "CAPABILITY_NOT_SUPPORTED", "PUDU mock does not claim " + command.commandType() + " without formal docs");
        }
        if ("DELIVERY_START".equals(command.commandType()) || "DELIVER".equals(command.commandType())
                || "CLEAN".equals(command.commandType()) || "CLEANING_START".equals(command.commandType())
                || "HOTEL_DELIVERY_START".equals(command.commandType())
                || "RETURN_TO_DOCK".equals(command.commandType())) {
            if (faultMode == MockFaultMode.TIMEOUT_UNKNOWN) {
                executor.startDelivery(command);
                return new CommandReceipt(command.commandId(), CommandReceiptStatus.UNKNOWN,
                        "COMMAND_STATUS_UNKNOWN", "simulated submit timeout — do not blind-retry");
            }
            executor.startDelivery(command);
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "accepted by PUDU mock");
        }
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                "CAPABILITY_NOT_SUPPORTED", command.commandType());
    }

    @Override
    public CommandReceipt cancel(CommandEnvelope command) {
        executor.emitCancel(command);
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "cancel accepted by PUDU mock");
    }

    @Override
    public void subscribe(Consumer<RobotEvent> listener) {
        executor.subscribe(listener);
    }

    private void onInternalEvent(RobotEvent event) {
        if ("robot.connectivity.changed".equals(event.eventType())) {
            store.findRobot(event.robotId()).ifPresent(r -> {
                r.setConnectivityStatus(ConnectivityStatus.OFFLINE);
                store.saveRobot(r);
            });
        }
    }

    private static String mapCapability(String commandType) {
        return switch (commandType) {
            case "DELIVERY_START", "DELIVER", "HOTEL_DELIVERY_START" -> "delivery";
            case "CLEAN", "CLEANING_START" -> "cleaning";
            case "RETURN_TO_DOCK" -> "docking";
            case "OPEN_COMPARTMENT" -> "compartment";
            default -> commandType.toLowerCase();
        };
    }
}
