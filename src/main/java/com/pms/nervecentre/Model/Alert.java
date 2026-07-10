package com.pms.nervecentre.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(nullable = false)
    private Double value;

    @Column(name = "z_score", nullable = false)
    private Double zScore;

    @Column(nullable = false)
    private Double mean;

    @Column(nullable = false)
    private Double stddev;

    @Column(nullable = false)
    private String severity; // "WARNING" or "CRITICAL"

    // This is for the LLM to add context to the alert
    private String explanation;
    private String likelyCause;
    private String recommendedAction;
}