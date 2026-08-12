package ai.amygo.raas.application;

import ai.amygo.raas.adapter.RobotAdapter;
import ai.amygo.raas.domain.mission.Task;
import ai.amygo.raas.domain.mission.TaskStatus;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.CommandReceiptStatus;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.AuditRepository;
import ai.amygo.raas.persistence.InMemoryStore;
import ai.amygo.raas.persistence.OutboxRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
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

    public MissionApplicationService(
            InMemoryStore store,
            RobotAdapter adapter,
            OutboxRepository outbox,
            AuditRepository auditRepository
    ) {
        this.store = store;
        this.adapter = adapter;
        this.outbox = outbox;
        this.auditRepository = auditRepository;
    }

    @PostConstruct
    void wireEvents() {
        adapter.subscribe(this::onRobotEvent);
    }

    public Task createAndQueueDelivery(String tenantId, String siteId, Map<String, Object> payload, Actor actor) {
        Task task = new Task(Ids.newId(), tenantId, siteId, "DELIVERY", payload);
        task.queue();
        store.saveTask(task);
        audit(tenantId, actor, "task.created", "Task", task.getId(), Map.of("taskType", "DELIVERY"));
        scheduleNext(tenantId, siteId);
        return task;
    }

    public synchronized void scheduleNext(String tenantId, String siteId) {
        List<Task> queued = store.listTasks(tenantId, siteId).stream()
                .filter(t -> t.getStatus() == TaskStatus.QUEUED)
                .toList();
        if (queued.isEmpty()) {
            return;
        }
        Robot robot = store.listRobots(tenantId, siteId).stream()
                .filter(r -> r.toSnapshot().schedulable())
                .filter(r -> !r.hasActiveLease(Instant.now()))
                .findFirst()
                .orElse(null);
        if (robot == null) {
            return;
        }
        Task task = queued.get(queued.size() - 1);
        // list sorted newest first; take oldest queued
        task = store.listTasks(tenantId, siteId).stream()
                .filter(t -> t.getStatus() == TaskStatus.QUEUED)
                .reduce((a, b) -> a.getCreatedAt().isBefore(b.getCreatedAt()) ? a : b)
                .orElse(task);

        String assignmentId = Ids.newId();
        task.assign(robot.getId(), assignmentId);
        robot.acquireLease(task.getId(), Instant.now().plus(Duration.ofMinutes(10)));
        store.saveRobot(robot);
        store.saveTask(task);
        dispatch(task, robot);
    }

    private void dispatch(Task task, Robot robot) {
        task.markDispatching();
        store.saveTask(task);

        String commandId = Ids.newId();
        String idempotencyKey = task.getId() + "-attempt-1";
        if (!store.rememberCommandIdempotency(task.getTenantId(), idempotencyKey, commandId)) {
            commandId = store.findCommandByIdempotency(task.getTenantId(), idempotencyKey).orElse(commandId);
        }

        Map<String, Object> payload = new LinkedHashMap<>(task.getPayload());
        payload.put("taskId", task.getId());
        payload.put("assignmentId", task.getActiveAssignmentId());

        CommandEnvelope command = new CommandEnvelope(
                commandId,
                task.getId(),
                task.getTenantId(),
                task.getSiteId(),
                robot.getId(),
                "DELIVERY_START",
                idempotencyKey,
                Instant.now(),
                Instant.now().plus(Duration.ofMinutes(2)),
                Actor.system("scheduler"),
                payload
        );

        CommandReceipt receipt = adapter.submit(command);
        audit(task.getTenantId(), Actor.system("scheduler"), "command.submitted", "Command", commandId,
                Map.of("status", receipt.status().name(), "taskId", task.getId(), "robotId", robot.getId()));

        if (receipt.status() == CommandReceiptStatus.REJECTED) {
            task.markFailed(Objects.toString(receipt.reasonCode(), "REJECTED"));
            robot.releaseLease();
            store.saveRobot(robot);
            store.saveTask(task);
        }
    }

    public Task cancel(String tenantId, String taskId, Actor actor) {
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
                    task.getId() + "-cancel",
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
        return task;
    }

    private void onRobotEvent(RobotEvent event) {
        if (store.hasEvent(event.eventId())) {
            return; // dedupe
        }
        store.appendEvent(event);
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
                            task.markSucceeded();
                            store.saveTask(task);
                            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                r.releaseLease();
                                store.saveRobot(r);
                            });
                            scheduleNext(task.getTenantId(), task.getSiteId());
                        }
                    }
                    case "task.failed" -> {
                        if (!task.getStatus().isTerminal()) {
                            task.markFailed(String.valueOf(event.payload().getOrDefault("reason", "failed")));
                            store.saveTask(task);
                            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                r.releaseLease();
                                store.saveRobot(r);
                            });
                        }
                    }
                    case "task.canceled" -> {
                        if (!task.getStatus().isTerminal()) {
                            task.markCanceled();
                            store.saveTask(task);
                            store.findRobot(task.getAssignedRobotId()).ifPresent(r -> {
                                r.releaseLease();
                                store.saveRobot(r);
                            });
                        }
                    }
                    default -> log.debug("ignored event {}", event.eventType());
                }
            } catch (IllegalStateException ex) {
                log.warn("state transition ignored: {}", ex.getMessage());
            }
        });
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
}
