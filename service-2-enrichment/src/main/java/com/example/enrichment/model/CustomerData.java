package com.example.enrichment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "customer", timeToLive = 3600)
public class CustomerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    private String customerId;
    private String customerName;
    @Indexed
    private String customerEmail;
    private String customerPhone;
    private CustomerLevel customerLevel;
    private LocalDateTime registeredAt;
    private List<ActiveRequest> activeRequests;
    
    public enum CustomerLevel { NEW, REGULAR, VIP, PREMIUM }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private String requestId;
        private String category;
        private String subject;
        private LocalDateTime createdAt;
        private RequestStatus status;
    }
    
    public enum RequestStatus { OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED }
}
