package ai.amygo.raas.domain.mission;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStateMachineTest {
    @Test
    void happyPath() {
        Task task = new Task("t1", "tenant", "site", "DELIVERY", Map.of());
        task.queue();
        task.assign("r1", "a1");
        task.markDispatching();
        task.markRunning();
        task.markSucceeded();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(task.getStatus().isTerminal()).isTrue();
    }

    @Test
    void terminalCannotReturnToRunning() {
        Task task = new Task("t1", "tenant", "site", "DELIVERY", Map.of());
        task.queue();
        task.assign("r1", "a1");
        task.markDispatching();
        task.markRunning();
        task.markSucceeded();
        assertThatThrownBy(task::markRunning).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegalSkipRejected() {
        Task task = new Task("t1", "tenant", "site", "DELIVERY", Map.of());
        assertThatThrownBy(task::markRunning).isInstanceOf(IllegalStateException.class);
    }
}
