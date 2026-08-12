package ai.amygo.raas;

import ai.amygo.raas.domain.robot.ConnectivityStatus;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.persistence.InMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week5ReconnectResyncIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
        registry.add("raas.pudu-mock.fault-mode", () -> "disconnect_mid_mission");
        registry.add("raas.ops.offline-policy", () -> "hold_on_disconnect");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InMemoryStore store;

    @Test
    void disconnect_hold_then_reconnect_restores_online_and_audits() throws Exception {
        String siteId = "site-w5-reconnect";
        store.saveRobot(new Robot(
                "robot-pudu-w5-1",
                "tenant-demo",
                siteId,
                "PUDU Hold Bot",
                "pudu.delivery.mock.v1",
                "PUDU"
        ));

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": { "pickupStationId": "p", "dropoffStationId": "d", "note": "w5-hold" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("NEEDS_INTERVENTION"))
        );

        assertThat(store.findRobot("robot-pudu-w5-1").orElseThrow().getConnectivityStatus())
                .isEqualTo(ConnectivityStatus.OFFLINE);

        mockMvc.perform(post("/api/v1/robots/robot-pudu-w5-1/reconnect")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .header("X-Actor-Id", "ops-w5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectivityStatus").value("ONLINE"))
                .andExpect(jsonPath("$.id").value("robot-pudu-w5-1"));

        assertThat(store.findRobot("robot-pudu-w5-1").orElseThrow().getConnectivityStatus())
                .isEqualTo(ConnectivityStatus.ONLINE);

        List<Map<String, Object>> audit = store.listAudit("tenant-demo");
        assertThat(audit.stream().anyMatch(a -> "robot.reconnected".equals(a.get("action")))).isTrue();
        assertThat(audit.stream().anyMatch(a -> "task.held_on_disconnect".equals(a.get("action")))).isTrue();

        // Operator closes held task then restarts lineage (new task); PUDU fault still injects — status may hold again.
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Actor-Id", "ops-w5")
                        .content("{\"reason\":\"operator_close_after_hold\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/restart")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Actor-Id", "ops-w5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptNo").value(2));

        mockMvc.perform(get("/api/v1/ops/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offlinePolicy").value("HOLD_ON_DISCONNECT"));
    }

    @Test
    void after_reconnect_simulator_site_still_completes_delivery() throws Exception {
        String siteId = "site-w5-sim-only";
        store.saveRobot(new Robot(
                "robot-sim-w5-1",
                "tenant-demo",
                siteId,
                "Sim Recovery Bot",
                "sim.delivery.v1",
                "SIMULATOR"
        ));

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": { "pickupStationId": "p", "dropoffStationId": "d", "note": "w5-sim" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        );
    }

    @Test
    void reconnect_unknown_robot_returns_404() throws Exception {
        mockMvc.perform(post("/api/v1/robots/missing-bot/reconnect")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo"))
                .andExpect(status().isNotFound());
    }
}
