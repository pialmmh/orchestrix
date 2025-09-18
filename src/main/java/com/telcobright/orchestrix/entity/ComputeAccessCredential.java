package com.telcobright.orchestrix.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "compute_access_credential")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComputeAccessCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compute_resource_id", nullable = false)
    @JsonIgnore
    private ComputeResource computeResource;
    
    @Column(name = "access_type", nullable = false, length = 50)
    private String accessType; // SSH, RDP, WINRM, REST_API, ANSIBLE, K8S_API, DOCKER_API, FTP, SFTP
    
    @Column(name = "access_name", length = 100)
    private String accessName;
    
    // Connection details
    private String host;
    
    private Integer port;
    
    @Column(length = 20)
    private String protocol;
    
    // Authentication
    @Column(name = "auth_type", length = 50)
    private String authType; // PASSWORD, SSH_KEY, CERTIFICATE, TOKEN, API_KEY, OAUTH2
    
    private String username;
    
    @Column(name = "password_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String passwordEncrypted;
    
    // SSH specific
    @Column(name = "ssh_private_key", columnDefinition = "TEXT")
    @JsonIgnore
    private String sshPrivateKey;
    
    @Column(name = "ssh_public_key", columnDefinition = "TEXT")
    private String sshPublicKey;
    
    @Column(name = "ssh_key_passphrase_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String sshKeyPassphraseEncrypted;
    
    // Certificate based
    @Column(name = "client_certificate", columnDefinition = "TEXT")
    @JsonIgnore
    private String clientCertificate;
    
    @Column(name = "client_certificate_key", columnDefinition = "TEXT")
    @JsonIgnore
    private String clientCertificateKey;
    
    @Column(name = "ca_certificate", columnDefinition = "TEXT")
    private String caCertificate;
    
    // API/Token based
    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String apiKeyEncrypted;
    
    @Column(name = "api_secret_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String apiSecretEncrypted;
    
    @Column(name = "bearer_token_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String bearerTokenEncrypted;
    
    // Additional auth parameters
    @Column(name = "auth_params", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> authParams = new HashMap<>();
    
    // Connection options
    @Column(name = "connection_params", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> connectionParams = new HashMap<>();
    
    // Ansible specific
    @Column(name = "ansible_become_method", length = 50)
    private String ansibleBecomeMethod;
    
    @Column(name = "ansible_become_user", length = 50)
    private String ansibleBecomeUser;
    
    @Column(name = "ansible_python_interpreter")
    private String ansiblePythonInterpreter;
    
    // Kubernetes specific
    @Column(name = "k8s_namespace", length = 100)
    private String k8sNamespace;
    
    @Column(name = "k8s_context", length = 100)
    private String k8sContext;
    
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String kubeconfig;
    
    // Docker specific
    @Column(name = "docker_tls_verify")
    private Boolean dockerTlsVerify = true;
    
    @Column(name = "docker_registry_url")
    private String dockerRegistryUrl;
    
    // Validation and testing
    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;
    
    @Column(name = "last_test_status", length = 50)
    private String lastTestStatus; // SUCCESS, FAILED, UNTESTED
    
    @Column(name = "last_test_message", columnDefinition = "TEXT")
    private String lastTestMessage;
    
    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}