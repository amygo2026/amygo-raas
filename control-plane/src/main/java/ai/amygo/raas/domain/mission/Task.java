package ai.amygo.raas.domain.mission;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Task {
    private final String id;
    private final String tenantId;
    private final String siteId;
    private final String taskType;
    private TaskStatus status;
    private final Map<String, Object> payload;
    private String assignedRobotId;
    private String activeAssignmentId;
    private int attemptNo = 1;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Task(String id, String tenantId, String siteId, String taskType, Map<String, Object> payload) {
        this.id = id;
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.taskType = taskType;
        this.status = TaskStatus.DRAFT;
        this.payload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getSiteId() { return siteId; }
    public String getTaskType() { return taskType; }
    public TaskStatus getStatus() { return status; }
    public Map<String, Object> getPayload() { return Map.copyOf(payload); }
    public String getAssignedRobotId() { return assignedRobotId; }
    public String getActiveAssignmentId() { return activeAssignmentId; }
    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) {
        if (attemptNo < 1) {
            throw new IllegalArgumentException("attemptNo must be >= 1");
        }
        this.attemptNo = attemptNo;
    }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void queue() {
        transitionTo(TaskStatus.QUEUED);
    }

    public void assign(String robotId, String assignmentId) {
        Objects.requireNonNull(robotId);
        Objects.requireNonNull(assignmentId);
        transitionTo(TaskStatus.ASSIGNED);
        this.assignedRobotId = robotId;
        this.activeAssignmentId = assignmentId;
    }

    public void markDispatching() {
        transitionTo(TaskStatus.DISPATCHING);
    }

    public void markRunning() {
        transitionTo(TaskStatus.RUNNING);
    }

    public void markSucceeded() {
        transitionTo(TaskStatus.SUCCEEDED);
    }

    public void markFailed(String reason) {
        transitionTo(TaskStatus.FAILED);
        this.payload.put("failureReason", reason);
    }

    public void markCanceled() {
        transitionTo(TaskStatus.CANCELED);
    }

    public void markNeedsIntervention() {
        markNeedsIntervention("needs_intervention");
    }

    public void markNeedsIntervention(String reason) {
        transitionTo(TaskStatus.NEEDS_INTERVENTION);
        this.payload.put("interventionReason", reason == null ? "needs_intervention" : reason);
    }

    private void transitionTo(TaskStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal task transition " + status + " -> " + next + " for " + id);
        }
        this.status = next;
        this.version++;
        this.updatedAt = Instant.now();
    }
}
