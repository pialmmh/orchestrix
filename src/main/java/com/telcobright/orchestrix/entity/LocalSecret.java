package com.telcobright.orchestrix.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing encrypted secrets locally in the database
 */
@Entity
@Table(name = "local_secrets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalSecret {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    private String namespace;
    
    @Column(length = 1000)
    private String tags;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String encryptedData;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "accessed_at")
    private LocalDateTime accessedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}