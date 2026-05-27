package com.pms.nervecentre.Controller;

import com.pms.nervecentre.DTO.MetricPayload;
import com.pms.nervecentre.Service.MetricStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Metric;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricController
{
    private final MetricStreamService metricStreamService;

    @PostMapping
    public ResponseEntity<Void> ingest(@Valid @RequestBody MetricPayload metric){
        metricStreamService.publish(metric);
        return ResponseEntity.accepted().build();
    }
}
