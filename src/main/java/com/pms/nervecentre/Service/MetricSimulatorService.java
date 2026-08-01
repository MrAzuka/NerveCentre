package com.pms.nervecentre.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

// Rather than have a hard coded simulator, i am making it toggable
// The simulator can be switched on and off
@Service
@Slf4j
public class MetricSimulatorService {
    private final RestClient restClient = RestClient.create();
    private final Random random = new Random();

    private static final String METRICS_URL = "http://localhost:8080/metrics";

    private final AtomicBoolean running = new AtomicBoolean(true);

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
        if(!running.get()) return;

        METRIC_RANGES.forEach((metricName, range) -> {
            double value = generateValue(metricName, range);
            sendMetric(metricName, value);
        });
    }

    public boolean toggle() {
        boolean newState = !running.get();
        running.set(newState);
        log.info("MetricSimulator toggled — running={}", newState);
        return newState;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean state) {
        running.set(state);
        log.info("MetricSimulator set to running={}", state);
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
        } catch (Exception e) {
            log.error("Simulator failed to send {}: {}", metricName, e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        log.info("MetricSimulator started — running={}", running.get());
    }
}
