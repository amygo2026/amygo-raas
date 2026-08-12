package ai.amygo.raas.adapter.simulator;

import ai.amygo.raas.adapter.AdapterDescriptor;
import ai.amygo.raas.adapter.RobotAdapter;
import ai.amygo.raas.domain.robot.RobotSnapshot;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.CommandReceiptStatus;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.InMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * In-process simulator adapter with fault-injection modes.
 * No vendor SDK. Profiles: sim.delivery.v1 / sim.cleaning.v1 / sim.hotel.v1.
 */
@Component
public class SimulatorRobotAdapter implements RobotAdapter {
    private static final Logger log = LoggerFactory.getLogger(SimulatorRobotAdapter.class);

    private final InMemoryStore store;
    private final String faultMode;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<RobotEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, String> acceptedIdempotency = new ConcurrentHashMap<>();

    public SimulatorRobotAdapter(
            InMemoryStore store,
            @Value("${raas.simulator.fault-mode:none}") String faultMode
    ) {
        this.store = store;
        this.faultMode = faultMode == null ? "none" : faultMode;
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("SIMULATOR", "0.1.0", "Simulator");
    }

    @Override
    public Set<String> capabilities(String robotId) {
        return Set.of("navigation", "delivery", "cleaning", "docking", "compartment");
    }

    @Override
    public RobotSnapshot getSnapshot(String robotId) {
        return store.findRobot(robotId)
                .map(r -> r.toSnapshot())
                .orElseThrow(() -> new IllegalArgumentException("Unknown robot " + robotId));
    }

    @Override
    public CommandReceipt submit(CommandEnvelope command) {
        if (command.expiresAt().isBefore(Instant.now()) || "expire".equalsIgnoreCase(faultMode)) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED, "COMMAND_EXPIRED", "expiresAt passed");
        }
        String prior = acceptedIdempotency.putIfAbsent(command.idempotencyKey(), command.commandId());
        if (prior != null) {
            return new CommandReceipt(prior, CommandReceiptStatus.ACCEPTED, "IDEMPOTENT_REPLAY", "same idempotencyKey");
        }

        if ("fail_on_submit".equalsIgnoreCase(faultMode)) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                    "VENDOR_TEMPORARY_ERROR", "simulator fault_mode=fail_on_submit");
        }

        String capability = mapCapability(command.commandType());
        if (!"CANCEL".equals(command.commandType()) && !capabilities(command.robotId()).contains(capability)) {
            return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                    "CAPABILITY_NOT_SUPPORTED", command.commandType());
        }

        emit(command, "command.accepted", Map.of("commandType", command.commandType()), null);
        scheduler.schedule(() -> simulateProgress(command), 200, TimeUnit.MILLISECONDS);
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "accepted by simulator");
    }

    @Override
    public CommandReceipt cancel(CommandEnvelope command) {
        emit(command, "command.accepted", Map.of("commandType", "CANCEL"), null);
        emit(command, "task.canceled", Map.of("reason", "operator_or_system_cancel"), null);
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "cancel accepted");
    }

    @Override
    public void subscribe(Consumer<RobotEvent> listener) {
        listeners.add(listener);
    }

    private void simulateProgress(CommandEnvelope command) {
        try {
            if ("duplicate_events".equalsIgnoreCase(faultMode)) {
                RobotEvent running = buildEvent(command, "task.running", Map.of("progress", 10), null);
                publish(running);
                publish(running); // duplicate same eventId must be ignored by control plane
            } else if ("out_of_order".equalsIgnoreCase(faultMode)) {
                // emit completed before running (control plane should not jump illegally)
                emit(command, "task.completed", Map.of("progress", 100, "result", "SUCCEEDED"), 2L);
                emit(command, "task.running", Map.of("progress", 10), 1L);
            } else {
                emit(command, "task.running", Map.of("progress", 10), null);
                Thread.sleep(150);
                emit(command, "task.progress.updated", Map.of("progress", 60), null);
                Thread.sleep(150);
                emit(command, "task.completed", Map.of("progress", 100, "result", "SUCCEEDED"), null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emit(command, "task.failed", Map.of("reason", "interrupted"), null);
        } catch (Exception e) {
            log.error("simulator failure", e);
            emit(command, "task.failed", Map.of("reason", e.getMessage()), null);
        }
    }

    private void emit(CommandEnvelope command, String eventType, Map<String, Object> payload, Long forcedSeq) {
        publish(buildEvent(command, eventType, payload, forcedSeq));
    }

    private RobotEvent buildEvent(CommandEnvelope command, String eventType, Map<String, Object> payload, Long forcedSeq) {
        long seq = forcedSeq != null
                ? forcedSeq
                : sequences.computeIfAbsent(command.robotId(), id -> new AtomicLong()).incrementAndGet();
        Object taskId = command.payload() == null ? null : command.payload().get("taskId");
        Map<String, Object> body = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        return new RobotEvent(
                Ids.newId(),
                eventType,
                "1.0",
                command.tenantId(),
                command.siteId(),
                command.robotId(),
                taskId == null ? null : taskId.toString(),
                seq,
                Instant.now(),
                Instant.now(),
                "SIMULATOR",
                command.correlationId(),
                body
        );
    }

    private void publish(RobotEvent event) {
        for (Consumer<RobotEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private String mapCapability(String commandType) {
        return switch (commandType) {
            case "DELIVERY_START", "DELIVER" -> "delivery";
            case "CLEAN", "CLEANING_START" -> "cleaning";
            case "RETURN_TO_DOCK" -> "docking";
            case "OPEN_COMPARTMENT" -> "compartment";
            default -> commandType.toLowerCase();
        };
    }
}
