package ai.amygo.raas.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WatermarkRepository {
    private final JdbcTemplate jdbc;

    public WatermarkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Long> loadAll() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT source, robot_id, last_sequence FROM event_sequence_watermark"
        );
        Map<String, Long> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String source = String.valueOf(row.get("source"));
            String robotId = String.valueOf(row.get("robot_id"));
            Number seq = (Number) row.get("last_sequence");
            out.put(key(source, robotId), seq == null ? 0L : seq.longValue());
        }
        return out;
    }

    public Long find(String source, String robotId) {
        List<Long> rows = jdbc.query(
                "SELECT last_sequence FROM event_sequence_watermark WHERE source = ? AND robot_id = ?",
                (rs, i) -> rs.getLong(1),
                source,
                robotId
        );
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public void upsert(String source, String robotId, long lastSequence) {
        Instant now = Instant.now();
        int updated = jdbc.update(
                """
                UPDATE event_sequence_watermark
                SET last_sequence = ?, updated_at = ?
                WHERE source = ? AND robot_id = ?
                """,
                lastSequence,
                Timestamp.from(now),
                source,
                robotId
        );
        if (updated == 0) {
            jdbc.update(
                    """
                    INSERT INTO event_sequence_watermark (source, robot_id, last_sequence, updated_at)
                    VALUES (?, ?, ?, ?)
                    """,
                    source,
                    robotId,
                    lastSequence,
                    Timestamp.from(now)
            );
        }
    }

    public long countRows() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM event_sequence_watermark", Long.class);
        return n == null ? 0L : n;
    }

    public static String key(String source, String robotId) {
        return source + "::" + robotId;
    }
}
