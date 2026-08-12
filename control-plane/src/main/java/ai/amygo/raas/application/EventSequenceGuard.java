package ai.amygo.raas.application;

import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.WatermarkRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dedupe by eventId elsewhere; here enforce (source, robotId, sequence) ordering.
 * Gaps are buffered; late sequences (seq &lt;= watermark) are dropped.
 * Watermarks are mirrored to {@code event_sequence_watermark} when a repository is present.
 */
@Component
public class EventSequenceGuard {
    private static final int MAX_BUFFER_PER_KEY = 64;

    private final WatermarkRepository watermarkRepository;
    private final Map<String, Long> watermark = new ConcurrentHashMap<>();
    private final Map<String, List<RobotEvent>> buffer = new ConcurrentHashMap<>();
    private final AtomicLong applied = new AtomicLong();
    private final AtomicLong droppedLate = new AtomicLong();
    private final AtomicLong buffered = new AtomicLong();
    private final AtomicLong bufferEvicted = new AtomicLong();
    private final AtomicLong persisted = new AtomicLong();

    public EventSequenceGuard(WatermarkRepository watermarkRepository) {
        this.watermarkRepository = watermarkRepository;
    }

    @PostConstruct
    void loadPersisted() {
        if (watermarkRepository == null) {
            return;
        }
        watermark.putAll(watermarkRepository.loadAll());
    }

    public synchronized Decision accept(RobotEvent event) {
        String key = key(event.source(), event.robotId());
        long last = watermark.getOrDefault(key, 0L);
        long seq = event.sequence();

        if (seq <= last) {
            droppedLate.incrementAndGet();
            return new Decision(false, List.of());
        }

        if (seq == last + 1) {
            advance(key, event.source(), event.robotId(), seq);
            applied.incrementAndGet();
            List<RobotEvent> drain = drain(key, seq);
            return new Decision(true, drain);
        }

        // gap: buffer
        List<RobotEvent> pending = buffer.computeIfAbsent(key, k -> new ArrayList<>());
        boolean exists = pending.stream().anyMatch(e -> e.sequence() == seq);
        if (!exists) {
            if (pending.size() >= MAX_BUFFER_PER_KEY) {
                pending.sort(Comparator.comparingLong(RobotEvent::sequence));
                pending.remove(0);
                bufferEvicted.incrementAndGet();
            }
            pending.add(event);
            buffered.incrementAndGet();
        }
        return new Decision(false, List.of());
    }

    private List<RobotEvent> drain(String key, long afterSeq) {
        List<RobotEvent> pending = buffer.get(key);
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        pending.sort(Comparator.comparingLong(RobotEvent::sequence));
        List<RobotEvent> ready = new ArrayList<>();
        long expect = afterSeq + 1;
        while (true) {
            RobotEvent next = null;
            for (RobotEvent e : pending) {
                if (e.sequence() == expect) {
                    next = e;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            pending.remove(next);
            advance(key, next.source(), next.robotId(), expect);
            applied.incrementAndGet();
            ready.add(next);
            expect++;
        }
        if (pending.isEmpty()) {
            buffer.remove(key);
        }
        return ready;
    }

    private void advance(String key, String source, String robotId, long seq) {
        watermark.put(key, seq);
        if (watermarkRepository != null) {
            watermarkRepository.upsert(source, robotId, seq);
            persisted.incrementAndGet();
        }
    }

    public Map<String, Long> metrics() {
        return Map.of(
                "applied", applied.get(),
                "droppedLate", droppedLate.get(),
                "buffered", buffered.get(),
                "bufferEvicted", bufferEvicted.get(),
                "watermarks", (long) watermark.size(),
                "persisted", persisted.get()
        );
    }

    public long memoryWatermark(String source, String robotId) {
        return watermark.getOrDefault(key(source, robotId), 0L);
    }

    /** Seed for adapter sequence counters after process restart. */
    public long currentWatermark(String source, String robotId) {
        return memoryWatermark(source, robotId);
    }

    public record Decision(boolean applyNow, List<RobotEvent> drainAfter) {}

    private static String key(String source, String robotId) {
        return WatermarkRepository.key(source, robotId);
    }
}
