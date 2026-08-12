package ai.amygo.raas.domain.robot;

/**
 * Ops policy when a robot drops mid-mission.
 * Domain rule: offline must not auto-fail tasks without an explicit policy.
 */
public enum OfflinePolicy {
    /** Legacy Mock Week4 behavior: disconnect → task FAILED. */
    FAIL_ON_DISCONNECT,
    /** Hold for operator: disconnect → NEEDS_INTERVENTION; recover via reconnect + fail/restart. */
    HOLD_ON_DISCONNECT;

    public static OfflinePolicy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return FAIL_ON_DISCONNECT;
        }
        return switch (raw.trim().toLowerCase().replace('-', '_')) {
            case "hold_on_disconnect", "hold" -> HOLD_ON_DISCONNECT;
            case "fail_on_disconnect", "fail" -> FAIL_ON_DISCONNECT;
            default -> FAIL_ON_DISCONNECT;
        };
    }
}
