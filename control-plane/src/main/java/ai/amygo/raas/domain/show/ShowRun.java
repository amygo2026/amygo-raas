package ai.amygo.raas.domain.show;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Venue show run — high-level cues only (no joint/torque streams).
 */
public class ShowRun {
    private final String id;
    private final String tenantId;
    private final String siteId;
    private final String robotId;
    private final String assetId;
    private final String assetVersion;
    private final String assetHash;
    private ShowRunStatus status;
    private int startCount;
    private final Map<String, Object> meta;
    private final Instant createdAt;
    private Instant updatedAt;

    public ShowRun(
            String id,
            String tenantId,
            String siteId,
            String robotId,
            String assetId,
            String assetVersion,
            String assetHash
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.robotId = robotId;
        this.assetId = assetId;
        this.assetVersion = assetVersion;
        this.assetHash = assetHash;
        this.status = ShowRunStatus.DRAFT;
        this.meta = new LinkedHashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getSiteId() { return siteId; }
    public String getRobotId() { return robotId; }
    public String getAssetId() { return assetId; }
    public String getAssetVersion() { return assetVersion; }
    public String getAssetHash() { return assetHash; }
    public ShowRunStatus getStatus() { return status; }
    public int getStartCount() { return startCount; }
    public Map<String, Object> getMeta() { return Map.copyOf(meta); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void preflightOk() {
        require(ShowRunStatus.DRAFT);
        status = ShowRunStatus.PREFLIGHT_OK;
        touch();
    }

    public void arm() {
        require(ShowRunStatus.PREFLIGHT_OK);
        status = ShowRunStatus.ARMED;
        touch();
    }

    /** Idempotent start: repeated calls while RUNNING do not increment startCount. */
    public boolean start() {
        if (status == ShowRunStatus.RUNNING) {
            return false;
        }
        require(ShowRunStatus.ARMED);
        status = ShowRunStatus.RUNNING;
        startCount++;
        touch();
        return true;
    }

    public void abort(String reason) {
        if (status.isTerminal()) {
            return;
        }
        status = ShowRunStatus.ABORTED;
        meta.put("abortReason", reason == null ? "aborted" : reason);
        touch();
    }

    public void succeed() {
        require(ShowRunStatus.RUNNING);
        status = ShowRunStatus.SUCCEEDED;
        touch();
    }

    public void fail(String reason) {
        if (status.isTerminal()) {
            return;
        }
        status = ShowRunStatus.FAILED;
        meta.put("failureReason", reason == null ? "failed" : reason);
        touch();
    }

    private void require(ShowRunStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("ShowRun " + id + " expected " + expected + " but was " + status);
        }
    }

    private void touch() {
        Objects.requireNonNull(status);
        updatedAt = Instant.now();
    }
}
