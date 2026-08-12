package ai.amygo.raas.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class VendorBindingRepository {
    private final JdbcTemplate jdbc;

    public VendorBindingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(
            String id,
            String tenantId,
            String siteId,
            String robotId,
            String vendorType,
            String vendorDeviceRef,
            String status,
            String notes
    ) {
        jdbc.update(
                """
                INSERT INTO robot_vendor_binding
                  (id, tenant_id, site_id, robot_id, vendor_type, vendor_device_ref, status, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, tenantId, siteId, robotId, vendorType, vendorDeviceRef, status, notes
        );
    }

    public List<Map<String, Object>> list(String tenantId, String siteId) {
        return jdbc.queryForList(
                """
                SELECT id, tenant_id, site_id, robot_id, vendor_type, vendor_device_ref, status, notes, created_at, updated_at
                FROM robot_vendor_binding
                WHERE tenant_id = ? AND site_id = ?
                ORDER BY created_at DESC
                """,
                tenantId, siteId
        );
    }

    public Optional<Map<String, Object>> find(String tenantId, String bindingId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT id, tenant_id, site_id, robot_id, vendor_type, vendor_device_ref, status, notes, created_at, updated_at
                FROM robot_vendor_binding
                WHERE tenant_id = ? AND id = ?
                """,
                tenantId, bindingId
        );
        return rows.stream().findFirst();
    }

    public boolean delete(String tenantId, String bindingId) {
        return jdbc.update(
                "DELETE FROM robot_vendor_binding WHERE tenant_id = ? AND id = ?",
                tenantId, bindingId
        ) > 0;
    }

    public void touch(String tenantId, String bindingId, String status) {
        jdbc.update(
                "UPDATE robot_vendor_binding SET status = ?, updated_at = ? WHERE tenant_id = ? AND id = ?",
                status, Timestamp.from(Instant.now()), tenantId, bindingId
        );
    }
}
