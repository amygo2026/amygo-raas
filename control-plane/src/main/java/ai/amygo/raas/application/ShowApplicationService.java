package ai.amygo.raas.application;

import ai.amygo.raas.adapter.unitree.UnitreeShowAgentMock;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.show.ShowRun;
import ai.amygo.raas.persistence.AuditRepository;
import ai.amygo.raas.persistence.InMemoryStore;
import ai.amygo.raas.persistence.OutboxRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ShowApplicationService {
    private final UnitreeShowAgentMock agent;
    private final InMemoryStore store;
    private final AuditRepository auditRepository;
    private final OutboxRepository outbox;

    public ShowApplicationService(
            UnitreeShowAgentMock agent,
            InMemoryStore store,
            AuditRepository auditRepository,
            OutboxRepository outbox
    ) {
        this.agent = agent;
        this.store = store;
        this.auditRepository = auditRepository;
        this.outbox = outbox;
    }

    public ShowRun create(
            String tenantId,
            String siteId,
            String robotId,
            String assetId,
            String assetVersion,
            String assetHash,
            Actor actor
    ) {
        store.findRobot(robotId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getSiteId().equals(siteId))
                .orElseThrow(() -> new IllegalArgumentException("Show robot not found in tenant/site"));
        ShowRun run = agent.create(tenantId, siteId, robotId, assetId, assetVersion, assetHash);
        audit(tenantId, actor, "show.created", "ShowRun", run.getId(), Map.of(
                "robotId", robotId,
                "assetId", assetId,
                "assetVersion", assetVersion
        ));
        return run;
    }

    public ShowRun preflight(String tenantId, String showRunId, Actor actor) {
        ShowRun run = requireTenant(tenantId, showRunId);
        run = agent.preflight(run.getId());
        audit(tenantId, actor, "show.preflight", "ShowRun", showRunId, Map.of("status", run.getStatus().name()));
        return run;
    }

    public ShowRun arm(String tenantId, String showRunId, Actor actor) {
        ShowRun run = requireTenant(tenantId, showRunId);
        run = agent.arm(run.getId());
        audit(tenantId, actor, "show.armed", "ShowRun", showRunId, Map.of("status", run.getStatus().name()));
        return run;
    }

    public Map<String, Object> start(String tenantId, String showRunId, String idempotencyKey, Actor actor) {
        requireTenant(tenantId, showRunId);
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? showRunId + "-start" : idempotencyKey;
        Map<String, Object> receipt = agent.start(showRunId, key);
        audit(tenantId, actor, "show.start", "ShowRun", showRunId, receipt);
        return receipt;
    }

    public Map<String, Object> reconcileStart(String tenantId, String showRunId, String idempotencyKey, Actor actor) {
        requireTenant(tenantId, showRunId);
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? showRunId + "-start" : idempotencyKey;
        Map<String, Object> receipt = agent.reconcileStart(showRunId, key);
        audit(tenantId, actor, "show.start.reconcile", "ShowRun", showRunId, receipt);
        return receipt;
    }

    public ShowRun abort(String tenantId, String showRunId, String reason, Actor actor) {
        ShowRun run = requireTenant(tenantId, showRunId);
        run = agent.abort(run.getId(), reason);
        audit(tenantId, actor, "show.aborted", "ShowRun", showRunId, Map.of("reason", reason == null ? "" : reason));
        return run;
    }

    public ShowRun get(String tenantId, String showRunId) {
        return requireTenant(tenantId, showRunId);
    }

    private ShowRun requireTenant(String tenantId, String showRunId) {
        return agent.find(showRunId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("ShowRun not found"));
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
