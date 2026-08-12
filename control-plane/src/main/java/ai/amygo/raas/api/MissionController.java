package ai.amygo.raas.api;

import ai.amygo.raas.application.MissionApplicationService;
import ai.amygo.raas.domain.mission.Task;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.persistence.InMemoryStore;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class MissionController {
    private final MissionApplicationService missions;
    private final InMemoryStore store;
    private final String defaultTenant;
    private final String defaultSite;

    public MissionController(
            MissionApplicationService missions,
            InMemoryStore store,
            @Value("${raas.demo-tenant-id}") String defaultTenant,
            @Value("${raas.demo-site-id}") String defaultSite
    ) {
        this.missions = missions;
        this.store = store;
        this.defaultTenant = defaultTenant;
        this.defaultSite = defaultSite;
    }

    public record CreateTaskRequest(
            String taskType,
            Map<String, Object> payload
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
        Task task = missions.createAndQueueDelivery(
                tenantId,
                siteId,
                payload,
                Actor.user(actorId == null ? "console-user" : actorId)
        );
        return toTaskView(task);
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

    @PostMapping("/tasks/{taskId}/cancel")
    public Map<String, Object> cancel(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @PathVariable @NotBlank String taskId
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        Task task = missions.cancel(tenantId, taskId, Actor.user(actorId == null ? "console-user" : actorId));
        return toTaskView(task);
    }

    @GetMapping("/robots")
    public List<Map<String, Object>> robots(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-Site-Id", required = false) String siteHeader
    ) {
        String tenantId = tenantHeader == null || tenantHeader.isBlank() ? defaultTenant : tenantHeader;
        String siteId = siteHeader == null || siteHeader.isBlank() ? defaultSite : siteHeader;
        return store.listRobots(tenantId, siteId).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("displayName", r.getDisplayName());
            m.put("modelProfile", r.getModelProfile());
            m.put("adapterType", r.getAdapterType());
            m.put("connectivityStatus", r.getConnectivityStatus());
            m.put("operationalStatus", r.getOperationalStatus());
            m.put("missionStatus", r.getMissionStatus());
            m.put("batteryStatus", r.getBatteryStatus());
            m.put("safetyStatus", r.getSafetyStatus());
            m.put("leaseTaskId", r.getLeaseTaskId());
            return m;
        }).toList();
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
        return store.listAudit(tenantId);
    }

    private Map<String, Object> toTaskView(Task task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("tenantId", task.getTenantId());
        m.put("siteId", task.getSiteId());
        m.put("taskType", task.getTaskType());
        m.put("status", task.getStatus());
        m.put("assignedRobotId", task.getAssignedRobotId());
        m.put("activeAssignmentId", task.getActiveAssignmentId());
        m.put("payload", task.getPayload());
        m.put("version", task.getVersion());
        m.put("createdAt", task.getCreatedAt().toString());
        m.put("updatedAt", task.getUpdatedAt().toString());
        return m;
    }
}
