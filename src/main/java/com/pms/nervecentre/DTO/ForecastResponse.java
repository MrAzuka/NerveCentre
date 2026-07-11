package com.pms.nervecentre.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ForecastResponse {
    private String metricName;
    private List<ForecastPoint> forecast;
    private Boolean proactiveAlert;
    private String alertMessage;
}

