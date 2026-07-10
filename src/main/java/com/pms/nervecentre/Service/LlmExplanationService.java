package com.pms.nervecentre.Service;

import com.pms.nervecentre.Model.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmExplanationService {
    @Value("${deepseek.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    // This is the main entry point. Given an alert and the recent metric values that
    // led to it, we ask an LLM to explain what's going on in plain English, then
    // stuff that explanation back onto the alert object.
    //  If anything goes wrong i just log it and move on
    public void enrich(Alert alert, List<Double> recentValues) {
        try {
            String prompt = buildPrompt(alert, recentValues);
            String response = callLlm(prompt);
            ExplanationResult result = parseResponse(response);

            alert.setExplanation(result.summary());
            alert.setLikelyCause(result.likelyCause());
            alert.setRecommendedAction(result.recommendedAction());

        } catch (Exception e) {
            log.error("LLM enrichment failed for alert {}: {}", alert.getId(), e.getMessage());
            // Alert is still saved — explanation just won't be present
        }
    }

    // Builds the actual text prompt we send to the LLM. Takes all the numbers off
    // the alert and the recent history of readings, then plugs them into a template that tells the
    // model exactly what to look at and exactly what shape of JSON to hand back.
    private String buildPrompt(Alert alert, List<Double> recentValues) {
        return String.format("""
            You are a backend systems expert analyzing a metric anomaly.
            
            Metric: %s
            Current value: %s (anomalous)
            Baseline mean: %s
            Baseline stddev: %s
            Z-score: %s
            Severity: %s
            Recent values (last 20): %s
            
            Respond ONLY with valid JSON in this exact format:
            {
              "summary": "one sentence plain-English explanation",
              "likely_cause": "most probable root cause",
              "recommended_action": "what the on-call engineer should check first"
            }
            """,
                alert.getMetricName(),
                alert.getValue(),
                String.format("%.2f", alert.getMean()),
                String.format("%.2f", alert.getStddev()),
                String.format("%.2f", alert.getZScore()),
                alert.getSeverity(),
                recentValues.toString()
        );
    }


    // Actually makes the call to the DeepSeek API. P
    private String callLlm(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("max_tokens", 512);
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        Map<String, Object> response = restClient.post()
                .uri("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString();
    }

    private ExplanationResult parseResponse(String json) {
        try {
            // Strip markdown if present
            String clean = json.replaceAll("```json|```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(clean);
            return new ExplanationResult(
                    node.get("summary").asText(),
                    node.get("likely_cause").asText(),
                    node.get("recommended_action").asText()
            );
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", json);
            return new ExplanationResult("Explanation unavailable", "Unknown", "Check logs");
        }
    }

    // A simple little container to hold the three pieces of info we pull out of
    // the LLM's response, so we can pass them around as one object instead of three.
    public record ExplanationResult(String summary, String likelyCause, String recommendedAction) {}
}
