package com.example.moderation.repository;

import com.example.moderation.model.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
    boolean existsByEventId(String eventId);
    Optional<ProcessedEvent> findByEventId(String eventId);
    List<ProcessedEvent> findByCustomerIdAndCategoryAndResult(String customerId, String category, ProcessedEvent.ProcessingResult result);
    
    @Query("{'processedAt': {$gte: ?0, $lte: ?1}}")
    List<ProcessedEvent> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByResult(ProcessedEvent.ProcessingResult result);
}
