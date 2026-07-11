package com.pms.nervecentre.Service;

import com.pms.nervecentre.Model.Alert;
import com.pms.nervecentre.Repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEnrichmentService {
    private final LlmExplanationService llmExplanationService;
    private final AlertRepository alertRepository;

    @Async
    public void enrichAsync(Alert alert, List<Double> recentValues) {
        log.info("Starting LLM enrichment for alert {}", alert.getId());
        llmExplanationService.enrich(alert, recentValues);
        alertRepository.save(alert);
        log.info("LLM enrichment complete for alert {} — {}", alert.getId(), alert.getExplanation());
    }
}
