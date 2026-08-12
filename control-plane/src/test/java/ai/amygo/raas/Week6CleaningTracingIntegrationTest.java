package ai.amygo.raas;

import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.persistence.InMemoryStore;
import com.fasterxml.jackson.databind.JsonNode;
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
class Week6CleaningTracingIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
        registry.add("raas.pudu-mock.fault-mode", () -> "none");
        registry.add("management.tracing.sampling.probability", () -> "1.0");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InMemoryStore store;

    @Test
    void cleaning_task_prefers_cleaning_profile_and_succeeds() throws Exception {
        String siteId = "site-w6-clean";
        store.saveRobot(new Robot(
                "robot-w6-delivery",
                "tenant-demo",
                siteId,
                "Delivery Prefer",
                "sim.delivery.v1",
                "SIMULATOR"
        ));
        store.saveRobot(new Robot(
                "robot-w6-clean",
                "tenant-demo",
                siteId,
                "Clean Prefer",
                "sim.cleaning.v1",
                "SIMULATOR"
        ));

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "taskType": "CLEANING",
                                  "payload": { "zoneId": "lobby-A", "note": "w6-clean" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskType").value("CLEANING"))
                .andReturn();
        String taskId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode body = mapper.readTree(mockMvc.perform(get("/api/v1/tasks/" + taskId)
                            .header("X-Tenant-Id", "tenant-demo"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
            assertThat(body.get("status").asText()).isEqualTo("SUCCEEDED");
            assertThat(body.get("assignedRobotId").asText()).isEqualTo("robot-w6-clean");
        });
    }

    @Test
    void unsupported_task_type_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo")
                        .content("""
                                { "taskType": "TELEOP_DRIVE", "payload": {} }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ops_metrics_exposes_tracing_bridge() throws Exception {
        mockMvc.perform(get("/api/v1/ops/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracing.bridge").value("micrometer-otel"))
                .andExpect(jsonPath("$.tracing.samplingProbability").value(1.0));
    }
}
