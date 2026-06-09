package com.pms.nervecentre.Repository;

import com.pms.nervecentre.Model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByNameAndTimeBetweenOrderByTimeAsc(
            String name,
            Instant from,
            Instant to
    );
}
