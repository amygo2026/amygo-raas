package ai.amygo.raas;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Week4ShowAgentPrototypeTest {

    @DynamicPropertySource
    static void disconnectStart(DynamicPropertyRegistry registry) {
        registry.add("raas.unitree-show.simulate-disconnect-on-start", () -> "true");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void show_start_unknown_then_reconcile_is_at_most_once() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Site-Id", "site-demo")
                        .content("""
                                {
                                  "robotId": "robot-unitree-show-mock-01",
                                  "assetId": "cue-pack-a",
                                  "assetVersion": "1.0.0",
                                  "assetHash": "sha256:abc"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String showId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/shows/" + showId + "/preflight").header("X-Tenant-Id", "tenant-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREFLIGHT_OK"));
        mockMvc.perform(post("/api/v1/shows/" + showId + "/arm").header("X-Tenant-Id", "tenant-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARMED"));

        MvcResult start1 = mockMvc.perform(post("/api/v1/shows/" + showId + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .content("{\"idempotencyKey\":\"show-start-1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode s1 = mapper.readTree(start1.getResponse().getContentAsString());
        assertThat(s1.get("status").asText()).isEqualTo("UNKNOWN");
        assertThat(s1.get("startCount").asInt()).isEqualTo(0);

        // Blind second start with same key must not double-execute
        MvcResult start2 = mockMvc.perform(post("/api/v1/shows/" + showId + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .content("{\"idempotencyKey\":\"show-start-1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode s2 = mapper.readTree(start2.getResponse().getContentAsString());
        assertThat(s2.get("startCount").asInt()).isEqualTo(0);

        MvcResult reconciled = mockMvc.perform(post("/api/v1/shows/" + showId + "/start/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .content("{\"idempotencyKey\":\"show-start-1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode s3 = mapper.readTree(reconciled.getResponse().getContentAsString());
        assertThat(s3.get("startCount").asInt()).isEqualTo(1);
        assertThat(s3.get("showStatus").asText()).isEqualTo("RUNNING");

        // Reconcile again — still once
        MvcResult again = mockMvc.perform(post("/api/v1/shows/" + showId + "/start/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .content("{\"idempotencyKey\":\"show-start-1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mapper.readTree(again.getResponse().getContentAsString()).get("startCount").asInt()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/shows/agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportLevel").value("Mock"));
    }
}
