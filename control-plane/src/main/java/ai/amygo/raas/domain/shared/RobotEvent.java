package ai.amygo.raas.domain.shared;

import java.time.Instant;
import java.util.Map;

public record RobotEvent(
        String eventId,
        String eventType,
        String schemaVersion,
        String tenantId,
        String siteId,
        String robotId,
        String taskId,
        long sequence,
        Instant occurredAt,
        Instant receivedAt,
        String source,
        String correlationId,
        Map<String, Object> payload
) {}
