package com.pms.nervecentre.Service;

import com.pms.nervecentre.Model.Alert;
import com.pms.nervecentre.Model.Metric;
import com.pms.nervecentre.Repository.AlertRepository;
import com.pms.nervecentre.Repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

//TODO: Change the console logs to write to a file rather than console. Decide that later

// AnomalyDetectionService.java
/**
 * Detects statistical anomalies in incoming metric values using a
 * rolling Z-score calculation against a recent window of historical data.
 * For every new metric value received, this service compares it against
 * the mean and standard deviation of the last WINDOW_SIZE data points.
 * If the value deviates enough (in standard deviations) from the recent
 * trend, an Alert is generated and persisted with a WARNING or CRITICAL
 * severity.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final AlertRepository alertRepository;
    private final MetricRepository metricRepository;
    private final AlertEnrichmentService alertEnrichmentService;

    // Number of most recent data points to use as the baseline window for mean/stddev
    private static final int WINDOW_SIZE = 20;   // look at last 20 data points

    // Z-score thresholds that determine alert severity
    private static final double WARNING_THRESHOLD  = 2.0; // ~95% confidence interval
    private static final double CRITICAL_THRESHOLD = 3.0; // ~99.7% confidence interval


    public void analyze(String metricName, Double currentValue, Instant time) {

        // Fetch the last N values for this metric from TimescaleDB
        // (does NOT include currentValue itself, since this hasn't been persisted yet)
        List<Metric> recent = metricRepository.findTopNByNameOrderByTimeDesc(metricName, WINDOW_SIZE);

        // Need at least 5 data points to compute meaningful stats.
        // Without enough history, mean/stddev would be unreliable, so skip analysis.
        if (recent.size() < 5) {
            log.debug("Not enough data points for {} ({} so far)", metricName, recent.size());
            return;
        }

        List<Double> values = recent.stream()
                .map(Metric::getValue)
                .collect(Collectors.toList());

        double mean   = calculateMean(values);
        double stddev = calculateStdDev(values, mean);

        // Avoid division by zero if all values are identical (stddev == 0 means no variance,
        // so a Z-score calculation would be undefined/infinite)
        if (stddev == 0) return;

        // Z-score = how many standard deviations the current value is from the mean.
        // Math.abs() is used because we care about deviation in either direction (spike or drop).
        double zScore = Math.abs((currentValue - mean) / stddev);


        log.info("Z-score for {}: {} (value={}, mean={}, stddev={})",
                metricName,
                String.format("%.2f", zScore),
                currentValue,
                String.format("%.2f", mean),
                String.format("%.2f", stddev));

        if (zScore >= WARNING_THRESHOLD) {
            String severity = zScore >= CRITICAL_THRESHOLD ? "CRITICAL" : "WARNING";

            Alert alert = new Alert();
            alert.setTime(time);
            alert.setMetricName(metricName);
            alert.setValue(currentValue);
            alert.setZScore(zScore);
            alert.setMean(mean);
            alert.setStddev(stddev);
            alert.setSeverity(severity);

            // save first to get an ID
            alertRepository.save(alert);

            // Fire and forget doesn't block the consumer
            alertEnrichmentService.enrichAsync(alert, values);

            log.warn("ALERT [{}] {} spiked to {} (Z-score: {})",
                    severity, metricName, currentValue, String.format("%.2f", zScore));
        }
    }

    private double calculateMean(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}