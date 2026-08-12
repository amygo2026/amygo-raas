package ai.amygo.raas.adapter.support;

/**
 * In-process fault modes for Mock adapters (no vendor API fields).
 */
public enum MockFaultMode {
    NONE,
    /** Submit returns UNKNOWN — control plane must not blind-retry. */
    TIMEOUT_UNKNOWN,
    /** Mid-mission connectivity loss → fail / intervention path. */
    DISCONNECT_MID_MISSION,
    /** Emit completed twice (different eventIds) — second must not rewind state. */
    DUPLICATE_CALLBACK;

    public static MockFaultMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return MockFaultMode.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
