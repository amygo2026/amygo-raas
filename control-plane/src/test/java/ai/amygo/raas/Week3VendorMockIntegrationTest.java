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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week3VendorMockIntegrationTest {

    @DynamicPropertySource
    static void fastSimulator(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InMemoryStore store;

    @Test
    void pudu_mock_delivery_loop_and_binding() throws Exception {
        String siteId = "site-pudu-only";
        store.saveRobot(new Robot(
                "robot-pudu-only-1",
                "tenant-demo",
                siteId,
                "PUDU Only Bot",
                "pudu.delivery.mock.v1",
                "PUDU"
        ));

        mockMvc.perform(post("/api/v1/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "robotId": "robot-pudu-only-1",
                                  "vendorType": "PUDU",
                                  "vendorDeviceRef": "sandbox-device-opaque-001",
                                  "notes": "mock bind until formal docs"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MOCK_BOUND"));

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId)
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": {
                                    "pickupStationId": "pickup-1",
                                    "dropoffStationId": "table-pudu"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                        .andExpect(jsonPath("$.assignedRobotId").value("robot-pudu-only-1"))
        );

        String events = mockMvc.perform(get("/api/v1/events")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(events).contains("PUDU_MOCK");
        assertThat(events).contains("task.completed");

        mockMvc.perform(get("/api/v1/adapters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PUDU.supportLevel").value("Mock"))
                .andExpect(jsonPath("$.KEENON.supportLevel").value("Mock"));
    }

    @Test
    void keenon_mock_rejects_unsupported_without_inventing_api() throws Exception {
        String siteId = "site-keenon-only";
        store.saveRobot(new Robot(
                "robot-keenon-only-1",
                "tenant-demo",
                siteId,
                "KEENON Only Bot",
                "keenon.delivery.mock.v1",
                "KEENON"
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

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        );

        String events = mockMvc.perform(get("/api/v1/events")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", siteId))
                .andReturn().getResponse().getContentAsString();
        assertThat(events).contains("KEENON_MOCK");
    }

    @Test
    void binding_unbind_is_audited() throws Exception {
        MvcResult bound = mockMvc.perform(post("/api/v1/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo")
                        .content("""
                                {
                                  "robotId": "robot-pudu-mock-01",
                                  "vendorType": "PUDU",
                                  "vendorDeviceRef": "opaque-ref-unbind-test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = mapper.readTree(bound.getResponse().getContentAsString());
        String bindingId = node.get("id").asText();

        mockMvc.perform(delete("/api/v1/bindings/" + bindingId).header("X-Tenant-Id", "tenant-demo"))
                .andExpect(status().isNoContent());

        String audit = mockMvc.perform(get("/api/v1/audit").header("X-Tenant-Id", "tenant-demo"))
                .andReturn().getResponse().getContentAsString();
        assertThat(audit).contains("robot.binding.created");
        assertThat(audit).contains("robot.binding.deleted");
    }
}
