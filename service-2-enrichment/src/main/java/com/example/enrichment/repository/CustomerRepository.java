package com.example.enrichment.repository;

import com.example.enrichment.model.CustomerData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с данными клиентов в Redis.
 * 
 * Spring Data Redis автоматически реализует методы на основе
 * аннотаций модели (@RedisHash, @Id, @Indexed).
 * 
 * CrudRepository предоставляет базовые CRUD операции:
 * - save, findById, findAll, delete, count, existsById
 */
@Repository
public interface CustomerRepository extends CrudRepository<CustomerData, String> {
    
    /**
     * Поиск клиента по email.
     * Работает благодаря @Indexed аннотации на поле customerEmail.
     */
    Optional<CustomerData> findByCustomerEmail(String email);
}
