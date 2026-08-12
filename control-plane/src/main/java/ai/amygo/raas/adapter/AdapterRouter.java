package ai.amygo.raas.adapter;

import ai.amygo.raas.adapter.keenon.KeenonMockRobotAdapter;
import ai.amygo.raas.adapter.pudu.PuduMockRobotAdapter;
import ai.amygo.raas.adapter.simulator.SimulatorRobotAdapter;
import ai.amygo.raas.domain.robot.RobotSnapshot;
import ai.amygo.raas.domain.shared.CommandEnvelope;
import ai.amygo.raas.domain.shared.CommandReceipt;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.InMemoryStore;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Routes commands to the adapter matching robot.adapterType.
 */
@Component
@Primary
public class AdapterRouter implements RobotAdapter {
    private final InMemoryStore store;
    private final Map<String, RobotAdapter> byType = new LinkedHashMap<>();
    private final SimulatorRobotAdapter simulator;

    public AdapterRouter(
            InMemoryStore store,
            SimulatorRobotAdapter simulator,
            PuduMockRobotAdapter pudu,
            KeenonMockRobotAdapter keenon
    ) {
        this.store = store;
        this.simulator = simulator;
        byType.put(simulator.descriptor().adapterType(), simulator);
        byType.put(pudu.descriptor().adapterType(), pudu);
        byType.put(keenon.descriptor().adapterType(), keenon);
    }

    @Override
    public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("ROUTER", "0.2.0", "Simulator");
    }

    @Override
    public Set<String> capabilities(String robotId) {
        return resolve(robotId).capabilities(robotId);
    }

    @Override
    public RobotSnapshot getSnapshot(String robotId) {
        return resolve(robotId).getSnapshot(robotId);
    }

    @Override
    public CommandReceipt submit(CommandEnvelope command) {
        return resolve(command.robotId()).submit(command);
    }

    @Override
    public CommandReceipt cancel(CommandEnvelope command) {
        return resolve(command.robotId()).cancel(command);
    }

    @Override
    public void subscribe(Consumer<RobotEvent> listener) {
        for (RobotAdapter adapter : byType.values()) {
            adapter.subscribe(listener);
        }
    }

    public Map<String, AdapterDescriptor> registered() {
        Map<String, AdapterDescriptor> out = new LinkedHashMap<>();
        byType.forEach((k, v) -> out.put(k, v.descriptor()));
        return out;
    }

    private RobotAdapter resolve(String robotId) {
        return store.findRobot(robotId)
                .map(r -> byType.getOrDefault(r.getAdapterType(), simulator))
                .orElse(simulator);
    }
}
