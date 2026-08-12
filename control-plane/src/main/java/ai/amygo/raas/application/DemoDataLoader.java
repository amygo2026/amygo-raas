package ai.amygo.raas.application;

import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.Ids;
import ai.amygo.raas.persistence.InMemoryStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader {
    private final InMemoryStore store;
    private final String tenantId;
    private final String siteId;

    public DemoDataLoader(
            InMemoryStore store,
            @Value("${raas.demo-tenant-id}") String tenantId,
            @Value("${raas.demo-site-id}") String siteId
    ) {
        this.store = store;
        this.tenantId = tenantId;
        this.siteId = siteId;
    }

    @PostConstruct
    void load() {
        store.saveRobot(new Robot(
                "robot-sim-01",
                tenantId,
                siteId,
                "Simulator Bot 01",
                "sim.delivery.v1",
                "SIMULATOR"
        ));
        store.saveRobot(new Robot(
                "robot-sim-02",
                tenantId,
                siteId,
                "Simulator Bot 02",
                "sim.delivery.v1",
                "SIMULATOR"
        ));
        // keep deterministic ids for demo UX
        if (store.findRobot("robot-sim-01").isEmpty()) {
            store.saveRobot(new Robot(Ids.newId(), tenantId, siteId, "extra", "sim.delivery.v1", "SIMULATOR"));
        }
    }
}
