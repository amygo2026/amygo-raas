package ai.amygo.raas.api;

import ai.amygo.raas.adapter.unitree.UnitreeShowAgentMock;
import ai.amygo.raas.application.ShowApplicationService;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.show.ShowRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shows")
@CrossOrigin(origins = "*")
public class ShowController {
    private final ShowApplicationService shows;
    private final UnitreeShowAgentMock agent;
    private final String defaultTenant;
    private final String defaultSite;

    public ShowController(
            ShowApplicationService shows,
            UnitreeShowAgentMock agent,
            @Value("${raas.demo-tenant-id}") String defaultTenant,
            @Value("${raas.demo-site-id}") String defaultSite
    ) {
        this.shows = shows;
        this.agent = agent;
        this.defaultTenant = defaultTenant;
        this.defaultSite = defaultSite;
    }

    public record CreateShowRequest(
            String robotId,
            String assetId,
            String assetVersion,
            String assetHash
    ) {}

    public record StartRequest(String idempotencyKey) {}

    public record AbortRequest(String reason) {}

    @GetMapping("/agent")
    public Map<String, Object> agent() {
        return agent.descriptor();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @RequestBody CreateShowRequest body
    ) {
        String tenantId = tenantOrDefault(tenantHeader);
        String siteId = siteOrDefault(siteHeader);
        try {
            ShowRun run = shows.create(
                    tenantId,
                    siteId,
                    body.robotId(),
                    body.assetId() == null ? "show-asset-demo" : body.assetId(),
                    body.assetVersion() == null ? "1.0.0" : body.assetVersion(),
                    body.assetHash() == null ? "sha256:demo" : body.assetHash(),
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
            return toView(run);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/{showRunId}")
    public Map<String, Object> get(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable String showRunId
    ) {
        try {
            return toView(shows.get(tenantOrDefault(tenantHeader), showRunId));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{showRunId}/preflight")
    public Map<String, Object> preflight(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String showRunId
    ) {
        return mutate(tenantHeader, actorId, showRunId, (t, a) -> shows.preflight(t, showRunId, a));
    }

    @PostMapping("/{showRunId}/arm")
    public Map<String, Object> arm(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String showRunId
    ) {
        return mutate(tenantHeader, actorId, showRunId, (t, a) -> shows.arm(t, showRunId, a));
    }

    @PostMapping("/{showRunId}/start")
    public Map<String, Object> start(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String showRunId,
            @RequestBody(required = false) StartRequest body
    ) {
        try {
            return shows.start(
                    tenantOrDefault(tenantHeader),
                    showRunId,
                    body == null ? null : body.idempotencyKey(),
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PostMapping("/{showRunId}/start/reconcile")
    public Map<String, Object> reconcile(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String showRunId,
            @RequestBody(required = false) StartRequest body
    ) {
        try {
            return shows.reconcileStart(
                    tenantOrDefault(tenantHeader),
                    showRunId,
                    body == null ? null : body.idempotencyKey(),
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PostMapping("/{showRunId}/abort")
    public Map<String, Object> abort(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String showRunId,
            @RequestBody(required = false) AbortRequest body
    ) {
        return mutate(tenantHeader, actorId, showRunId,
                (t, a) -> shows.abort(t, showRunId, body == null ? null : body.reason(), a));
    }

    private Map<String, Object> mutate(
            String tenantHeader,
            String actorId,
            String showRunId,
            ShowMutator mutator
    ) {
        try {
            ShowRun run = mutator.apply(
                    tenantOrDefault(tenantHeader),
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
            return toView(run);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private interface ShowMutator {
        ShowRun apply(String tenantId, Actor actor);
    }

    private String tenantOrDefault(String header) {
        return header == null || header.isBlank() ? defaultTenant : header;
    }

    private String siteOrDefault(String header) {
        return header == null || header.isBlank() ? defaultSite : header;
    }

    private Map<String, Object> toView(ShowRun run) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", run.getId());
        m.put("tenantId", run.getTenantId());
        m.put("siteId", run.getSiteId());
        m.put("robotId", run.getRobotId());
        m.put("assetId", run.getAssetId());
        m.put("assetVersion", run.getAssetVersion());
        m.put("assetHash", run.getAssetHash());
        m.put("status", run.getStatus().name());
        m.put("startCount", run.getStartCount());
        m.put("meta", run.getMeta());
        m.put("createdAt", run.getCreatedAt().toString());
        m.put("updatedAt", run.getUpdatedAt().toString());
        return m;
    }
}
