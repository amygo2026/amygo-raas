package ai.amygo.raas.application;

import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.persistence.InMemoryStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader {
    private final InMemoryStore store;
    private final JdbcTemplate jdbc;
    private final String tenantId;
    private final String siteId;

    public DemoDataLoader(
            InMemoryStore store,
            JdbcTemplate jdbc,
            @Value("${raas.demo-tenant-id}") String tenantId,
            @Value("${raas.demo-site-id}") String siteId
    ) {
        this.store = store;
        this.jdbc = jdbc;
        this.tenantId = tenantId;
        this.siteId = siteId;
    }

    @PostConstruct
    void load() {
        jdbc.update(
                "DELETE FROM tenant WHERE id = ?",
                tenantId
        );
        jdbc.update("INSERT INTO tenant (id, name) VALUES (?, ?)", tenantId, "Demo Tenant");
        jdbc.update("DELETE FROM site WHERE id = ?", siteId);
        jdbc.update(
                "INSERT INTO site (id, tenant_id, name, timezone) VALUES (?, ?, ?, ?)",
                siteId, tenantId, "Demo Site", "UTC"
        );

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
                "sim.cleaning.v1",
                "SIMULATOR"
        ));
        store.saveRobot(new Robot(
                "robot-sim-03",
                tenantId,
                siteId,
                "Simulator Bot 03",
                "sim.hotel.v1",
                "SIMULATOR"
        ));
        store.saveRobot(new Robot(
                "robot-pudu-mock-01",
                tenantId,
                siteId,
                "PUDU Mock Delivery 01",
                "pudu.delivery.mock.v1",
                "PUDU"
        ));
        store.saveRobot(new Robot(
                "robot-keenon-mock-01",
                tenantId,
                siteId,
                "KEENON Mock Delivery 01",
                "keenon.delivery.mock.v1",
                "KEENON"
        ));
        store.saveRobot(new Robot(
                "robot-unitree-show-mock-01",
                tenantId,
                siteId,
                "Unitree Show Mock 01",
                "unitree.show.mock.v1",
                "UNITREE"
        ));
    }
}
