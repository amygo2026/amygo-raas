package ai.amygo.raas.application;

import ai.amygo.raas.persistence.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polls unpublished outbox rows and delivers to an in-process sink.
 * At-least-once: rows stay unpublished until markPublished; sink dedupes by outbox id.
 */
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final Map<Long, Map<String, Object>> sink = new ConcurrentHashMap<>();
    private final AtomicLong publishRuns = new AtomicLong();
    private final AtomicLong publishedTotal = new AtomicLong();

    public OutboxPublisher(OutboxRepository outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${raas.outbox.poll-interval-ms:500}")
    public void poll() {
        publishBatch(50);
    }

    public synchronized int publishBatch(int limit) {
        publishRuns.incrementAndGet();
        List<OutboxRepository.OutboxRow> rows = outbox.claimUnpublished(limit);
        int ok = 0;
        for (OutboxRepository.OutboxRow row : rows) {
            try {
                deliver(row);
                outbox.markPublished(row.id());
                publishedTotal.incrementAndGet();
                ok++;
            } catch (Exception ex) {
                outbox.markError(row.id(), ex.getMessage());
                log.warn("outbox publish failed id={}: {}", row.id(), ex.getMessage());
            }
        }
        return ok;
    }

    private void deliver(OutboxRepository.OutboxRow row) {
        // Idempotent sink: same outbox id overwrites / no-ops
        sink.putIfAbsent(row.id(), Map.of(
                "tenantId", row.tenantId(),
                "aggregateType", row.aggregateType(),
                "aggregateId", row.aggregateId(),
                "eventType", row.eventType(),
                "payload", row.payload() == null ? Map.of() : row.payload()
        ));
    }

    public int sinkSize() {
        return sink.size();
    }

    public List<Map<String, Object>> sinkSnapshot() {
        return new ArrayList<>(sink.values());
    }

    public Map<String, Long> metrics() {
        return Map.of(
                "publishRuns", publishRuns.get(),
                "publishedTotal", publishedTotal.get(),
                "sinkSize", (long) sink.size(),
                "unpublished", (long) outbox.unpublishedCount(),
                "publishedDb", (long) outbox.publishedCount()
        );
    }
}
