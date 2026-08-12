package ai.amygo.raas.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public int publishedCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM outbox WHERE published_at IS NOT NULL", Integer.class);
        return n == null ? 0 : n;
    }

    /** Claim a batch of unpublished rows (at-least-once; safe with idempotent sinks). */
    public List<OutboxRow> claimUnpublished(int limit) {
        List<OutboxRow> rows = jdbc.query(
                """
                SELECT id, tenant_id, aggregate_type, aggregate_id, event_type, payload_json, publish_attempts
                FROM outbox
                WHERE published_at IS NULL
                ORDER BY id ASC
                LIMIT ?
                """,
                (rs, i) -> new OutboxRow(
                        rs.getLong("id"),
                        rs.getString("tenant_id"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        read(rs.getString("payload_json")),
                        rs.getInt("publish_attempts")
                ),
                limit
        );
        for (OutboxRow row : rows) {
            jdbc.update(
                    "UPDATE outbox SET publish_attempts = publish_attempts + 1 WHERE id = ? AND published_at IS NULL",
                    row.id()
            );
        }
        return rows;
    }

    public void markPublished(long id) {
        jdbc.update(
                "UPDATE outbox SET published_at = ?, last_error = NULL WHERE id = ? AND published_at IS NULL",
                Timestamp.from(Instant.now()),
                id
        );
    }

    public void markError(long id, String error) {
        String trimmed = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 500));
        jdbc.update("UPDATE outbox SET last_error = ? WHERE id = ?", trimmed, id);
    }

    private String write(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> read(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of("raw", Objects.toString(json));
        }
    }

    public record OutboxRow(
            long id,
            String tenantId,
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, Object> payload,
            int publishAttempts
    ) {}
}
