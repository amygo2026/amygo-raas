package ai.amygo.raas.application;

import ai.amygo.raas.adapter.RobotAdapter;
import ai.amygo.raas.domain.mission.Task;
import ai.amygo.raas.domain.mission.TaskStatus;
import ai.amygo.raas.domain.mission.TaskTypes;
import ai.amygo.raas.domain.robot.ConnectivityStatus;
import ai.amygo.raas.domain.robot.OfflinePolicy;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.robot.RobotSnapshot;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.CommandReceiptStatus;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.AuditRepository;
import ai.amygo.raas.persistence.InMemoryStore;
import ai.amygo.raas.persistence.OutboxRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MissionApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MissionApplicationService.class);

    private final InMemoryStore store;
    private final RobotAdapter adapter;
    private final OutboxRepository outbox;
    private final AuditRepository auditRepository;
    private final EventSequenceGuard sequenceGuard;
    private final OfflinePolicy offlinePolicy;
    private final ObservationRegistry observations;

    public MissionApplicationService(
            InMemoryStore store,
            RobotAdapter adapter,
            OutboxRepository outbox,
            AuditRepository auditRepository,
            EventSequenceGuard sequenceGuard,
            ObservationRegistry observations,
            @Value("${raas.ops.offline-policy:fail_on_disconnect}") String offlinePolicyRaw
    ) {
        this.store = store;
        this.adapter = adapter;
        this.outbox = outbox;
        this.auditRepository = auditRepository;
        this.sequenceGuard = sequenceGuard;
        this.observations = observations == null ? ObservationRegistry.NOOP : observations;
        this.offlinePolicy = OfflinePolicy.from(offlinePolicyRaw);
    }

    public OfflinePolicy offlinePolicy() {
        return offlinePolicy;
    }

    @PostConstruct
    void wireEvents() {
        adapter.subscribe(this::onRobotEvent);
    }

    /** @deprecated use {@link #createAndQueue} */
    @Deprecated
    public Task createAndQueueDelivery(String tenantId, String siteId, Map<String, Object> payload, Actor actor) {
        return createAndQueue(tenantId, siteId, TaskTypes.DELIVERY, payload, actor);
    }

    public Task createAndQueue(
            String tenantId,
            String siteId,
            String taskTypeRaw,
            Map<String, Object> payload,
            Actor actor
    ) {
        String taskType = TaskTypes.normalize(taskTypeRaw);
        if (!TaskTypes.isSupported(taskType)) {
            throw new IllegalArgumentException("CAPABILITY_NOT_SUPPORTED: taskType=" + taskType);
        }
        return Observation.createNotStarted("raas.mission.create", observations)
                .lowCardinalityKeyValue("task.type", taskType)
                .lowCardinalityKeyValue("tenant.id", tenantId)
                .observe(() -> {
                    Task task = new Task(Ids.newId(), tenantId, siteId, taskType, payload);
                    task.queue();
                    store.saveTask(task);
                    audit(tenantId, actor, "task.created", "Task", task.getId(), Map.of("taskType", taskType));
                    scheduleNext(tenantId, siteId);
                    return task;
                });
    }

    public synchronized void scheduleNext(String tenantId, String siteId) {
        List<Task> queued = store.listTasks(tenantId, siteId).stream()
                .filter(t -> t.getStatus() == TaskStatus.QUEUED)
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .toList();
        if (queued.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (Task task : queued) {
            String capability = TaskTypes.requiredCapability(task.getTaskType());
            Robot robot = store.listRobots(tenantId, siteId).stream()
                    .filter(r -> r.toSnapshot().schedulable())
                    .filter(r -> !r.hasActiveLease(now))
                    .filter(r -> adapter.capabilities(r.getId()).contains(capability))
                    .sorted(Comparator
                            .comparing((Robot r) -> TaskTypes.prefersProfile(task.getTaskType(), r.getModelProfile()) ? 0 : 1)
                            .thenComparing(Robot::getId))
                    .findFirst()
                    .orElse(null);
            if (robot == null) {
                continue;
            }
            String assignmentId = Ids.newId();
            task.assign(robot.getId(), assignmentId);
            robot.acquireLease(task.getId(), now.plus(Duration.ofMinutes(10)));
            store.saveRobot(robot);
            store.saveTask(task);
            dispatch(task, robot);
            return;
        }
    }

    private void dispatch(Task task, Robot robot) {
        Observation.createNotStarted("raas.mission.dispatch", observations)
                .lowCardinalityKeyValue("task.type", task.getTaskType())
                .lowCardinalityKeyValue("robot.adapter", Objects.toString(robot.getAdapterType(), "unknown"))
                .observe(() -> doDispatch(task, robot));
    }

    private void doDispatch(Task task, Robot robot) {
        task.markDispatching();
        store.saveTask(task);

        int attemptNo = task.getAttemptNo();
        String commandId = Ids.newId();
        String idempotencyKey = task.getId() + "-attempt-" + attemptNo;
        if (!store.rememberCommandIdempotency(task.getTenantId(), idempotencyKey, commandId)) {
            commandId = store.findCommandByIdempotency(task.getTenantId(), idempotencyKey).orElse(commandId);
        }

        Map<String, Object> payload = new LinkedHashMap<>(task.getPayload());
        payload.put("taskId", task.getId());
        payload.put("assignmentId", task.getActiveAssignmentId());
        payload.put("attemptNo", attemptNo);
        payload.put("taskType", task.getTaskType());

        String commandType = TaskTypes.startCommandType(task.getTaskType());
        CommandEnvelope command = new CommandEnvelope(
                commandId,
                task.getId(),
                task.getTenantId(),
                task.getSiteId(),
                robot.getId(),
                commandType,
                idempotencyKey,
                Instant.now(),
                Instant.now().plus(Duration.ofMinutes(2)),
                Actor.system("scheduler"),
                payload
        );

        CommandReceipt receipt = adapter.submit(command);
        audit(task.getTenantId(), Actor.system("scheduler"), "command.submitted", "Command", commandId,
                Map.of("status", receipt.status().name(), "taskId", task.getId(), "robotId", robot.getId(),
                        "attemptNo", attemptNo, "commandType", commandType));

        if (receipt.status() == CommandReceiptStatus.REJECTED) {
            task.markFailed(Objects.toString(receipt.reasonCode(), "REJECTED"));
            robot.releaseLease();
            store.saveRobot(robot);
            store.saveTask(task);
            scheduleNext(task.getTenantId(), task.getSiteId());
            return;
        }

        if (receipt.status() == CommandReceiptStatus.UNKNOWN) {
            // Do NOT blind-retry the same idempotencyKey. Hold for operator / reconcile.
            task.markNeedsIntervention(Objects.toString(receipt.reasonCode(), "COMMAND_STATUS_UNKNOWN"));
            store.saveTask(task);
            store.rememberUnknownCommand(commandId, task.getId(), command.idempotencyKey(), receipt.reasonCode());
            audit(task.getTenantId(), Actor.system("scheduler"), "command.unknown", "Command", commandId,
                    Map.of("taskId", task.getId(), "reason", Objects.toString(receipt.reasonCode(), "UNKNOWN")));
            robot.releaseLease();
            store.saveRobot(robot);
            scheduleNext(task.getTenantId(), task.getSiteId());
        }
    }

    public synchronized Task cancel(String tenantId, String taskId, Actor actor) {
        Task task = store.findTask(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (task.getStatus().isTerminal()) {
            return task;
        }
        if (task.getAssignedRobotId() != null) {
            CommandEnvelope cancel = new CommandEnvelope(
                    Ids.newId(),
                    task.getId(),
                    tenantId,
                    task.getSiteId(),
                    task.getAssignedRobotId(),
                    "CANCEL",
                    task.getId() + "-cancel-" + task.getAttemptNo(),
                    Instant.now(),
                    Instant.now().plus(Duration.ofMinutes(1)),
                    actor,
                    Map.of("taskId", task.getId())
            );
            adapter.cancel(cancel);
            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                r.releaseLease();
                store.saveRobot(r);
            });
        }
        task.markCanceled();
        store.saveTask(task);
        audit(tenantId, actor, "task.canceled", "Task", taskId, Map.of());
        scheduleNext(tenantId, task.getSiteId());
        return task;
    }

    /** Operator fail: non-terminal → FAILED; lease released; does not invent vendor errors. */
    public synchronized Task fail(String tenantId, String taskId, String reason, Actor actor) {
        Task task = store.findTask(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (task.getStatus().isTerminal()) {
            return task;
        }
        String failReason = reason == null || reason.isBlank() ? "operator_fail" : reason;
        if (task.getAssignedRobotId() != null) {
            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                r.releaseLease();
                store.saveRobot(r);
            });
        }
        task.markFailed(failReason);
        store.saveTask(task);
        audit(tenantId, actor, "task.failed", "Task", taskId, Map.of("reason", failReason, "source", "operator"));
        scheduleNext(tenantId, task.getSiteId());
        return task;
    }

    /**
     * Operator reconnect / resync after mid-mission disconnect (Mock path).
     * Marks robot ONLINE, pulls adapter snapshot, audits, then schedules queued work.
     */
    public synchronized Robot reconnect(String tenantId, String siteId, String robotId, Actor actor) {
        return Observation.createNotStarted("raas.robot.reconnect", observations)
                .lowCardinalityKeyValue("robot.id", robotId)
                .observe(() -> doReconnect(tenantId, siteId, robotId, actor));
    }

    private Robot doReconnect(String tenantId, String siteId, String robotId, Actor actor) {
        Robot robot = store.findRobot(robotId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .filter(r -> r.getSiteId().equals(siteId))
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));

        ConnectivityStatus before = robot.getConnectivityStatus();
        robot.markOnline();
        store.saveRobot(robot);

        RobotSnapshot snapshot;
        try {
            snapshot = adapter.getSnapshot(robotId);
        } catch (RuntimeException ex) {
            log.warn("reconnect snapshot failed for {}: {}", robotId, ex.getMessage());
            snapshot = robot.toSnapshot();
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before.name());
        detail.put("after", robot.getConnectivityStatus().name());
        detail.put("snapshotConnectivity", snapshot.connectivityStatus().name());
        detail.put("offlinePolicy", offlinePolicy.name());
        audit(tenantId, actor, "robot.reconnected", "Robot", robotId, detail);

        scheduleNext(tenantId, siteId);
        return robot;
    }

    /**
     * Recovery: terminal task stays terminal; creates a new QUEUED task (new attempt lineage).
     */
    public synchronized Task restart(String tenantId, String taskId, Actor actor) {
        Task source = store.findTask(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!source.getStatus().isTerminal()) {
            throw new IllegalStateException("Restart only allowed from terminal tasks; cancel or fail first");
        }
        Map<String, Object> payload = new LinkedHashMap<>(source.getPayload());
        payload.put("restartedFromTaskId", source.getId());
        payload.remove("failureReason");
        Task next = new Task(Ids.newId(), source.getTenantId(), source.getSiteId(), source.getTaskType(), payload);
        next.setAttemptNo(source.getAttemptNo() + 1);
        next.queue();
        store.saveTask(next);
        audit(tenantId, actor, "task.restarted", "Task", next.getId(),
                Map.of("fromTaskId", source.getId(), "attemptNo", next.getAttemptNo()));
        scheduleNext(tenantId, source.getSiteId());
        return next;
    }

    public List<Map<String, Object>> timeline(String tenantId, String taskId) {
        Task task = store.findTask(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of(
                "kind", "task",
                "at", task.getCreatedAt().toString(),
                "status", task.getStatus().name(),
                "detail", Map.of("created", true, "attemptNo", task.getAttemptNo())
        ));
        for (RobotEvent e : store.listEventsForTask(tenantId, taskId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("kind", "event");
            row.put("at", e.occurredAt().toString());
            row.put("eventType", e.eventType());
            row.put("eventId", e.eventId());
            row.put("sequence", e.sequence());
            row.put("robotId", e.robotId());
            row.put("payload", e.payload());
            items.add(row);
        }
        for (Map<String, Object> a : store.listAudit(tenantId)) {
            if (taskId.equals(String.valueOf(a.get("objectId")))) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("kind", "audit");
                row.put("at", a.get("createdAt"));
                row.put("action", a.get("action"));
                row.put("actorId", a.get("actorId"));
                row.put("detail", a.get("detail"));
                items.add(row);
            }
        }
        items.sort((a, b) -> String.valueOf(a.get("at")).compareTo(String.valueOf(b.get("at"))));
        return items;
    }

    private synchronized void onRobotEvent(RobotEvent event) {
        if (store.hasEvent(event.eventId())) {
            return;
        }

        EventSequenceGuard.Decision decision = sequenceGuard.accept(event);
        if (!decision.applyNow()) {
            // Gap-buffered or late-dropped; buffered events apply later via drainAfter.
            return;
        }
        applyEvent(event);
        for (RobotEvent drained : decision.drainAfter()) {
            if (!store.hasEvent(drained.eventId())) {
                applyEvent(drained);
            }
        }
    }

    private void applyEvent(RobotEvent event) {
        store.appendEvent(event);
        applyConnectivity(event);
        if (event.taskId() == null || event.taskId().isBlank()) {
            return;
        }
        store.findTask(event.taskId()).ifPresent(task -> {
            try {
                switch (event.eventType()) {
                    case "command.accepted", "task.running", "task.progress.updated" -> {
                        if (task.getStatus() == TaskStatus.ASSIGNED) {
                            task.markDispatching();
                        }
                        if (task.getStatus() == TaskStatus.DISPATCHING) {
                            task.markRunning();
                            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                r.markExecuting();
                                store.saveRobot(r);
                            });
                            store.saveTask(task);
                        }
                    }
                    case "task.completed" -> {
                        if (!task.getStatus().isTerminal()) {
                            if (task.getStatus() == TaskStatus.ASSIGNED) {
                                task.markDispatching();
                            }
                            if (task.getStatus() == TaskStatus.DISPATCHING) {
                                task.markRunning();
                            }
                            if (task.getStatus() == TaskStatus.RUNNING) {
                                task.markSucceeded();
                                store.saveTask(task);
                                store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                    r.releaseLease();
                                    store.saveRobot(r);
                                });
                                scheduleNext(task.getTenantId(), task.getSiteId());
                            }
                        }
                    }
                    case "task.failed" -> applyTaskFailed(task, event);
                    case "task.canceled" -> {
                        if (!task.getStatus().isTerminal()) {
                            task.markCanceled();
                            store.saveTask(task);
                            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                r.releaseLease();
                                store.saveRobot(r);
                            });
                            scheduleNext(task.getTenantId(), task.getSiteId());
                        }
                    }
                    default -> log.debug("ignored event {}", event.eventType());
                }
            } catch (IllegalStateException ex) {
                log.warn("state transition ignored: {}", ex.getMessage());
            }
        });
    }

    private void applyConnectivity(RobotEvent event) {
        if (!"robot.connectivity.changed".equals(event.eventType())) {
            return;
        }
        Object raw = event.payload().get("connectivity");
        if (raw == null) {
            return;
        }
        ConnectivityStatus status;
        try {
            status = ConnectivityStatus.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }
        store.findRobot(event.robotId()).ifPresent(r -> {
            r.setConnectivityStatus(status);
            store.saveRobot(r);
        });
    }

    private void applyTaskFailed(Task task, RobotEvent event) {
        if (task.getStatus().isTerminal() || task.getStatus() == TaskStatus.NEEDS_INTERVENTION) {
            return;
        }
        String reason = String.valueOf(event.payload().getOrDefault("reason", "failed"));
        boolean disconnect = "disconnect_mid_mission".equals(reason);
        if (disconnect && offlinePolicy == OfflinePolicy.HOLD_ON_DISCONNECT) {
            task.markNeedsIntervention(reason);
            store.saveTask(task);
            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                r.releaseLease();
                store.saveRobot(r);
            });
            audit(task.getTenantId(), Actor.system("mission"), "task.held_on_disconnect", "Task", task.getId(),
                    Map.of("reason", reason, "policy", offlinePolicy.name()));
            scheduleNext(task.getTenantId(), task.getSiteId());
            return;
        }
        task.markFailed(reason);
        store.saveTask(task);
        store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
            r.releaseLease();
            store.saveRobot(r);
        });
        scheduleNext(task.getTenantId(), task.getSiteId());
    }

    private void audit(String tenantId, Actor actor, String action, String objectType, String objectId, Map<String, Object> detail) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("tenantId", tenantId);
        entry.put("actorType", actor.type());
        entry.put("actorId", actor.id());
        entry.put("action", action);
        entry.put("objectType", objectType);
        entry.put("objectId", objectId);
        entry.put("detail", detail);
        entry.put("createdAt", Instant.now().toString());
        store.appendAudit(entry);
        auditRepository.append(tenantId, actor.type(), actor.id(), action, objectType, objectId, detail);
        outbox.append(tenantId, objectType, objectId, action, detail);
    }

    public Map<String, Long> sequenceMetrics() {
        return sequenceGuard.metrics();
    }
}
