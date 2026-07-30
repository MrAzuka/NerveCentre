package com.pms.nervecentre.Service;

import com.pms.nervecentre.DTO.AlertResponse;
import com.pms.nervecentre.Model.Alert;
import com.pms.nervecentre.Repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public Page<AlertResponse> getAlerts(
            String severity,
            String metricName,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Alert> alerts;

        if (severity != null && metricName != null) {
            alerts = alertRepository.findBySeverityAndMetricNameOrderByTimeDesc(
                    severity.toUpperCase(), metricName, pageable
            );
        } else if (severity != null) {
            alerts = alertRepository.findBySeverityOrderByTimeDesc(
                    severity.toUpperCase(), pageable
            );
        } else if (metricName != null) {
            alerts = alertRepository.findByMetricNameOrderByTimeDesc(
                    metricName, pageable
            );
        } else {
            alerts = alertRepository.findAllByOrderByTimeDesc(pageable);
        }

        return alerts.map(this::toResponse);
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTime(),
                alert.getMetricName(),
                alert.getValue(),
                alert.getZScore(),
                alert.getMean(),
                alert.getStddev(),
                alert.getSeverity(),
                alert.getExplanation(),
                alert.getLikelyCause(),
                alert.getRecommendedAction()
        );
    }
}
