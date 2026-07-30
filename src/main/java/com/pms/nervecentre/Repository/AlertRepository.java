package com.pms.nervecentre.Repository;


import com.pms.nervecentre.Model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findTop10ByOrderByTimeDesc();

    // All alerts, newest first
    Page<Alert> findAllByOrderByTimeDesc(Pageable pageable);

    // Filter by severity
    Page<Alert> findBySeverityOrderByTimeDesc(String severity, Pageable pageable);

    // Filter by metric name
    Page<Alert> findByMetricNameOrderByTimeDesc(String metricName, Pageable pageable);

    // Filter by both
    Page<Alert> findBySeverityAndMetricNameOrderByTimeDesc(
            String severity, String metricName, Pageable pageable
    );
}