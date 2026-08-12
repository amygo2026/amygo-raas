package ai.amygo.raas.adapter.support;

import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.domain.shared.RobotEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Shared in-process delivery progression for Simulator / vendor Mock adapters.
 * Does not encode any vendor DTO or proprietary API fields.
 */
public final class InProcessDeliveryExecutor {
    private final String source;
    private final long progressDelayMs;
    private final MockFaultMode faultMode;
    private final java.util.function.ToLongFunction<String> sequenceSeed;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<RobotEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, String> acceptedIdempotency = new ConcurrentHashMap<>();
    private final Map<String, String> commandOutcomes = new ConcurrentHashMap<>();

    public InProcessDeliveryExecutor(String source, long progressDelayMs) {
        this(source, progressDelayMs, MockFaultMode.NONE, id -> 0L);
    }

    public InProcessDeliveryExecutor(String source, long progressDelayMs, MockFaultMode faultMode) {
        this(source, progressDelayMs, faultMode, id -> 0L);
    }

    public InProcessDeliveryExecutor(
            String source,
            long progressDelayMs,
            MockFaultMode faultMode,
            java.util.function.ToLongFunction<String> sequenceSeed
    ) {
        this.source = source;
        this.progressDelayMs = Math.max(0, progressDelayMs);
        this.faultMode = faultMode == null ? MockFaultMode.NONE : faultMode;
        this.sequenceSeed = sequenceSeed == null ? id -> 0L : sequenceSeed;
    }

    public MockFaultMode faultMode() {
        return faultMode;
    }

    public void subscribe(Consumer<RobotEvent> listener) {
        listeners.add(listener);
    }

    public String rememberIdempotency(String key, String commandId) {
        return acceptedIdempotency.putIfAbsent(key, commandId);
    }

    public String commandOutcome(String commandId) {
        return commandOutcomes.get(commandId);
    }

    public void startDelivery(CommandEnvelope command) {
        if (faultMode == MockFaultMode.TIMEOUT_UNKNOWN) {
            commandOutcomes.put(command.commandId(), "UNKNOWN");
            return;
        }
        emit(command, "command.accepted", Map.of("commandType", command.commandType()), null);
        commandOutcomes.put(command.commandId(), "ACCEPTED");
        long startDelay = Math.max(1, progressDelayMs);
        scheduler.schedule(() -> runProgress(command), startDelay, TimeUnit.MILLISECONDS);
    }

    public void emitCancel(CommandEnvelope command) {
        emit(command, "command.accepted", Map.of("commandType", "CANCEL"), null);
        emit(command, "task.canceled", Map.of("reason", "operator_or_system_cancel"), null);
        commandOutcomes.put(command.commandId(), "CANCELED");
    }

    public void emit(CommandEnvelope command, String eventType, Map<String, Object> payload, Long forcedSeq) {
        publish(buildEvent(command, eventType, payload, forcedSeq));
    }

    private void runProgress(CommandEnvelope command) {
        emit(command, "task.running", Map.of("progress", 10), null);
        if (faultMode == MockFaultMode.DISCONNECT_MID_MISSION) {
            emit(command, "robot.connectivity.changed", Map.of("connectivity", "OFFLINE"), null);
            emit(command, "task.failed", Map.of("reason", "disconnect_mid_mission"), null);
            commandOutcomes.put(command.commandId(), "FAILED_DISCONNECT");
            return;
        }
        sleep();
        emit(command, "task.progress.updated", Map.of("progress", 60), null);
        sleep();
        emit(command, "task.completed", Map.of("progress", 100, "result", "SUCCEEDED"), null);
        if (faultMode == MockFaultMode.DUPLICATE_CALLBACK) {
            // Second completion with a new eventId — projection must ignore illegal terminal rewind.
            emit(command, "task.completed", Map.of("progress", 100, "result", "SUCCEEDED", "duplicate", true), null);
        }
        commandOutcomes.put(command.commandId(), "SUCCEEDED");
    }

    private void sleep() {
        if (progressDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(progressDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private RobotEvent buildEvent(CommandEnvelope command, String eventType, Map<String, Object> payload, Long forcedSeq) {
        AtomicLong counter = sequences.computeIfAbsent(
                command.robotId(),
                id -> new AtomicLong(sequenceSeed.applyAsLong(id))
        );
        long seq;
        if (forcedSeq != null) {
            seq = forcedSeq;
            counter.updateAndGet(v -> Math.max(v, forcedSeq));
        } else {
            seq = counter.incrementAndGet();
        }
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
                source,
                command.correlationId(),
                body
        );
    }

    private void publish(RobotEvent event) {
        for (Consumer<RobotEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
