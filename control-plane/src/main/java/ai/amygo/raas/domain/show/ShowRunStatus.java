package ai.amygo.raas.domain.show;

public enum ShowRunStatus {
    DRAFT,
    PREFLIGHT_OK,
    ARMED,
    RUNNING,
    SUCCEEDED,
    ABORTED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == ABORTED || this == FAILED;
    }
}
