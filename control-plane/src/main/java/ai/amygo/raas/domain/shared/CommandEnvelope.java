package ai.amygo.raas.domain.shared;

import java.time.Instant;
import java.util.Map;

public record CommandEnvelope(
        String commandId,
        String correlationId,
        String tenantId,
        String siteId,
        String robotId,
        String commandType,
        String idempotencyKey,
        Instant issuedAt,
        Instant expiresAt,
        Actor actor,
        Map<String, Object> payload
) {}
