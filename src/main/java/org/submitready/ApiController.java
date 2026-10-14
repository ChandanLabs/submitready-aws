package org.submitready;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ValidationService validationService;
    private final ObjectMapper objectMapper;

    public ApiController(ValidationService validationService, ObjectMapper objectMapper) {
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody JsonNode packet) {
        return validationService.validate(packet);
    }

    @GetMapping("/demo/{kind}")
    public JsonNode demo(@PathVariable String kind) throws IOException {
        if (!kind.equals("broken") && !kind.equals("fixed")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo packet not found");
        }
        String filename = "demo/application-" + kind + ".json";
        return objectMapper.readTree(new ClassPathResource(filename).getInputStream());
    }
}