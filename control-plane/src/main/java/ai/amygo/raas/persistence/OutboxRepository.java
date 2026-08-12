package ai.amygo.raas.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void append(String tenantId, String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        jdbc.update(
                """
                INSERT INTO outbox (tenant_id, aggregate_type, aggregate_id, event_type, payload_json)
                VALUES (?, ?, ?, ?, ?)
                """,
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                write(payload)
        );
    }

    public int unpublishedCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM outbox WHERE published_at IS NULL", Integer.class);
        return n == null ? 0 : n;
    }

    private String write(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
