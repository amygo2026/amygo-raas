package ai.amygo.raas.adapter;

import ai.amygo.raas.adapter.simulator.SimulatorRobotAdapter;
import ai.amygo.raas.domain.shared.Actor;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.CommandReceiptStatus;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter TCK baseline against Simulator.
 * Real vendor adapters must pass the same assertions before Beta/Supported.
 */
class AdapterTckTest {
    private InMemoryStore store;
    private SimulatorRobotAdapter adapter;
    private final List<RobotEvent> events = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        store.saveRobot(new ai.amygo.raas.domain.robot.Robot(
                "robot-tck-1", "tenant-demo", "site-demo", "TCK Bot", "sim.delivery.v1", "SIMULATOR"));
        adapter = new SimulatorRobotAdapter(store, "none");
        events.clear();
        adapter.subscribe(events::add);
    }

    @Test
    void descriptorAndCapabilities() {
        assertThat(adapter.descriptor().adapterType()).isEqualTo("SIMULATOR");
        assertThat(adapter.capabilities("robot-tck-1")).contains("delivery", "navigation");
        assertThat(adapter.getSnapshot("robot-tck-1").schedulable()).isTrue();
    }

    @Test
    void submitIsIdempotent() {
        CommandEnvelope cmd = command("DELIVERY_START", "idem-1", Instant.now().plus(Duration.ofMinutes(1)));
        CommandReceipt first = adapter.submit(cmd);
        CommandReceipt second = adapter.submit(cmd);
        assertThat(first.status()).isEqualTo(CommandReceiptStatus.ACCEPTED);
        assertThat(second.status()).isEqualTo(CommandReceiptStatus.ACCEPTED);
        assertThat(second.reasonCode()).isEqualTo("IDEMPOTENT_REPLAY");
    }

    @Test
    void expiredCommandRejected() {
        CommandEnvelope cmd = command("DELIVERY_START", "idem-exp", Instant.now().minusSeconds(5));
        CommandReceipt receipt = adapter.submit(cmd);
        assertThat(receipt.status()).isEqualTo(CommandReceiptStatus.REJECTED);
        assertThat(receipt.reasonCode()).isEqualTo("COMMAND_EXPIRED");
    }

    @Test
    void unsupportedCapabilityRejected() {
        CommandEnvelope cmd = command("DANCE_NOW", "idem-cap", Instant.now().plus(Duration.ofMinutes(1)));
        CommandReceipt receipt = adapter.submit(cmd);
        assertThat(receipt.status()).isEqualTo(CommandReceiptStatus.REJECTED);
        assertThat(receipt.reasonCode()).isEqualTo("CAPABILITY_NOT_SUPPORTED");
    }

    @Test
    void failModeRejectsSubmit() {
        SimulatorRobotAdapter failing = new SimulatorRobotAdapter(store, "fail_on_submit");
        CommandReceipt receipt = failing.submit(command("DELIVERY_START", "idem-fail", Instant.now().plus(Duration.ofMinutes(1))));
        assertThat(receipt.status()).isEqualTo(CommandReceiptStatus.REJECTED);
        assertThat(receipt.reasonCode()).isEqualTo("VENDOR_TEMPORARY_ERROR");
    }

    private CommandEnvelope command(String type, String idem, Instant expiresAt) {
        return new CommandEnvelope(
                Ids.newId(),
                "corr-1",
                "tenant-demo",
                "site-demo",
                "robot-tck-1",
                type,
                idem,
                Instant.now(),
                expiresAt,
                Actor.system("tck"),
                Map.of("taskId", "task-tck-1")
        );
    }
}
