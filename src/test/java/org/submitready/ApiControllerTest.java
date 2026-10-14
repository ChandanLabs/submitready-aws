package org.submitready;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fixedDemoPassesAllEightChecks() throws Exception {
        JsonNode packet = readDemo("fixed");

        mockMvc.perform(post("/api/validate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(packet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.passedCount").value(8))
                .andExpect(jsonPath("$.totalChecks").value(8));
    }

    @Test
    void brokenDemoReportsNameAndIncomeMismatches() throws Exception {
        JsonNode packet = readDemo("broken");

        mockMvc.perform(post("/api/validate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(packet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.results[?(@.id == 'name-match')].passed").value(false))
                .andExpect(jsonPath("$.results[?(@.id == 'income-match')].passed").value(false));
    }

    @Test
    void rejectsAttachmentsLargerThanFiveMegabytes() throws Exception {
        JsonNode packet = readDemo("fixed");
        ((ObjectNode) packet.path("documents").path("identityProof")).put("sizeBytes", 5_000_001);

        mockMvc.perform(post("/api/validate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(packet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.id == 'file-constraints')].passed").value(false));
    }

    @Test
    void servesPageAndDemoAndHealthRoutes() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("Applicant pre-check")))
            .andExpect(content().string(containsString("identity-file")))
            .andExpect(content().string(containsString("Check readiness")));
        mockMvc.perform(get("/api/demo/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.name").exists());
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void rejectsMalformedJsonAndUnknownDemo() throws Exception {
        mockMvc.perform(post("/api/validate")
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/api/demo/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    private JsonNode readDemo(String kind) throws IOException {
        try (var input = new ClassPathResource("demo/application-" + kind + ".json").getInputStream()) {
            return objectMapper.readTree(input);
        }
    }
}