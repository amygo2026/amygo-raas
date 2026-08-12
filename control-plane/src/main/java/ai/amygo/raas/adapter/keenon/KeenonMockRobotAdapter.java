package ai.amygo.raas.adapter.keenon;

import ai.amygo.raas.adapter.AdapterDescriptor;
import ai.amygo.raas.adapter.RobotAdapter;
import ai.amygo.raas.adapter.support.InProcessDeliveryExecutor;
import ai.amygo.raas.application.EventSequenceGuard;
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
 * KEENON Mock Adapter (support level: Mock).
 * Partner API fields are unknown until formal docs — do not invent them here.
 */
@Component
public class KeenonMockRobotAdapter implements RobotAdapter {
    public static final String TYPE = "KEENON";

    private final InMemoryStore store;
    private final InProcessDeliveryExecutor executor;

    public KeenonMockRobotAdapter(
            InMemoryStore store,
            EventSequenceGuard sequenceGuard,
            @Value("${raas.simulator.progress-delay-ms:150}") long progressDelayMs
    ) {
        this.store = store;
        this.executor = new InProcessDeliveryExecutor(
                "KEENON_MOCK",
                progressDelayMs,
                null,
                id -> sequenceGuard.currentWatermark("KEENON_MOCK", id)
        );
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor(TYPE, "0.1.0-mock", "Mock");
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
            return new CommandReceipt(prior, CommandReceiptStatus.ACCEPTED, "IDEMPOTENT_REPLAY", "same idempotencyKey");
        }
        if (!"CANCEL".equals(command.commandType()) && !capabilities(command.robotId()).contains(mapCapability(command.commandType()))) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                    "CAPABILITY_NOT_SUPPORTED", "KEENON mock does not claim " + command.commandType() + " without formal docs");
        }
        if ("DELIVERY_START".equals(command.commandType()) || "DELIVER".equals(command.commandType())
                || "CLEAN".equals(command.commandType()) || "CLEANING_START".equals(command.commandType())
                || "HOTEL_DELIVERY_START".equals(command.commandType())
                || "RETURN_TO_DOCK".equals(command.commandType())) {
            executor.startDelivery(command);
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "accepted by KEENON mock");
        }
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                "CAPABILITY_NOT_SUPPORTED", command.commandType());
    }

    @Override
    public CommandReceipt cancel(CommandEnvelope command) {
        executor.emitCancel(command);
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "cancel accepted by KEENON mock");
    }

    @Override
    public void subscribe(Consumer<RobotEvent> listener) {
        executor.subscribe(listener);
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
