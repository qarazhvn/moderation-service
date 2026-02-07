package com.example.moderation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestEvent {
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Request ID is required")
    private String requestId;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String description;
    
    @NotNull(message = "Priority is required")
    private Priority priority;
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
}
