package com.pms.nervecentre.Service;

import com.pms.nervecentre.Model.Metric;
import com.pms.nervecentre.Repository.MetricRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Add more comments explaining my code. DO FOR THE WHOLE CODE

// MetricStreamConsumer.java
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricStreamConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final MetricRepository metricRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    private static final String STREAM_KEY = "metrics:stream";
    private static final String GROUP_NAME  = "nervecentre-group";
    private static final String CONSUMER    = "consumer-1";

    @PostConstruct
    public void init() {
        // Create consumer group if it doesn't exist
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
            log.info("Consumer group created: {}", GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group already exists, continuing...");
        }
    }

    @Scheduled(fixedDelay = 1000) // polls every second
    public void consume() {
        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER),
                        StreamReadOptions.empty().count(10),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) return;



        for (MapRecord<String, Object, Object> record : records) {
            try {
                Map<Object, Object> fields = record.getValue();

                Metric metric = new Metric();
                metric.setTime(Instant.ofEpochMilli(Long.parseLong(fields.get("timestamp").toString())));
                metric.setName(fields.get("name").toString());
                metric.setValue(Double.parseDouble(fields.get("value").toString()));
                metric.setTags(extractTags(fields));

                metricRepository.save(metric);
                // After saving the metric it will analyze it for any anomaly
                anomalyDetectionService.analyze(metric.getName(), metric.getValue(), metric.getTime());

                // Acknowledge so the message isn't reprocessed
                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());

                log.info("Saved metric: {} = {}", metric.getName(), metric.getValue());

            } catch (Exception e) {
                log.error("Failed to process record {}: {}", record.getId(), e.getMessage());
            }
        }
    }

    private String extractTags(Map<Object, Object> fields) {
        StringBuilder json = new StringBuilder("{");
        fields.forEach((k, v) -> {
            if (k.toString().startsWith("tag:")) {
                json.append("\"").append(k.toString().substring(4)).append("\":")
                        .append("\"").append(v.toString()).append("\",");
            }
        });
        if (json.length() > 1) json.deleteCharAt(json.length() - 1);
        json.append("}");
        String result = json.toString();
        return result.equals("{}") ? null : result;
    }
}