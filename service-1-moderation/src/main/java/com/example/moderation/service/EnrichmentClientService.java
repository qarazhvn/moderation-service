package com.example.moderation.service;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentClientService {
    
    private final WebClient webClient;
    
    @Retryable(
            retryFor = {WebClientException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public EnrichmentData getEnrichmentData(CustomerRequestEvent event) {
        log.debug("Calling Service-2 for customer: {}", event.getCustomerId());
        
        try {
            EnrichmentData result = webClient.get()
                    .uri("/api/v1/enrichment/customer/{customerId}", event.getCustomerId())
                    .retrieve()
                    .bodyToMono(EnrichmentData.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return result != null ? result : 
                    createEmptyEnrichmentData(event.getCustomerId(), "No data from Service-2");
        } catch (Exception e) {
            log.error("Failed to get enrichment data: {}", e.getMessage());
            throw new RuntimeException("Enrichment service call failed", e);
        }
    }
    
    public Mono<EnrichmentData> getEnrichmentDataAsync(String customerId) {
        return webClient.get()
                .uri("/api/v1/enrichment/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(EnrichmentData.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(error -> Mono.just(createEmptyEnrichmentData(customerId, error.getMessage())));
    }
    
    @Recover
    public EnrichmentData recoverFromFailure(Exception e, CustomerRequestEvent event) {
        log.error("Retry exhausted for customer: {}", event.getCustomerId());
        return createEmptyEnrichmentData(event.getCustomerId(), "Service-2 unavailable: " + e.getMessage());
    }
    
    private EnrichmentData createEmptyEnrichmentData(String customerId, String errorMessage) {
        return EnrichmentData.builder()
                .customerId(customerId)
                .dataAvailable(false)
                .errorMessage(errorMessage)
                .build();
    }
}
