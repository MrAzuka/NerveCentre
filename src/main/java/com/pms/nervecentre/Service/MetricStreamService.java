package com.pms.nervecentre.Service;

import com.pms.nervecentre.DTO.MetricPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricStreamService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String STREAM_KEY = "metrics:stream";

    public void publish(MetricPayload metricPayload) {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", metricPayload.getName());
        fields.put("value", metricPayload.getValue());
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        if (metricPayload.getTags() != null) {
            fields.putAll(metricPayload.getTags());
        }

        redisTemplate.opsForStream().add(STREAM_KEY, fields);
    }
}
