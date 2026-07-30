package com.pms.nervecentre.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private Instant time;
    private String metricName;
    private Double value;
    private Double zScore;
    private Double mean;
    private Double stddev;
    private String severity;
    private String explanation;
    private String likelyCause;
    private String recommendedAction;
}
