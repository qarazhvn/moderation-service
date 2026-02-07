package com.example.enrichment.service;

import com.example.enrichment.dto.CustomerRequest;
import com.example.enrichment.dto.EnrichmentResponse;
import com.example.enrichment.model.CustomerData;
import com.example.enrichment.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// Customer data service with Redis caching
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {
    
    private final CustomerRepository customerRepository;
    
    public EnrichmentResponse getCustomerEnrichment(String customerId) {
        log.debug("Fetching customer: {}", customerId);
        return customerRepository.findById(customerId)
                .map(this::mapToResponse)
                .orElse(EnrichmentResponse.notFound(customerId));
    }
    
    public EnrichmentResponse saveCustomer(CustomerRequest request) {
        CustomerData customer = mapToEntity(request);
        customer.setRegisteredAt(LocalDateTime.now());
        return mapToResponse(customerRepository.save(customer));
    }
    
    public EnrichmentResponse updateCustomer(String customerId, CustomerRequest request) {
        CustomerData customer = customerRepository.findById(customerId)
                .map(existing -> { updateEntityFromRequest(existing, request); return existing; })
                .orElseGet(() -> {
                    CustomerData c = mapToEntity(request);
                    c.setCustomerId(customerId);
                    c.setRegisteredAt(LocalDateTime.now());
                    return c;
                });
        return mapToResponse(customerRepository.save(customer));
    }
    
    public EnrichmentResponse addActiveRequest(String customerId, CustomerRequest.ActiveRequestDto requestDto) {
        return customerRepository.findById(customerId)
                .map(customer -> {
                    if (customer.getActiveRequests() == null) customer.setActiveRequests(new ArrayList<>());
                    customer.getActiveRequests().add(CustomerData.ActiveRequest.builder()
                            .requestId(requestDto.getRequestId())
                            .category(requestDto.getCategory())
                            .subject(requestDto.getSubject())
                            .status(parseStatus(requestDto.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build());
                    return mapToResponse(customerRepository.save(customer));
                })
                .orElse(EnrichmentResponse.notFound(customerId));
    }
    
    public EnrichmentResponse removeActiveRequest(String customerId, String requestId) {
        return customerRepository.findById(customerId)
                .map(customer -> {
                    if (customer.getActiveRequests() != null) {
                        customer.getActiveRequests().removeIf(r -> r.getRequestId().equals(requestId));
                        customerRepository.save(customer);
                    }
                    return mapToResponse(customer);
                })
                .orElse(EnrichmentResponse.notFound(customerId));
    }
    
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }
    
    public List<EnrichmentResponse> getAllCustomers() {
        Iterable<CustomerData> customers = customerRepository.findAll();
        
        return StreamSupport.stream(customers.spliterator(), false)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public boolean customerExists(String customerId) {
        return customerRepository.existsById(customerId);
    }
    
    private EnrichmentResponse mapToResponse(CustomerData customer) {
        List<EnrichmentResponse.ActiveRequest> activeRequests = null;
        
        if (customer.getActiveRequests() != null) {
            activeRequests = customer.getActiveRequests().stream()
                    .map(r -> EnrichmentResponse.ActiveRequest.builder()
                            .requestId(r.getRequestId())
                            .category(r.getCategory())
                            .subject(r.getSubject())
                            .createdAt(r.getCreatedAt())
                            .status(mapStatus(r.getStatus()))
                            .build())
                    .collect(Collectors.toList());
        }
        
        return EnrichmentResponse.builder()
                .customerId(customer.getCustomerId())
                .customerName(customer.getCustomerName())
                .customerEmail(customer.getCustomerEmail())
                .customerLevel(mapLevel(customer.getCustomerLevel()))
                .activeRequests(activeRequests)
                .dataAvailable(true)
                .build();
    }
    
    private CustomerData mapToEntity(CustomerRequest request) {
        List<CustomerData.ActiveRequest> activeRequests = null;
        
        if (request.getActiveRequests() != null) {
            activeRequests = request.getActiveRequests().stream()
                    .map(r -> CustomerData.ActiveRequest.builder()
                            .requestId(r.getRequestId())
                            .category(r.getCategory())
                            .subject(r.getSubject())
                            .status(parseStatus(r.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return CustomerData.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .customerLevel(parseLevel(request.getCustomerLevel()))
                .activeRequests(activeRequests)
                .build();
    }
    
    private void updateEntityFromRequest(CustomerData customer, CustomerRequest request) {
        if (request.getCustomerName() != null) customer.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) customer.setCustomerEmail(request.getCustomerEmail());
        if (request.getCustomerPhone() != null) customer.setCustomerPhone(request.getCustomerPhone());
        if (request.getCustomerLevel() != null) customer.setCustomerLevel(parseLevel(request.getCustomerLevel()));
    }
    
    private CustomerData.CustomerLevel parseLevel(String level) {
        if (level == null) return CustomerData.CustomerLevel.REGULAR;
        try { return CustomerData.CustomerLevel.valueOf(level.toUpperCase()); } 
        catch (IllegalArgumentException e) { return CustomerData.CustomerLevel.REGULAR; }
    }
    
    private CustomerData.RequestStatus parseStatus(String status) {
        if (status == null) return CustomerData.RequestStatus.OPEN;
        try { return CustomerData.RequestStatus.valueOf(status.toUpperCase()); } 
        catch (IllegalArgumentException e) { return CustomerData.RequestStatus.OPEN; }
    }
    
    private EnrichmentResponse.CustomerLevel mapLevel(CustomerData.CustomerLevel level) {
        return level == null ? EnrichmentResponse.CustomerLevel.REGULAR : EnrichmentResponse.CustomerLevel.valueOf(level.name());
    }
    
    private EnrichmentResponse.RequestStatus mapStatus(CustomerData.RequestStatus status) {
        return status == null ? EnrichmentResponse.RequestStatus.OPEN : EnrichmentResponse.RequestStatus.valueOf(status.name());
    }
}
