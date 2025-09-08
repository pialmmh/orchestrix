package com.telcobright.orchestrix.service.secret;

import com.telcobright.orchestrix.dto.SecretCredentialDto;
import com.telcobright.orchestrix.dto.SecretItemDto;
import com.telcobright.orchestrix.entity.LocalSecret;
import com.telcobright.orchestrix.repository.LocalSecretRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Local encrypted storage implementation of SecretProvider
 * Stores secrets in the local database with AES-256-GCM encryption
 */
@Service
@Slf4j
public class LocalEncryptedSecretProvider implements SecretProvider {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    @Value("${secret.provider.local.encryption.key:}")
    private String encryptionKeyBase64;
    
    @Value("${secret.provider.local.enabled:true}")
    private boolean enabled;
    
    @Autowired(required = false)
    private LocalSecretRepository secretRepository;
    
    private SecretKey secretKey;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public SecretProviderType getType() {
        return SecretProviderType.LOCAL_ENCRYPTED;
    }
    
    @Override
    public boolean testConnection() {
        // Local provider is always connected if repository is available
        return secretRepository != null;
    }
    
    @Override
    public boolean authenticate() {
        // Local provider doesn't require authentication
        return true;
    }
    
    @Override
    public SecretItemDto saveCredential(SecretCredentialDto credential) {
        if (secretRepository == null) {
            log.error("LocalSecretRepository not available");
            return null;
        }
        
        try {
            LocalSecret secret = new LocalSecret();
            
            if (credential.getId() != null) {
                // Update existing
                Optional<LocalSecret> existing = secretRepository.findById(credential.getId());
                if (existing.isPresent()) {
                    secret = existing.get();
                }
            } else {
                // Generate new ID
                secret.setId(UUID.randomUUID().toString());
            }
            
            secret.setName(credential.getName());
            secret.setDescription(credential.getDescription());
            secret.setNamespace(credential.getNamespace());
            secret.setTags(String.join(",", credential.getTags() != null ? credential.getTags() : List.of()));
            
            // Encrypt sensitive data
            Map<String, String> sensitiveData = new HashMap<>();
            if (credential.getPassword() != null) sensitiveData.put("password", credential.getPassword());
            if (credential.getSshKey() != null) sensitiveData.put("sshKey", credential.getSshKey());
            if (credential.getSshKeyPassphrase() != null) sensitiveData.put("sshKeyPassphrase", credential.getSshKeyPassphrase());
            if (credential.getCertificate() != null) sensitiveData.put("certificate", credential.getCertificate());
            if (credential.getCertificateKey() != null) sensitiveData.put("certificateKey", credential.getCertificateKey());
            if (credential.getApiKey() != null) sensitiveData.put("apiKey", credential.getApiKey());
            if (credential.getApiSecret() != null) sensitiveData.put("apiSecret", credential.getApiSecret());
            if (credential.getClientSecret() != null) sensitiveData.put("clientSecret", credential.getClientSecret());
            
            String encryptedData = encrypt(objectMapper.writeValueAsString(sensitiveData));
            secret.setEncryptedData(encryptedData);
            
            // Store non-sensitive data
            Map<String, String> metadata = new HashMap<>();
            metadata.put("username", credential.getUsername());
            metadata.put("url", credential.getUrl());
            metadata.put("clientId", credential.getClientId());
            secret.setMetadata(objectMapper.writeValueAsString(metadata));
            
            secret.setNotes(credential.getNotes());
            secret.setUpdatedAt(LocalDateTime.now());
            
            LocalSecret saved = secretRepository.save(secret);
            
            return convertToSecretItem(saved);
        } catch (Exception e) {
            log.error("Failed to save credential locally: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public SecretCredentialDto getCredential(String itemId) {
        if (secretRepository == null) {
            return null;
        }
        
        try {
            Optional<LocalSecret> secret = secretRepository.findById(itemId);
            if (secret.isPresent()) {
                return convertToSecretCredential(secret.get());
            }
        } catch (Exception e) {
            log.error("Failed to retrieve credential: {}", e.getMessage());
        }
        return null;
    }
    
    @Override
    public boolean deleteCredential(String itemId) {
        if (secretRepository == null) {
            return false;
        }
        
        try {
            secretRepository.deleteById(itemId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete credential: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<SecretItemDto> listCredentials(String namespace) {
        if (secretRepository == null) {
            return List.of();
        }
        
        try {
            List<LocalSecret> secrets = namespace != null ?
                secretRepository.findByNamespace(namespace) :
                secretRepository.findAll();
            
            return secrets.stream()
                .map(this::convertToSecretItem)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list credentials: {}", e.getMessage());
            return List.of();
        }
    }
    
    @Override
    public List<SecretItemDto> searchCredentials(String searchTerm) {
        if (secretRepository == null) {
            return List.of();
        }
        
        try {
            List<LocalSecret> secrets = secretRepository.searchByTerm(searchTerm);
            return secrets.stream()
                .map(this::convertToSecretItem)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search credentials: {}", e.getMessage());
            return List.of();
        }
    }
    
    @Override
    public Map<String, String> getConfigurationRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("encryption.key", "Base64 encoded AES-256 encryption key");
        return requirements;
    }
    
    @Override
    public boolean validateConfiguration(Map<String, String> config) {
        // Local provider only needs encryption key
        return config.containsKey("encryption.key") || encryptionKeyBase64 != null;
    }
    
    @Override
    public boolean isConfigured() {
        return enabled && getOrCreateKey() != null;
    }
    
    @Override
    public SecretProviderStatus getStatus() {
        SecretProviderStatus status = new SecretProviderStatus();
        status.setConnected(secretRepository != null);
        status.setAuthenticated(true);
        
        if (secretRepository != null) {
            status.setItemCount(secretRepository.count());
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("enabled", enabled);
        metadata.put("encryptionConfigured", getOrCreateKey() != null);
        status.setMetadata(metadata);
        
        return status;
    }
    
    // Encryption/Decryption methods
    private String encrypt(String plaintext) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV and ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    private String decrypt(String encryptedData) throws Exception {
        SecretKey key = getOrCreateKey();
        byte[] combined = Base64.getDecoder().decode(encryptedData);
        
        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    private SecretKey getOrCreateKey() {
        if (secretKey == null) {
            if (encryptionKeyBase64 != null && !encryptionKeyBase64.isEmpty()) {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                // Generate a new key (should be stored securely in production)
                try {
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(256);
                    secretKey = keyGen.generateKey();
                    log.warn("Generated new encryption key. Store this securely: {}", 
                        Base64.getEncoder().encodeToString(secretKey.getEncoded()));
                } catch (Exception e) {
                    log.error("Failed to generate encryption key: {}", e.getMessage());
                }
            }
        }
        return secretKey;
    }
    
    // Conversion methods
    private SecretItemDto convertToSecretItem(LocalSecret secret) {
        SecretItemDto item = new SecretItemDto();
        item.setId(secret.getId());
        item.setName(secret.getName());
        item.setNamespace(secret.getNamespace());
        item.setType("CREDENTIAL");
        item.setProviderType("LOCAL_ENCRYPTED");
        item.setProviderId(secret.getId());
        item.setUpdatedAt(secret.getUpdatedAt() != null ? secret.getUpdatedAt().toString() : null);
        
        if (secret.getTags() != null && !secret.getTags().isEmpty()) {
            item.setTags(Arrays.asList(secret.getTags().split(",")));
        }
        
        return item;
    }
    
    private SecretCredentialDto convertToSecretCredential(LocalSecret secret) throws Exception {
        SecretCredentialDto credential = new SecretCredentialDto();
        credential.setId(secret.getId());
        credential.setName(secret.getName());
        credential.setDescription(secret.getDescription());
        credential.setNamespace(secret.getNamespace());
        credential.setNotes(secret.getNotes());
        
        if (secret.getTags() != null && !secret.getTags().isEmpty()) {
            credential.setTags(Arrays.asList(secret.getTags().split(",")));
        }
        
        // Decrypt sensitive data
        if (secret.getEncryptedData() != null) {
            String decryptedJson = decrypt(secret.getEncryptedData());
            Map<String, String> sensitiveData = objectMapper.readValue(decryptedJson, Map.class);
            
            credential.setPassword(sensitiveData.get("password"));
            credential.setSshKey(sensitiveData.get("sshKey"));
            credential.setSshKeyPassphrase(sensitiveData.get("sshKeyPassphrase"));
            credential.setCertificate(sensitiveData.get("certificate"));
            credential.setCertificateKey(sensitiveData.get("certificateKey"));
            credential.setApiKey(sensitiveData.get("apiKey"));
            credential.setApiSecret(sensitiveData.get("apiSecret"));
            credential.setClientSecret(sensitiveData.get("clientSecret"));
        }
        
        // Load non-sensitive metadata
        if (secret.getMetadata() != null) {
            Map<String, String> metadata = objectMapper.readValue(secret.getMetadata(), Map.class);
            credential.setUsername(metadata.get("username"));
            credential.setUrl(metadata.get("url"));
            credential.setClientId(metadata.get("clientId"));
        }
        
        return credential;
    }
}