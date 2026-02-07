package com.example.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentData {
    private String customerId;
    private String customerName;
    private String customerEmail;
    private CustomerLevel customerLevel;
    private List<ActiveRequest> activeRequests;
    private boolean dataAvailable;
    private String errorMessage;
    
    public enum CustomerLevel { NEW, REGULAR, VIP, PREMIUM }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRequest {
        private String requestId;
        private String category;
        private String subject;
        private LocalDateTime createdAt;
        private RequestStatus status;
    }
    
    public enum RequestStatus { OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED }
}
