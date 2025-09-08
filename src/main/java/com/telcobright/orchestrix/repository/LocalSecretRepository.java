package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.LocalSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LocalSecret entities
 */
@Repository
public interface LocalSecretRepository extends JpaRepository<LocalSecret, String> {
    
    /**
     * Find secrets by namespace
     */
    List<LocalSecret> findByNamespace(String namespace);
    
    /**
     * Find secrets by name
     */
    List<LocalSecret> findByNameContainingIgnoreCase(String name);
    
    /**
     * Search secrets by term in name, description, tags, or notes
     */
    @Query("SELECT s FROM LocalSecret s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(s.tags) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(s.notes) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<LocalSecret> searchByTerm(@Param("term") String term);
    
    /**
     * Find secrets by namespace and name
     */
    Optional<LocalSecret> findByNamespaceAndName(String namespace, String name);
    
    /**
     * Delete secrets by namespace
     */
    void deleteByNamespace(String namespace);
    
    /**
     * Count secrets by namespace
     */
    long countByNamespace(String namespace);
}