package com.pms.nervecentre.Service;

import com.pms.nervecentre.DTO.ForecastPoint;
import com.pms.nervecentre.DTO.ForecastResponse;
import com.pms.nervecentre.Model.Metric;
import com.pms.nervecentre.Repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastingService {

    private final MetricRepository metricRepository;

    private static final int HISTORY_POINTS = 30;
    private static final int FORECAST_MINUTES = 10;
    private static final double CONFIDENCE_INTERVAL = 1.96; // 95%

    // predicts its next 10 minutes and tells you
    // whether it's on track to blow past a threshold you care about.
    public ForecastResponse forecast(String metricName, Double alertThreshold) {

        // Fetch last 30 data points
        List<Metric> recent = metricRepository
                .findTopNByNameOrderByTimeDesc(metricName, HISTORY_POINTS);

        if (recent.size() < 5) {
            throw new IllegalStateException(
                    "Not enough data points to forecast. Need at least 5, have " + recent.size()
            );
        }

        // Reverse so oldest is first (regression needs old-new order)
        Collections.reverse(recent);

        // Build regression
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < recent.size(); i++) {
            regression.addData(i, recent.get(i).getValue());
        }

        double stddev = calculateStdDev(recent);
        double margin = CONFIDENCE_INTERVAL * stddev;

        Instant lastTime = recent.get(recent.size() - 1).getTime();
        int baseIndex = recent.size();

        List<ForecastPoint> points = new ArrayList<>();
        boolean proactiveAlert = false;
        String alertMessage = null;

        // Step forward minute by minute, asking the regression line "where will
        // this metric be at this future point?" and recording the prediction plus
        // its upper/lower confidence bounds.
        for (int i = 1; i <= FORECAST_MINUTES; i++) {
            double predicted = regression.predict(baseIndex + i);
            Instant forecastTime = lastTime.plus(i, ChronoUnit.MINUTES);

            points.add(new ForecastPoint(
                    forecastTime,
                    Math.round(predicted * 100.0) / 100.0,
                    Math.round((predicted + margin) * 100.0) / 100.0,
                    Math.round((predicted - margin) * 100.0) / 100.0
            ));

            // Proactive alert if forecast crosses threshold
            if (alertThreshold != null && predicted >= alertThreshold && !proactiveAlert) {
                proactiveAlert = true;
                alertMessage = String.format(
                        "%s is forecast to reach %.2f in %d minute(s), crossing your threshold of %.2f",
                        metricName, predicted, i, alertThreshold
                );
                log.warn("PROACTIVE ALERT: {}", alertMessage);
            }
        }

        return new ForecastResponse(metricName, points, proactiveAlert, alertMessage);
    }

    private double calculateStdDev(List<Metric> metrics) {
        double mean = metrics.stream()
                .mapToDouble(Metric::getValue)
                .average()
                .orElse(0.0);
        double variance = metrics.stream()
                .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}
