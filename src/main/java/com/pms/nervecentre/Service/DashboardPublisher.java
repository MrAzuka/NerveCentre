package com.pms.nervecentre.Service;


import com.pms.nervecentre.Model.Alert;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMetric(String metricName, Double value, Instant time) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("metricName", metricName);
        payload.put("value", value);
        payload.put("time", time.toString());

        messagingTemplate.convertAndSend("/topic/metrics", Optional.of(payload));
    }

    public void publishAlert(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", alert.getId());
        payload.put("metricName", alert.getMetricName());
        payload.put("value", alert.getValue());
        payload.put("severity", alert.getSeverity());
        payload.put("zScore", alert.getZScore());
        payload.put("explanation", alert.getExplanation());
        payload.put("time", alert.getTime().toString());

        messagingTemplate.convertAndSend("/topic/alerts", Optional.of(payload));
    }
}
