package ai.amygo.raas;

import ai.amygo.raas.application.OutboxPublisher;
import ai.amygo.raas.persistence.OutboxRepository;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week2HardeningIntegrationTest {

    @DynamicPropertySource
    static void fastSimulator(DynamicPropertyRegistry registry) {
        registry.add("raas.simulator.progress-delay-ms", () -> "0");
        registry.add("raas.outbox.poll-interval-ms", () -> "100");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired OutboxPublisher outboxPublisher;
    @Autowired OutboxRepository outboxRepository;

    @Test
    void cancel_fail_restart_recovery() throws Exception {
        // No robots on this site → task remains QUEUED and can be canceled deterministically.
        MvcResult queued = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-no-robots")
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": { "pickupStationId": "p1", "dropoffStationId": "d1" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn();
        String cancelTarget = mapper.readTree(queued.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/tasks/" + cancelTarget + "/cancel")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Actor-Id", "qa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        String doneId = createTask("table-restart-src");
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/tasks/" + doneId).header("X-Tenant-Id", "tenant-demo"))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        );

        MvcResult restarted = mockMvc.perform(post("/api/v1/tasks/" + doneId + "/restart")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Actor-Id", "qa"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode restartNode = mapper.readTree(restarted.getResponse().getContentAsString());
        assertThat(restartNode.get("attemptNo").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(restartNode.get("payload").get("restartedFromTaskId").asText()).isEqualTo(doneId);
        assertThat(restartNode.get("id").asText()).isNotEqualTo(doneId);

        // Operator fail: create on empty site so it stays non-terminal, then fail.
        MvcResult toFail = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-no-robots")
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": { "pickupStationId": "p2", "dropoffStationId": "d2" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String failTarget = mapper.readTree(toFail.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/tasks/" + failTarget + "/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .content("{\"reason\":\"manual_abort\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(get("/api/v1/tasks/" + doneId + "/timeline")
                        .header("X-Tenant-Id", "tenant-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/audit").header("X-Tenant-Id", "tenant-demo"))
                .andExpect(status().isOk());
    }

    @Test
    void outbox_publisher_at_least_once_no_republish_after_mark() {
        outboxRepository.append("tenant-demo", "Task", "t-outbox-1", "task.created", Map.of("x", 1));
        outboxRepository.append("tenant-demo", "Task", "t-outbox-2", "task.created", Map.of("x", 2));
        assertThat(outboxRepository.unpublishedCount()).isGreaterThanOrEqualTo(2);

        int guard = 0;
        while (outboxRepository.unpublishedCount() > 0 && guard++ < 200) {
            outboxPublisher.publishBatch(200);
        }
        assertThat(outboxRepository.unpublishedCount()).isEqualTo(0);
        int sinkAfterDrain = outboxPublisher.sinkSize();

        int second = outboxPublisher.publishBatch(100);
        assertThat(second).isEqualTo(0);
        assertThat(outboxPublisher.sinkSize()).isEqualTo(sinkAfterDrain);
    }

    @Test
    void stress_one_thousand_simulated_tasks() throws Exception {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(createTask("stress-" + i));
        }

        await().atMost(Duration.ofSeconds(120)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            String body = mockMvc.perform(get("/api/v1/tasks")
                            .header("X-Tenant-Id", "tenant-demo")
                            .header("X-Site-Id", "site-demo"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode arr = mapper.readTree(body);
            int succeeded = 0;
            for (JsonNode n : arr) {
                if (ids.contains(n.get("id").asText()) && "SUCCEEDED".equals(n.get("status").asText())) {
                    succeeded++;
                }
            }
            assertThat(succeeded).isEqualTo(1000);
        });
    }

    private String createTask(String dropoff) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo")
                        .content("""
                                {
                                  "taskType": "DELIVERY",
                                  "payload": {
                                    "pickupStationId": "pickup-1",
                                    "dropoffStationId": "%s"
                                  }
                                }
                                """.formatted(dropoff)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }
}
