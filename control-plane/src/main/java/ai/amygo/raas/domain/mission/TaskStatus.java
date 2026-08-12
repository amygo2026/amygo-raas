package ai.amygo.raas.domain.mission;

public enum TaskStatus {
    DRAFT,
    QUEUED,
    ASSIGNED,
    DISPATCHING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    SUSPENDED,
    NEEDS_INTERVENTION;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }

    public boolean canTransitionTo(TaskStatus next) {
        if (this.isTerminal()) {
            return false;
        }
        return switch (this) {
            case DRAFT -> next == QUEUED || next == CANCELED;
            case QUEUED -> next == ASSIGNED || next == CANCELED || next == FAILED;
            case ASSIGNED -> next == DISPATCHING || next == CANCELED || next == FAILED;
            case DISPATCHING -> next == RUNNING || next == FAILED || next == CANCELED || next == NEEDS_INTERVENTION;
            case RUNNING -> next == SUCCEEDED || next == FAILED || next == CANCELED
                    || next == SUSPENDED || next == NEEDS_INTERVENTION;
            case SUSPENDED -> next == RUNNING || next == CANCELED || next == FAILED || next == NEEDS_INTERVENTION;
            case NEEDS_INTERVENTION -> next == RUNNING || next == CANCELED || next == FAILED;
            default -> false;
        };
    }
}
