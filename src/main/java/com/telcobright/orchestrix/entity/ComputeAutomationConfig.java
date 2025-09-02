package com.telcobright.orchestrix.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "compute_automation_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComputeAutomationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compute_resource_id", nullable = false)
    @JsonIgnore
    private ComputeResource computeResource;
    
    @Column(name = "tool_type", nullable = false, length = 50)
    private String toolType; // ANSIBLE, TERRAFORM, PUPPET, CHEF, SALTSTACK, JENKINS
    
    // Ansible specific
    @Column(name = "ansible_inventory_group", length = 100)
    private String ansibleInventoryGroup;
    
    @Column(name = "ansible_host_vars", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> ansibleHostVars = new HashMap<>();
    
    // Terraform specific
    @Column(name = "terraform_provider", length = 50)
    private String terraformProvider;
    
    @Column(name = "terraform_resource_id")
    private String terraformResourceId;
    
    // Puppet specific
    @Column(name = "puppet_node_name")
    private String puppetNodeName;
    
    @Column(name = "puppet_environment", length = 50)
    private String puppetEnvironment;
    
    @Column(name = "puppet_classes", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private List<String> puppetClasses = new ArrayList<>();
    
    // Chef specific
    @Column(name = "chef_node_name")
    private String chefNodeName;
    
    @Column(name = "chef_environment", length = 50)
    private String chefEnvironment;
    
    @Column(name = "chef_run_list", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private List<String> chefRunList = new ArrayList<>();
    
    // Common automation settings
    @Column(name = "config_params", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> configParams = new HashMap<>();
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}