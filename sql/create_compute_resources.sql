-- Create compute resources table for tracking servers and their automation access
CREATE TABLE IF NOT EXISTS compute_resources (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45), -- Support IPv4 and IPv6
    datacenter_id INT,
    partner_id INT,
    
    -- Resource specifications
    cpu_cores INT,
    memory_gb INT,
    storage_gb INT,
    os_type VARCHAR(50), -- Linux, Windows, AIX, Solaris, etc.
    os_version VARCHAR(100),
    
    -- Status and management
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DISCONTINUED, MAINTENANCE
    environment VARCHAR(50), -- PRODUCTION, STAGING, DEVELOPMENT, TEST
    purpose VARCHAR(255), -- Application server, Database server, Build server, etc.
    
    -- Metadata
    tags JSON, -- Flexible tagging system
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (datacenter_id) REFERENCES datacenters(id) ON DELETE SET NULL,
    FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE SET NULL,
    INDEX idx_compute_status (status),
    INDEX idx_compute_environment (environment)
);

-- Create table for storing various access credentials
CREATE TABLE IF NOT EXISTS compute_access_credentials (
    id INT AUTO_INCREMENT PRIMARY KEY,
    compute_resource_id INT NOT NULL,
    access_type VARCHAR(50) NOT NULL, -- SSH, RDP, WINRM, REST_API, ANSIBLE, K8S_API, DOCKER_API, FTP, SFTP
    access_name VARCHAR(100), -- Friendly name for this access method
    
    -- Connection details
    host VARCHAR(255), -- Can be different from main hostname (jump host, load balancer, etc.)
    port INT,
    protocol VARCHAR(20), -- SSH, HTTPS, HTTP, FTP, SFTP, etc.
    
    -- Authentication
    auth_type VARCHAR(50), -- PASSWORD, SSH_KEY, CERTIFICATE, TOKEN, API_KEY, OAUTH2
    username VARCHAR(100),
    password_encrypted TEXT, -- Encrypted password storage
    
    -- SSH specific
    ssh_private_key TEXT, -- Encrypted SSH private key
    ssh_public_key TEXT,
    ssh_key_passphrase_encrypted TEXT,
    
    -- Certificate based
    client_certificate TEXT,
    client_certificate_key TEXT,
    ca_certificate TEXT,
    
    -- API/Token based
    api_key_encrypted TEXT,
    api_secret_encrypted TEXT,
    bearer_token_encrypted TEXT,
    
    -- Additional auth parameters (JSON for flexibility)
    auth_params JSON, -- For OAuth2 params, custom headers, etc.
    
    -- Connection options
    connection_params JSON, -- Timeout, retry, proxy settings, etc.
    
    -- Ansible specific
    ansible_become_method VARCHAR(50), -- sudo, su, doas, pbrun, pfexec
    ansible_become_user VARCHAR(50),
    ansible_python_interpreter VARCHAR(255),
    
    -- Kubernetes specific
    k8s_namespace VARCHAR(100),
    k8s_context VARCHAR(100),
    kubeconfig TEXT,
    
    -- Docker specific
    docker_tls_verify BOOLEAN DEFAULT TRUE,
    docker_registry_url VARCHAR(255),
    
    -- Validation and testing
    last_tested_at TIMESTAMP NULL,
    last_test_status VARCHAR(50), -- SUCCESS, FAILED, UNTESTED
    last_test_message TEXT,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_primary BOOLEAN DEFAULT FALSE, -- Mark primary access method
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (compute_resource_id) REFERENCES compute_resources(id) ON DELETE CASCADE,
    INDEX idx_access_type (access_type),
    INDEX idx_access_active (is_active),
    UNIQUE KEY unique_primary_access (compute_resource_id, access_type, is_primary)
);

-- Create table for automation tool configurations
CREATE TABLE IF NOT EXISTS compute_automation_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    compute_resource_id INT NOT NULL,
    tool_type VARCHAR(50) NOT NULL, -- ANSIBLE, TERRAFORM, PUPPET, CHEF, SALTSTACK, JENKINS
    
    -- Ansible specific
    ansible_inventory_group VARCHAR(100),
    ansible_host_vars JSON,
    
    -- Terraform specific
    terraform_provider VARCHAR(50),
    terraform_resource_id VARCHAR(255),
    
    -- Puppet specific
    puppet_node_name VARCHAR(255),
    puppet_environment VARCHAR(50),
    puppet_classes JSON,
    
    -- Chef specific
    chef_node_name VARCHAR(255),
    chef_environment VARCHAR(50),
    chef_run_list JSON,
    
    -- Common automation settings
    config_params JSON, -- Flexible storage for tool-specific configs
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (compute_resource_id) REFERENCES compute_resources(id) ON DELETE CASCADE,
    INDEX idx_automation_tool (tool_type)
);

-- Sample data for compute resources
INSERT INTO compute_resources (name, hostname, ip_address, status, environment, purpose, cpu_cores, memory_gb, storage_gb, os_type, os_version, tags) VALUES
('Production Web Server 1', 'web01.prod.example.com', '10.0.1.10', 'ACTIVE', 'PRODUCTION', 'Web Application Server', 8, 16, 500, 'Linux', 'Ubuntu 22.04 LTS', '["web", "nginx", "production"]'),
('Production DB Server 1', 'db01.prod.example.com', '10.0.1.20', 'ACTIVE', 'PRODUCTION', 'PostgreSQL Database Server', 16, 64, 2000, 'Linux', 'RHEL 8.6', '["database", "postgresql", "production"]'),
('Build Server', 'build01.dev.example.com', '10.0.2.10', 'ACTIVE', 'DEVELOPMENT', 'CI/CD Build Server', 12, 32, 1000, 'Linux', 'Ubuntu 20.04 LTS', '["jenkins", "build", "ci-cd"]'),
('Kubernetes Master', 'k8s-master01.example.com', '10.0.3.10', 'ACTIVE', 'PRODUCTION', 'Kubernetes Control Plane', 4, 8, 100, 'Linux', 'Ubuntu 22.04 LTS', '["kubernetes", "master", "container"]'),
('Windows App Server', 'winapp01.example.com', '10.0.1.30', 'ACTIVE', 'PRODUCTION', 'Windows Application Server', 8, 32, 500, 'Windows', 'Windows Server 2022', '["windows", "dotnet", "iis"]'),
('Ansible Controller', 'ansible.ops.example.com', '10.0.4.10', 'ACTIVE', 'PRODUCTION', 'Ansible Automation Controller', 4, 16, 200, 'Linux', 'RHEL 9', '["ansible", "automation", "orchestration"]');

-- Sample access credentials (passwords and keys would be encrypted in production)
INSERT INTO compute_access_credentials (compute_resource_id, access_type, access_name, host, port, protocol, auth_type, username, is_primary, is_active) VALUES
(1, 'SSH', 'Primary SSH Access', 'web01.prod.example.com', 22, 'SSH', 'SSH_KEY', 'ubuntu', TRUE, TRUE),
(1, 'ANSIBLE', 'Ansible Access', 'web01.prod.example.com', 22, 'SSH', 'SSH_KEY', 'ansible', FALSE, TRUE),
(2, 'SSH', 'Database Server SSH', 'db01.prod.example.com', 22, 'SSH', 'SSH_KEY', 'rhel', TRUE, TRUE),
(3, 'SSH', 'Build Server SSH', 'build01.dev.example.com', 22, 'SSH', 'PASSWORD', 'jenkins', TRUE, TRUE),
(3, 'REST_API', 'Jenkins API', 'build01.dev.example.com', 8080, 'HTTPS', 'API_KEY', 'jenkins', FALSE, TRUE),
(4, 'K8S_API', 'Kubernetes API', 'k8s-master01.example.com', 6443, 'HTTPS', 'CERTIFICATE', 'admin', TRUE, TRUE),
(5, 'RDP', 'Windows RDP', 'winapp01.example.com', 3389, 'RDP', 'PASSWORD', 'Administrator', TRUE, TRUE),
(5, 'WINRM', 'Windows Remote Management', 'winapp01.example.com', 5986, 'HTTPS', 'PASSWORD', 'Administrator', FALSE, TRUE),
(6, 'SSH', 'Ansible Controller SSH', 'ansible.ops.example.com', 22, 'SSH', 'SSH_KEY', 'ansible', TRUE, TRUE);

-- Sample automation configurations
INSERT INTO compute_automation_config (compute_resource_id, tool_type, ansible_inventory_group, config_params) VALUES
(1, 'ANSIBLE', 'webservers', '{"ansible_python_interpreter": "/usr/bin/python3", "http_port": 80}'),
(2, 'ANSIBLE', 'databases', '{"ansible_python_interpreter": "/usr/bin/python3", "postgresql_version": "14"}'),
(3, 'JENKINS', NULL, '{"jenkins_url": "https://jenkins.example.com", "node_label": "linux-build"}'),
(4, 'TERRAFORM', NULL, '{"provider": "kubernetes", "cluster_name": "prod-cluster"}');