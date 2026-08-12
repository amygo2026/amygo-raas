package ai.amygo.raas.application;

import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.persistence.AuditRepository;
import ai.amygo.raas.persistence.InMemoryStore;
import ai.amygo.raas.persistence.OutboxRepository;
import ai.amygo.raas.persistence.VendorBindingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class VendorBindingService {
    private static final Set<String> VENDORS = Set.of("PUDU", "KEENON", "UNITREE", "SIMULATOR");

    private final VendorBindingRepository bindings;
    private final InMemoryStore store;
    private final AuditRepository auditRepository;
    private final OutboxRepository outbox;

    public VendorBindingService(
            VendorBindingRepository bindings,
            InMemoryStore store,
            AuditRepository auditRepository,
            OutboxRepository outbox
    ) {
        this.bindings = bindings;
        this.store = store;
        this.auditRepository = auditRepository;
        this.outbox = outbox;
    }

    public Map<String, Object> bind(
            String tenantId,
            String siteId,
            String robotId,
            String vendorType,
            String vendorDeviceRef,
            String notes,
            Actor actor
    ) {
        if (vendorDeviceRef == null || vendorDeviceRef.isBlank()) {
            throw new IllegalArgumentException("vendorDeviceRef is required");
        }
        String vendor = vendorType == null ? "" : vendorType.trim().toUpperCase(Locale.ROOT);
        if (!VENDORS.contains(vendor)) {
            throw new IllegalArgumentException("Unsupported vendorType: " + vendorType);
        }
        Robot robot = store.findRobot(robotId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getSiteId().equals(siteId))
                .orElseThrow(() -> new IllegalArgumentException("Robot not found in tenant/site"));

        // Without formal vendor docs, binding stays MOCK_BOUND (not ACTIVE/Supported).
        String status = "SIMULATOR".equals(vendor) ? "ACTIVE" : "MOCK_BOUND";
        String id = Ids.newId();
        try {
            bindings.insert(id, tenantId, siteId, robotId, vendor, vendorDeviceRef.trim(), status, notes);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("vendorDeviceRef already bound in tenant");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("robotId", robot.getId());
        detail.put("vendorType", vendor);
        detail.put("vendorDeviceRef", vendorDeviceRef.trim());
        detail.put("status", status);
        detail.put("adapterType", robot.getAdapterType());
        audit(tenantId, actor, "robot.binding.created", "RobotBinding", id, detail);

        return bindings.find(tenantId, id).orElseThrow();
    }

    public List<Map<String, Object>> list(String tenantId, String siteId) {
        return bindings.list(tenantId, siteId);
    }

    public void unbind(String tenantId, String bindingId, Actor actor) {
        Map<String, Object> existing = bindings.find(tenantId, bindingId)
                .orElseThrow(() -> new IllegalArgumentException("Binding not found"));
        if (!bindings.delete(tenantId, bindingId)) {
            throw new IllegalArgumentException("Binding not found");
        }
        audit(tenantId, actor, "robot.binding.deleted", "RobotBinding", bindingId, Map.of(
                "robotId", existing.get("robot_id"),
                "vendorType", existing.get("vendor_type")
        ));
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
