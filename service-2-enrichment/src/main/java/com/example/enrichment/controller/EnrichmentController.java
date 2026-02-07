package com.example.enrichment.controller;

import com.example.enrichment.dto.CustomerRequest;
import com.example.enrichment.dto.EnrichmentResponse;
import com.example.enrichment.service.EnrichmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/enrichment")
@RequiredArgsConstructor
@Tag(name = "Enrichment API", description = "API для получения расширенной информации о клиентах")
public class EnrichmentController {
    
    private final EnrichmentService enrichmentService;
    
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Получить данные клиента")
    public ResponseEntity<EnrichmentResponse> getCustomerEnrichment(
            @Parameter(description = "ID клиента") @PathVariable String customerId) {
        log.info("Get customer: {}", customerId);
        EnrichmentResponse response = enrichmentService.getCustomerEnrichment(customerId);
        log.info("Customer {} found: {}", customerId, response.isDataAvailable());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/customer")
    @Operation(summary = "Создать данные клиента")
    public ResponseEntity<EnrichmentResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("Creating customer: {}", request.getCustomerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrichmentService.saveCustomer(request));
    }
    
    @PutMapping("/customer/{customerId}")
    @Operation(summary = "Обновить данные клиента")
    public ResponseEntity<EnrichmentResponse> updateCustomer(
            @PathVariable String customerId, @Valid @RequestBody CustomerRequest request) {
        log.info("Updating customer: {}", customerId);
        return ResponseEntity.ok(enrichmentService.updateCustomer(customerId, request));
    }
    
    @PostMapping("/customer/{customerId}/requests")
    @Operation(summary = "Добавить активное обращение")
    public ResponseEntity<EnrichmentResponse> addActiveRequest(
            @PathVariable String customerId, @Valid @RequestBody CustomerRequest.ActiveRequestDto request) {
        log.info("Adding request for customer: {}", customerId);
        EnrichmentResponse response = enrichmentService.addActiveRequest(customerId, request);
        return response.isDataAvailable() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/customer/{customerId}/requests/{requestId}")
    @Operation(summary = "Удалить активное обращение")
    public ResponseEntity<EnrichmentResponse> removeActiveRequest(
            @PathVariable String customerId, @PathVariable String requestId) {
        log.info("Removing request {} for customer: {}", requestId, customerId);
        EnrichmentResponse response = enrichmentService.removeActiveRequest(customerId, requestId);
        return response.isDataAvailable() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/customers")
    @Operation(summary = "Получить всех клиентов")
    public ResponseEntity<List<EnrichmentResponse>> getAllCustomers() {
        return ResponseEntity.ok(enrichmentService.getAllCustomers());
    }
    
    @DeleteMapping("/customer/{customerId}")
    @Operation(summary = "Удалить данные клиента")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable String customerId) {
        log.info("Deleting customer: {}", customerId);
        if (!enrichmentService.customerExists(customerId)) return ResponseEntity.notFound().build();
        enrichmentService.deleteCustomer(customerId);
        return ResponseEntity.ok(Map.of("status", "DELETED", "customerId", customerId));
    }
    
    @PostMapping("/setup-test-data")
    @Operation(summary = "Создать тестовые данные")
    public ResponseEntity<Map<String, Object>> setupTestData() {
        
        // Клиент 1: Обычный клиент без активных обращений
        CustomerRequest customer1 = CustomerRequest.builder()
                .customerId("CUST-001")
                .customerName("Иван Петров")
                .customerEmail("ivan.petrov@example.com")
                .customerPhone("+7-999-123-4567")
                .customerLevel("REGULAR")
                .activeRequests(List.of())
                .build();
        enrichmentService.saveCustomer(customer1);
        
        // Клиент 2: VIP клиент с активным обращением
        CustomerRequest customer2 = CustomerRequest.builder()
                .customerId("CUST-002")
                .customerName("Мария Сидорова")
                .customerEmail("maria.sidorova@example.com")
                .customerPhone("+7-999-234-5678")
                .customerLevel("VIP")
                .activeRequests(List.of(
                        CustomerRequest.ActiveRequestDto.builder()
                                .requestId("REQ-VIP-001")
                                .category("BILLING")
                                .subject("Вопрос по счёту")
                                .status("OPEN")
                                .build()
                ))
                .build();
        enrichmentService.saveCustomer(customer2);
        
        // Клиент 3: Premium клиент с несколькими активными обращениями
        CustomerRequest customer3 = CustomerRequest.builder()
                .customerId("CUST-003")
                .customerName("Алексей Козлов")
                .customerEmail("alexey.kozlov@example.com")
                .customerPhone("+7-999-345-6789")
                .customerLevel("PREMIUM")
                .activeRequests(List.of(
                        CustomerRequest.ActiveRequestDto.builder()
                                .requestId("REQ-PREM-001")
                                .category("TECHNICAL_SUPPORT")
                                .subject("Проблема с подключением")
                                .status("IN_PROGRESS")
                                .build(),
                        CustomerRequest.ActiveRequestDto.builder()
                                .requestId("REQ-PREM-002")
                                .category("GENERAL_INQUIRY")
                                .subject("Общий вопрос")
                                .status("OPEN")
                                .build()
                ))
                .build();
        enrichmentService.saveCustomer(customer3);
        
        // Клиент 4: Новый клиент
        CustomerRequest customer4 = CustomerRequest.builder()
                .customerId("CUST-004")
                .customerName("Елена Новикова")
                .customerEmail("elena.novikova@example.com")
                .customerLevel("NEW")
                .activeRequests(List.of())
                .build();
        enrichmentService.saveCustomer(customer4);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "customersCreated", 4,
                "timestamp", LocalDateTime.now()
        ));
    }
    
    @GetMapping("/health")
    @Operation(summary = "Проверка состояния")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "enrichment-service", "timestamp", LocalDateTime.now()));
    }
}
