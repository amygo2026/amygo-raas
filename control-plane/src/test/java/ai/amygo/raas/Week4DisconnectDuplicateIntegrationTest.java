package ai.amygo.raas;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week4DisconnectDuplicateIntegrationTest {

    @DynamicPropertySource
    static void disconnect(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
        registry.add("raas.pudu-mock.fault-mode", () -> "disconnect_mid_mission");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InMemoryStore store;

    @Test
    void pudu_disconnect_mid_mission_fails_task_and_marks_offline() throws Exception {
        String siteId = "site-pudu-disconnect";
        store.saveRobot(new Robot(
                "robot-pudu-disc-1",
                "tenant-demo",
                siteId,
                "PUDU Disc Bot",
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
                                  "payload": { "pickupStationId": "p", "dropoffStationId": "d" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("FAILED"))
        );

        assertThat(store.findRobot("robot-pudu-disc-1").orElseThrow().getConnectivityStatus().name())
                .isEqualTo("OFFLINE");
    }
}
