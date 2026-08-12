package ai.amygo.raas;

import ai.amygo.raas.application.EventSequenceGuard;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.RobotEvent;
import ai.amygo.raas.persistence.InMemoryStore;
import ai.amygo.raas.persistence.WatermarkRepository;
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
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week7HotelWatermarkIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
        registry.add("raas.pudu-mock.fault-mode", () -> "none");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InMemoryStore store;
    @Autowired WatermarkRepository watermarks;
    @Autowired EventSequenceGuard sequenceGuard;

    @Test
    void hotel_delivery_prefers_hotel_profile_and_succeeds() throws Exception {
        String siteId = "site-w7-hotel";
        store.saveRobot(new Robot(
                "robot-w7-delivery",
                "tenant-demo",
                siteId,
                "Delivery Only",
                "sim.delivery.v1",
                "SIMULATOR"
        ));
        store.saveRobot(new Robot(
                "robot-w7-hotel",
                "tenant-demo",
                siteId,
                "Hotel Cabin Bot",
                "sim.hotel.v1",
                "SIMULATOR"
        ));

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "taskType": "HOTEL_DELIVERY",
                                  "payload": {
                                    "roomNumber": "1208",
                                    "floor": 12,
                                    "compartment": "A",
                                    "note": "w7-hotel"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskType").value("HOTEL_DELIVERY"))
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
            assertThat(body.get("assignedRobotId").asText()).isEqualTo("robot-w7-hotel");
        });
    }

    @Test
    void watermark_persists_to_database() {
        RobotEvent e1 = new RobotEvent(
                "wm-1", "command.accepted", "1.0", "tenant-demo", "site-w7-wm",
                "robot-wm-1", "task-wm", 1L,
                Instant.now(), Instant.now(), "SIMULATOR", "corr", Map.of()
        );
        RobotEvent e2 = new RobotEvent(
                "wm-2", "task.running", "1.0", "tenant-demo", "site-w7-wm",
                "robot-wm-1", "task-wm", 2L,
                Instant.now(), Instant.now(), "SIMULATOR", "corr", Map.of()
        );
        assertThat(sequenceGuard.accept(e1).applyNow()).isTrue();
        assertThat(sequenceGuard.accept(e2).applyNow()).isTrue();
        assertThat(sequenceGuard.memoryWatermark("SIMULATOR", "robot-wm-1")).isEqualTo(2L);
        assertThat(watermarks.find("SIMULATOR", "robot-wm-1")).isEqualTo(2L);
        assertThat(watermarks.countRows()).isGreaterThanOrEqualTo(1L);
        assertThat(sequenceGuard.metrics().get("persisted")).isGreaterThanOrEqualTo(2L);
    }
}
