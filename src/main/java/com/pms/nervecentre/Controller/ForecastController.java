package com.pms.nervecentre.Controller;

import com.pms.nervecentre.DTO.ForecastResponse;
import com.pms.nervecentre.Service.ForecastingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastingService forecastingService;

    @GetMapping("/{metricName}")
    public ResponseEntity<ForecastResponse> forecast(
            @PathVariable String metricName,
            @RequestParam(required = false) Double threshold) {
        try {
            return ResponseEntity.ok(forecastingService.forecast(metricName, threshold));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}