package ai.amygo.raas.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AuditRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void append(String tenantId, String actorType, String actorId, String action,
                       String objectType, String objectId, Map<String, Object> detail) {
        jdbc.update(
                """
                INSERT INTO audit_log (tenant_id, actor_type, actor_id, action, object_type, object_id, detail_json)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                tenantId, actorType, actorId, action, objectType, objectId, write(detail)
        );
    }

    public List<Map<String, Object>> list(String tenantId) {
        return jdbc.queryForList(
                """
                SELECT id, tenant_id, actor_type, actor_id, action, object_type, object_id, detail_json, created_at
                FROM audit_log
                WHERE tenant_id = ?
                ORDER BY id DESC
                """,
                tenantId
        );
    }

    private String write(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
