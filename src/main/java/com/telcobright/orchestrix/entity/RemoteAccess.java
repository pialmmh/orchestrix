package com.telcobright.orchestrix.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import com.telcobright.orchestrix.service.secret.SecretProvider.SecretProviderType;

/**
 * Unified remote access configuration for all device types.
 * This entity stores references to credentials in external secret providers, not the actual credentials.
 */
@Entity
@Table(name = "remote_access", 
    indexes = {
        @Index(name = "idx_remote_access_device", columnList = "device_type, device_id"),
        @Index(name = "idx_remote_access_secret", columnList = "secret_provider_type, secret_item_id"),
        @Index(name = "idx_remote_access_active", columnList = "is_active, is_primary")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    // Device reference (polymorphic)
    @Column(name = "device_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType; // COMPUTE, NETWORK_DEVICE, STORAGE, FIREWALL, LOAD_BALANCER, etc.
    
    @Column(name = "device_id", nullable = false)
    private Integer deviceId;
    
    @Column(name = "device_name", length = 255)
    private String deviceName; // Cached for quick reference
    
    // Access configuration
    @Column(name = "access_name", length = 100)
    private String accessName; // e.g., "Primary SSH", "Management Console", "API Access"
    
    @Column(name = "access_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AccessType accessType; // SSH, RDP, TELNET, HTTPS, REST_API, SNMP, etc.
    
    @Column(name = "access_protocol", length = 50)
    @Enumerated(EnumType.STRING)
    private AccessProtocol accessProtocol; // SSH, RDP, WINRM, TELNET, HTTPS, HTTP, SNMP_V2C, SNMP_V3
    
    // Connection details
    @Column(name = "host", length = 255)
    private String host;
    
    @Column(name = "port")
    private Integer port;
    
    @Column(name = "connection_url", length = 500)
    private String connectionUrl; // Full connection string if applicable
    
    // Secret Provider Integration
    @Column(name = "secret_provider_type", length = 50)
    @Enumerated(EnumType.STRING)
    private SecretProviderType secretProviderType = SecretProviderType.LOCAL_ENCRYPTED;
    
    @Column(name = "secret_item_id", length = 100)
    private String secretItemId; // Generic secret item identifier
    
    @Column(name = "secret_namespace", length = 100)
    private String secretNamespace; // Organization/Collection/Vault/Path depending on provider
    
    // Bitwarden-specific fields (kept for backward compatibility)
    @Column(name = "bitwarden_item_id", length = 100)
    private String bitwardenItemId; // Bitwarden item UUID
    
    @Column(name = "bitwarden_collection_id", length = 100)
    private String bitwardenCollectionId; // Bitwarden collection UUID
    
    @Column(name = "bitwarden_organization_id", length = 100)
    private String bitwardenOrganizationId; // Bitwarden org UUID
    
    @Column(name = "bitwarden_folder_id", length = 100)
    private String bitwardenFolderId; // Bitwarden folder UUID
    
    @Column(name = "bitwarden_sync_enabled")
    private Boolean bitwardenSyncEnabled = true;
    
    @Column(name = "bitwarden_last_sync")
    private LocalDateTime bitwardenLastSync;
    
    // Credential type stored in Bitwarden
    @Column(name = "auth_method", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuthMethod authMethod; // PASSWORD, SSH_KEY, CERTIFICATE, API_KEY, TOKEN, MULTI_FACTOR
    
    // Cached non-sensitive metadata
    @Column(name = "username", length = 100)
    private String username; // Cached from Bitwarden for display
    
    @Column(name = "ssh_key_fingerprint", length = 255)
    private String sshKeyFingerprint; // SSH key fingerprint for verification
    
    @Column(name = "certificate_cn", length = 255)
    private String certificateCn; // Certificate Common Name
    
    @Column(name = "certificate_expiry")
    private LocalDateTime certificateExpiry;
    
    // Additional configuration
    @Column(name = "sudo_enabled")
    private Boolean sudoEnabled = false;
    
    @Column(name = "sudo_method", length = 50)
    private String sudoMethod; // sudo, su, enable, runas
    
    @Column(name = "enable_mode_required")
    private Boolean enableModeRequired = false; // For network devices
    
    @Column(name = "jump_host_id")
    private Integer jumpHostId; // Reference to another RemoteAccess for jump/bastion host
    
    @Column(name = "proxy_url", length = 255)
    private String proxyUrl; // SOCKS/HTTP proxy if needed
    
    // Connection parameters (non-sensitive)
    @Column(name = "connection_params", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> connectionParams = new HashMap<>();
    
    // SSH specific options
    @Column(name = "ssh_options", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, String> sshOptions = new HashMap<>(); // StrictHostKeyChecking, UserKnownHostsFile, etc.
    
    // Validation and monitoring
    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;
    
    @Column(name = "last_test_status", length = 50)
    @Enumerated(EnumType.STRING)
    private TestStatus lastTestStatus; // SUCCESS, FAILED, UNTESTED, IN_PROGRESS
    
    @Column(name = "last_test_message", columnDefinition = "TEXT")
    private String lastTestMessage;
    
    @Column(name = "last_successful_connection")
    private LocalDateTime lastSuccessfulConnection;
    
    @Column(name = "connection_timeout_seconds")
    private Integer connectionTimeoutSeconds = 30;
    
    @Column(name = "retry_attempts")
    private Integer retryAttempts = 3;
    
    // Security and compliance
    @Column(name = "requires_mfa")
    private Boolean requiresMfa = false;
    
    @Column(name = "mfa_type", length = 50)
    private String mfaType; // TOTP, SMS, HARDWARE_TOKEN, PUSH
    
    @Column(name = "session_recording_enabled")
    private Boolean sessionRecordingEnabled = false;
    
    @Column(name = "privileged_access")
    private Boolean privilegedAccess = false;
    
    @Column(name = "compliance_tags", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private String[] complianceTags; // PCI, HIPAA, SOC2, etc.
    
    // Status and lifecycle
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "is_emergency_access")
    private Boolean isEmergencyAccess = false;
    
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    
    @Column(name = "rotation_required")
    private Boolean rotationRequired = false;
    
    @Column(name = "last_rotation_date")
    private LocalDateTime lastRotationDate;
    
    @Column(name = "next_rotation_date")
    private LocalDateTime nextRotationDate;
    
    // Audit fields
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
    
    // Enums
    public enum DeviceType {
        COMPUTE,
        NETWORK_DEVICE,
        STORAGE,
        FIREWALL,
        LOAD_BALANCER,
        DATABASE,
        CONTAINER,
        HYPERVISOR,
        APPLIANCE,
        IOT_DEVICE,
        OTHER
    }
    
    public enum AccessType {
        SSH,
        RDP,
        TELNET,
        SERIAL,
        HTTPS,
        HTTP,
        REST_API,
        GRAPHQL_API,
        SNMP,
        WMI,
        WINRM,
        IPMI,
        REDFISH,
        VNC,
        SPICE,
        ANSIBLE,
        KUBERNETES_API,
        DOCKER_API,
        VMWARE_API,
        CUSTOM
    }
    
    public enum AccessProtocol {
        SSH,
        SSH2,
        RDP,
        WINRM_HTTP,
        WINRM_HTTPS,
        TELNET,
        SERIAL,
        HTTPS,
        HTTP,
        SNMP_V1,
        SNMP_V2C,
        SNMP_V3,
        WS,
        WSS,
        GRPC,
        CUSTOM
    }
    
    public enum AuthMethod {
        PASSWORD,
        SSH_KEY,
        SSH_KEY_WITH_PASSPHRASE,
        CERTIFICATE,
        CLIENT_CERTIFICATE,
        API_KEY,
        API_KEY_SECRET,
        BEARER_TOKEN,
        OAUTH2,
        KERBEROS,
        NTLM,
        BASIC_AUTH,
        DIGEST_AUTH,
        MULTI_FACTOR,
        BIOMETRIC,
        HARDWARE_TOKEN,
        NONE
    }
    
    public enum TestStatus {
        SUCCESS,
        FAILED,
        UNTESTED,
        IN_PROGRESS,
        SCHEDULED,
        TIMEOUT,
        AUTH_FAILED,
        CONNECTION_REFUSED,
        HOST_UNREACHABLE
    }
    
    // Helper methods
    @Transient
    public boolean isExpired() {
        return validUntil != null && validUntil.isBefore(LocalDateTime.now());
    }
    
    @Transient
    public boolean isNotYetValid() {
        return validFrom != null && validFrom.isAfter(LocalDateTime.now());
    }
    
    @Transient
    public boolean isValid() {
        return !isExpired() && !isNotYetValid() && isActive;
    }
    
    @Transient
    public boolean needsRotation() {
        return rotationRequired && nextRotationDate != null && 
               nextRotationDate.isBefore(LocalDateTime.now());
    }
}