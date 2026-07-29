package com.pms.nervecentre.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ConditionalOnProperty(name = "simulator.enabled", havingValue = "true")
@Service
@Slf4j
public class MetricSimulatorService {
    private final RestClient restClient = RestClient.create();
    private final Random random = new Random();

    private static final String METRICS_URL = "http://localhost:8080/metrics";

    // Simulated metrics with their normal ranges
    private static final Map<String, double[]> METRIC_RANGES = Map.of(
            "api_response_time_ms",  new double[]{80, 150},
            "cpu_usage_percent",     new double[]{20, 60},
            "memory_usage_mb",       new double[]{400, 700},
            "db_query_duration_ms",  new double[]{10, 50},
            "active_connections",    new double[]{5, 30}
    );

    @Scheduled(fixedDelay = 5000) // every 5 seconds
    public void simulate() {
        METRIC_RANGES.forEach((metricName, range) -> {
            double value = generateValue(metricName, range);
            sendMetric(metricName, value);
        });
    }

    private double generateValue(String metricName, double[] range) {
        double min = range[0];
        double max = range[1];

        // 10% chance of injecting a spike for any metric
        boolean isSpike = random.nextDouble() < 0.10;

        if (isSpike) {
            double spikeMultiplier = 4 + random.nextDouble() * 6; // 4x to 10x normal
            double spikeValue = max * spikeMultiplier;
            log.warn("SIMULATED SPIKE injected for {}: {}", metricName,
                    String.format("%.2f", spikeValue));
            return spikeValue;
        }

        // Normal traffic — random value within the normal range
        return min + (random.nextDouble() * (max - min));
    }

    private void sendMetric(String metricName, double value) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", metricName);
            payload.put("value", Math.round(value * 100.0) / 100.0);
            payload.put("tags", Map.of("source", "simulator"));

            restClient.post()
                    .uri(METRICS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Simulated {} = {}", metricName,
                    String.format("%.2f", value));

        } catch (Exception e) {
            log.error("Simulator failed to send {}: {}", metricName, e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        log.info("MetricSimulator started — sending {} metrics every 5 seconds with 10% spike probability",
                METRIC_RANGES.size());
        log.info("Simulating: {}", METRIC_RANGES.keySet());
    }
}
