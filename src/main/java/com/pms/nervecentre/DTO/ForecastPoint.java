package com.pms.nervecentre.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ForecastPoint {
    private Instant time;
    private Double predictedValue;
    private Double upperBound;
    private Double lowerBound;
}
