package ai.amygo.raas.adapter;

import ai.amygo.raas.domain.robot.RobotSnapshot;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.RobotEvent;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Vendor-agnostic robot adapter port.
 * Vendor SDKs/DTOs must stay outside domain and this contract's consumers.
 */
public interface RobotAdapter {
    AdapterDescriptor descriptor();

    Set<String> capabilities(String robotId);

    RobotSnapshot getSnapshot(String robotId);

    CommandReceipt submit(CommandEnvelope command);

    CommandReceipt cancel(CommandEnvelope command);

    void subscribe(Consumer<RobotEvent> listener);
}
