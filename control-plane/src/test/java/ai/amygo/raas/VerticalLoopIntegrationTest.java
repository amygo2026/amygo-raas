package ai.amygo.raas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class VerticalLoopIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createTask_scheduler_simulator_completes() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo")
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": {
                                    "pickupStationId": "pickup-1",
                                    "dropoffStationId": "table-12"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String taskId = body.replaceAll("(?s).*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + taskId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        );

        String events = mockMvc.perform(get("/api/v1/events")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(events).contains("task.completed");
    }
}
