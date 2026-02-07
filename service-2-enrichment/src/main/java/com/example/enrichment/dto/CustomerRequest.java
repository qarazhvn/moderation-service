package com.example.enrichment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @Email(message = "Valid email is required")
    private String customerEmail;
    
    private String customerPhone;
    
    private String customerLevel;
    
    private List<ActiveRequestDto> activeRequests;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRequestDto {
        private String requestId;
        private String category;
        private String subject;
        private String status;
    }
}
