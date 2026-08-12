package ai.amygo.raas.application;

import ai.amygo.raas.domain.shared.RobotEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventSequenceGuardTest {

    @Test
    void buffersGapThenDrainsInOrder() {
        EventSequenceGuard guard = new EventSequenceGuard(null);
        RobotEvent completed = event("e-c", "task.completed", 3);
        RobotEvent running = event("e-r", "task.running", 2);
        RobotEvent accepted = event("e-a", "command.accepted", 1);

        EventSequenceGuard.Decision d1 = guard.accept(completed);
        assertThat(d1.applyNow()).isFalse();

        EventSequenceGuard.Decision d2 = guard.accept(accepted);
        assertThat(d2.applyNow()).isTrue();
        assertThat(d2.drainAfter()).isEmpty();

        EventSequenceGuard.Decision d3 = guard.accept(running);
        assertThat(d3.applyNow()).isTrue();
        assertThat(d3.drainAfter()).extracting(RobotEvent::eventId).containsExactly("e-c");

        assertThat(guard.metrics().get("droppedLate")).isZero();
    }

    @Test
    void dropsLateSequence() {
        EventSequenceGuard guard = new EventSequenceGuard(null);
        assertThat(guard.accept(event("a", "command.accepted", 1)).applyNow()).isTrue();
        assertThat(guard.accept(event("b", "task.running", 2)).applyNow()).isTrue();
        EventSequenceGuard.Decision late = guard.accept(event("late", "task.running", 1));
        assertThat(late.applyNow()).isFalse();
        assertThat(guard.metrics().get("droppedLate")).isEqualTo(1L);
    }

    private static RobotEvent event(String id, String type, long seq) {
        return new RobotEvent(
                id, type, "1.0", "t", "s", "robot-1", "task-1", seq,
                Instant.now(), Instant.now(), "SIMULATOR", "corr", Map.of()
        );
    }
}
