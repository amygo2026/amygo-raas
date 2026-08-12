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
import org.springframework.stereotype.Component;

import java.time.Instant;
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
 * In-process simulator adapter. No vendor SDK.
 * Supports success path, idempotent submit, expired rejection, and delayed events.
 */
@Component
public class SimulatorRobotAdapter implements RobotAdapter {
    private static final Logger log = LoggerFactory.getLogger(SimulatorRobotAdapter.class);

    private final InMemoryStore store;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<RobotEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, String> acceptedIdempotency = new ConcurrentHashMap<>();

    public SimulatorRobotAdapter(InMemoryStore store) {
        this.store = store;
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("SIMULATOR", "0.1.0", "Simulator");
    }

    @Override
    public Set<String> capabilities(String robotId) {
        return Set.of("navigation", "delivery", "docking");
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
        String prior = acceptedIdempotency.putIfAbsent(command.idempotencyKey(), command.commandId());
        if (prior != null) {
            return new CommandReceipt(prior, CommandReceiptStatus.ACCEPTED, "IDEMPOTENT_REPLAY", "same idempotencyKey");
        }

        if (!capabilities(command.robotId()).contains(mapCapability(command.commandType()))
                && !"CANCEL".equals(command.commandType())) {
            // DELIVERY_START maps to delivery
            if (!"DELIVERY_START".equals(command.commandType()) && !"DELIVER".equals(command.commandType())) {
                return new CommandReceipt(command.commandId(), CommandReceiptStatus.REJECTED,
                        "CAPABILITY_NOT_SUPPORTED", command.commandType());
            }
        }

        emit(command, "command.accepted", Map.of("commandType", command.commandType()));
        scheduler.schedule(() -> simulateProgress(command), 300, TimeUnit.MILLISECONDS);
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "accepted by simulator");
    }

    @Override
    public CommandReceipt cancel(CommandEnvelope command) {
        emit(command, "command.accepted", Map.of("commandType", "CANCEL"));
        emit(command, "task.canceled", Map.of("reason", "operator_or_system_cancel"));
        return new CommandReceipt(command.commandId(), CommandReceiptStatus.ACCEPTED, null, "cancel accepted");
    }

    @Override
    public void subscribe(Consumer<RobotEvent> listener) {
        listeners.add(listener);
    }

    private void simulateProgress(CommandEnvelope command) {
        try {
            emit(command, "task.running", Map.of("progress", 10));
            Thread.sleep(200);
            emit(command, "task.progress.updated", Map.of("progress", 60));
            Thread.sleep(200);
            emit(command, "task.completed", Map.of("progress", 100, "result", "SUCCEEDED"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emit(command, "task.failed", Map.of("reason", "interrupted"));
        } catch (Exception e) {
            log.error("simulator failure", e);
            emit(command, "task.failed", Map.of("reason", e.getMessage()));
        }
    }

    private void emit(CommandEnvelope command, String eventType, Map<String, Object> payload) {
        long seq = sequences.computeIfAbsent(command.robotId(), id -> new AtomicLong()).incrementAndGet();
        RobotEvent event = new RobotEvent(
                Ids.newId(),
                eventType,
                "1.0",
                command.tenantId(),
                command.siteId(),
                command.robotId(),
                command.payload() == null ? null : String.valueOf(command.payload().getOrDefault("taskId", "")),
                seq,
                Instant.now(),
                Instant.now(),
                "SIMULATOR",
                command.correlationId(),
                payload
        );
        // Fix taskId extraction
        Object taskId = command.payload() == null ? null : command.payload().get("taskId");
        event = new RobotEvent(
                event.eventId(),
                event.eventType(),
                event.schemaVersion(),
                event.tenantId(),
                event.siteId(),
                event.robotId(),
                taskId == null ? null : taskId.toString(),
                event.sequence(),
                event.occurredAt(),
                event.receivedAt(),
                event.source(),
                event.correlationId(),
                event.payload()
        );
        for (Consumer<RobotEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private String mapCapability(String commandType) {
        return switch (commandType) {
            case "DELIVERY_START", "DELIVER" -> "delivery";
            case "CLEAN", "CLEANING_START" -> "cleaning";
            case "RETURN_TO_DOCK" -> "docking";
            default -> commandType.toLowerCase();
        };
    }
}
