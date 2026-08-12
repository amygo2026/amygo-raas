package ai.amygo.raas.api;

import ai.amygo.raas.adapter.AdapterRouter;
import ai.amygo.raas.application.MissionApplicationService;
import ai.amygo.raas.application.OutboxPublisher;
import ai.amygo.raas.application.VendorBindingService;
import ai.amygo.raas.domain.mission.Task;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.persistence.AuditRepository;
import ai.amygo.raas.persistence.InMemoryStore;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class MissionController {
    private final MissionApplicationService missions;
    private final InMemoryStore store;
    private final AuditRepository auditRepository;
    private final OutboxPublisher outboxPublisher;
    private final VendorBindingService bindings;
    private final AdapterRouter adapterRouter;
    private final String defaultTenant;
    private final String defaultSite;
    private final double tracingSampling;

    public MissionController(
            MissionApplicationService missions,
            InMemoryStore store,
            AuditRepository auditRepository,
            OutboxPublisher outboxPublisher,
            VendorBindingService bindings,
            AdapterRouter adapterRouter,
            @Value("${raas.demo-tenant-id}") String defaultTenant,
            @Value("${raas.demo-site-id}") String defaultSite,
            @Value("${management.tracing.sampling.probability:1.0}") double tracingSampling
    ) {
        this.missions = missions;
        this.store = store;
        this.auditRepository = auditRepository;
        this.outboxPublisher = outboxPublisher;
        this.bindings = bindings;
        this.adapterRouter = adapterRouter;
        this.defaultTenant = defaultTenant;
        this.defaultSite = defaultSite;
        this.tracingSampling = tracingSampling;
    }

    public record CreateTaskRequest(
            String taskType,
            Map<String, Object> payload
    ) {}

    public record FailTaskRequest(String reason) {}

    public record BindRequest(
            String robotId,
            String vendorType,
            String vendorDeviceRef,
            String notes
    ) {}

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTask(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @RequestBody CreateTaskRequest request
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        try {
            Task task = missions.createAndQueue(
                    tenantId,
                    siteId,
                    request.taskType(),
                    payload,
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
            return toTaskView(task);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> listTasks(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        return store.listTasks(tenantId, siteId).stream().map(this::toTaskView).toList();
    }

    @GetMapping("/tasks/{taskId}")
    public Map<String, Object> getTask(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable String taskId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        Task task = store.findTask(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toTaskView(task);
    }

    @GetMapping("/tasks/{taskId}/timeline")
    public List<Map<String, Object>> timeline(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable String taskId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        try {
            return missions.timeline(tenantId, taskId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public Map<String, Object> cancel(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable @NotBlank String taskId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        try {
            Task task = missions.cancel(tenantId, taskId, Actor.user(actorId == null ? "console-user" : actorId));
            return toTaskView(task);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/fail")
    public Map<String, Object> fail(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable @NotBlank String taskId,
            @RequestBody(required = false) FailTaskRequest body
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String reason = body == null ? null : body.reason();
        try {
            Task task = missions.fail(tenantId, taskId, reason, Actor.user(actorId == null ? "console-user" : actorId));
            return toTaskView(task);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/restart")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> restart(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable @NotBlank String taskId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        try {
            Task task = missions.restart(tenantId, taskId, Actor.user(actorId == null ? "console-user" : actorId));
            return toTaskView(task);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @GetMapping("/robots")
    public List<Map<String, Object>> robots(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        return store.listRobots(tenantId, siteId).stream().map(this::toRobotView).toList();
    }

    @PostMapping("/robots/{robotId}/reconnect")
    public Map<String, Object> reconnect(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable @NotBlank String robotId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        try {
            return toRobotView(missions.reconnect(
                    tenantId,
                    siteId,
                    robotId,
                    Actor.user(actorId == null ? "console-user" : actorId)
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/events")
    public Object events(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        return store.listEvents(tenantId, siteId);
    }

    @GetMapping("/audit")
    public Object audit(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        return auditRepository.list(tenantId);
    }

    @GetMapping("/adapters")
    public Object adapters() {
        return adapterRouter.registered();
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> listBindings(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        return bindings.list(tenantId, siteId);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBinding(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @RequestBody BindRequest request
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        try {
            return bindings.bind(
                    tenantId,
                    siteId,
                    request.robotId(),
                    request.vendorType(),
                    request.vendorDeviceRef(),
                    request.notes(),
                    Actor.user(actorId == null ? "console-user" : actorId)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @DeleteMapping("/bindings/{bindingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBinding(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable String bindingId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        try {
            bindings.unbind(tenantId, bindingId, Actor.user(actorId == null ? "console-user" : actorId));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/ops/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sequence", missions.sequenceMetrics());
        m.put("outbox", outboxPublisher.metrics());
        m.put("unknownCommands", store.listUnknownCommands().size());
        m.put("offlinePolicy", missions.offlinePolicy().name());
        m.put("tracing", Map.of(
                "bridge", "micrometer-otel",
                "samplingProbability", tracingSampling
        ));
        return m;
    }

    @GetMapping("/commands/unknown")
    public Object unknownCommands() {
        return store.listUnknownCommands();
    }

    private Map<String, Object> toRobotView(Robot r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("displayName", r.getDisplayName());
        m.put("modelProfile", r.getModelProfile());
        m.put("adapterType", r.getAdapterType());
        m.put("connectivityStatus", r.getConnectivityStatus());
        m.put("operationalStatus", r.getOperationalStatus());
        m.put("missionStatus", r.getMissionStatus());
        m.put("batteryStatus", r.getBatteryStatus());
        m.put("localizationStatus", r.getLocalizationStatus());
        m.put("safetyStatus", r.getSafetyStatus());
        m.put("maintenanceStatus", r.getMaintenanceStatus());
        m.put("leaseTaskId", r.getLeaseTaskId());
        m.put("leaseExpiresAt", r.getLeaseExpiresAt() == null ? null : r.getLeaseExpiresAt().toString());
        return m;
    }

    private Map<String, Object> toTaskView(Task task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("tenantId", task.getTenantId());
        m.put("siteId", task.getSiteId());
        m.put("taskType", task.getTaskType());
        m.put("status", task.getStatus());
        m.put("attemptNo", task.getAttemptNo());
        m.put("assignedRobotId", task.getAssignedRobotId());
        m.put("activeAssignmentId", task.getActiveAssignmentId());
        m.put("payload", task.getPayload());
        m.put("version", task.getVersion());
        m.put("createdAt", task.getCreatedAt().toString());
        m.put("updatedAt", task.getUpdatedAt().toString());
        return m;
    }
}
