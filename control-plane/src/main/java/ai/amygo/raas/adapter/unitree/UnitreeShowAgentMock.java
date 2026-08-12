package ai.amygo.raas.adapter.unitree;

import ai.amygo.raas.domain.show.ShowRun;
import ai.amygo.raas.domain.show.ShowRunStatus;
import ai.amygo.raas.domain.shared.Ids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unitree Show Agent Mock — local venue prototype only.
 * No SDK2 / DDS / joint commands. Support level: Mock.
 */
@Component
public class UnitreeShowAgentMock {
    public static final String SUPPORT_LEVEL = "Mock";
    public static final String VERSION = "0.1.0-mock";

    private final Map<String, ShowRun> runs = new ConcurrentHashMap<>();
    private final Map<String, String> startIdempotency = new ConcurrentHashMap<>();
    private final boolean simulateDisconnectOnStart;

    public UnitreeShowAgentMock(
            @Value("${raas.unitree-show.simulate-disconnect-on-start:false}") boolean simulateDisconnectOnStart
    ) {
        this.simulateDisconnectOnStart = simulateDisconnectOnStart;
    }

    public ShowRun create(
            String tenantId,
            String siteId,
            String robotId,
            String assetId,
            String assetVersion,
            String assetHash
    ) {
        ShowRun run = new ShowRun(Ids.newId(), tenantId, siteId, robotId, assetId, assetVersion, assetHash);
        runs.put(run.getId(), run);
        return run;
    }

    public Optional<ShowRun> find(String showRunId) {
        return Optional.ofNullable(runs.get(showRunId));
    }

    public ShowRun preflight(String showRunId) {
        ShowRun run = require(showRunId);
        run.preflightOk();
        return run;
    }

    public ShowRun arm(String showRunId) {
        ShowRun run = require(showRunId);
        run.arm();
        return run;
    }

    /**
     * @return receipt status ACCEPTED | UNKNOWN | IDEMPOTENT
     */
    public Map<String, Object> start(String showRunId, String idempotencyKey) {
        ShowRun run = require(showRunId);
        String prior = startIdempotency.putIfAbsent(idempotencyKey, showRunId);
        if (prior != null) {
            return Map.of(
                    "status", run.getStatus() == ShowRunStatus.RUNNING ? "IDEMPOTENT" : "IDEMPOTENT",
                    "showRunId", showRunId,
                    "startCount", run.getStartCount(),
                    "showStatus", run.getStatus().name()
            );
        }
        if (simulateDisconnectOnStart && run.getStatus() == ShowRunStatus.ARMED) {
            // First attempt: UNKNOWN — must not leave RUNNING; operator may retry with SAME key.
            startIdempotency.put(idempotencyKey, showRunId);
            return Map.of(
                    "status", "UNKNOWN",
                    "reasonCode", "COMMAND_STATUS_UNKNOWN",
                    "showRunId", showRunId,
                    "startCount", run.getStartCount(),
                    "showStatus", run.getStatus().name()
            );
        }
        boolean newlyStarted = run.start();
        return Map.of(
                "status", newlyStarted ? "ACCEPTED" : "IDEMPOTENT",
                "showRunId", showRunId,
                "startCount", run.getStartCount(),
                "showStatus", run.getStatus().name()
        );
    }

    /**
     * Reconcile after UNKNOWN: with same idempotency key, perform at-most-once start.
     */
    public Map<String, Object> reconcileStart(String showRunId, String idempotencyKey) {
        ShowRun run = require(showRunId);
        String bound = startIdempotency.get(idempotencyKey);
        if (bound == null) {
            throw new IllegalStateException("Unknown idempotencyKey — refuse blind start");
        }
        if (run.getStatus() == ShowRunStatus.RUNNING || run.getStatus().isTerminal()) {
            return Map.of(
                    "status", "IDEMPOTENT",
                    "showRunId", showRunId,
                    "startCount", run.getStartCount(),
                    "showStatus", run.getStatus().name()
            );
        }
        if (run.getStatus() == ShowRunStatus.ARMED) {
            run.start();
        }
        return Map.of(
                "status", "ACCEPTED",
                "showRunId", showRunId,
                "startCount", run.getStartCount(),
                "showStatus", run.getStatus().name()
        );
    }

    public ShowRun abort(String showRunId, String reason) {
        ShowRun run = require(showRunId);
        run.abort(reason);
        return run;
    }

    public Map<String, Object> descriptor() {
        return Map.of(
                "adapterType", "UNITREE_SHOW",
                "version", VERSION,
                "supportLevel", SUPPORT_LEVEL
        );
    }

    private ShowRun require(String showRunId) {
        ShowRun run = runs.get(showRunId);
        if (run == null) {
            throw new IllegalArgumentException("ShowRun not found");
        }
        return run;
    }
}
