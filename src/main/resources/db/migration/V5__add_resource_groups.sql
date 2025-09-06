-- Create resource_groups table
CREATE TABLE resource_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    icon VARCHAR(50),
    color VARCHAR(7),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create resource_group_services table for service types
CREATE TABLE resource_group_services (
    resource_group_id BIGINT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    FOREIGN KEY (resource_group_id) REFERENCES resource_groups(id) ON DELETE CASCADE
);

-- Create datacenter_resource_groups junction table
CREATE TABLE datacenter_resource_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    datacenter_id INT NOT NULL,
    resource_group_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    configuration TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (datacenter_id) REFERENCES datacenters(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_group_id) REFERENCES resource_groups(id) ON DELETE CASCADE,
    UNIQUE KEY unique_datacenter_resource_group (datacenter_id, resource_group_id)
);

-- Insert default resource groups
INSERT INTO resource_groups (name, display_name, description, category, sort_order, icon, color) VALUES
('cloud_computing', 'Cloud Computing', 'Infrastructure as a Service - Compute, Storage, and Networking', 'IaaS', 1, 'cloud', '#2196F3'),
('platform_services', 'Platform Services', 'Platform as a Service - Development and deployment platforms', 'PaaS', 2, 'developer_mode', '#9C27B0'),
('saas_applications', 'SaaS Applications', 'Software as a Service - Business applications and databases', 'SaaS', 3, 'apps', '#4CAF50'),
('data_analytics', 'Data & Analytics', 'Big Data, Analytics, and Business Intelligence services', 'SaaS', 4, 'analytics', '#FF9800'),
('security_compliance', 'Security & Compliance', 'Security, Identity, and Compliance services', 'SaaS', 5, 'security', '#F44336'),
('networking_cdn', 'Networking & CDN', 'Content Delivery Network and networking services', 'IaaS', 6, 'network_check', '#00BCD4'),
('ai_ml', 'AI & Machine Learning', 'Artificial Intelligence and Machine Learning services', 'PaaS', 7, 'psychology', '#673AB7'),
('containers_kubernetes', 'Containers & Kubernetes', 'Container orchestration and management', 'PaaS', 8, 'widgets', '#3F51B5');

-- Insert service types for each resource group
-- Cloud Computing (IaaS)
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Compute' FROM resource_groups WHERE name = 'cloud_computing'
UNION ALL
SELECT id, 'Storage' FROM resource_groups WHERE name = 'cloud_computing'
UNION ALL
SELECT id, 'Block Storage' FROM resource_groups WHERE name = 'cloud_computing'
UNION ALL
SELECT id, 'Object Storage' FROM resource_groups WHERE name = 'cloud_computing'
UNION ALL
SELECT id, 'Virtual Network' FROM resource_groups WHERE name = 'cloud_computing';

-- Platform Services (PaaS)
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'App Service' FROM resource_groups WHERE name = 'platform_services'
UNION ALL
SELECT id, 'Functions' FROM resource_groups WHERE name = 'platform_services'
UNION ALL
SELECT id, 'API Gateway' FROM resource_groups WHERE name = 'platform_services'
UNION ALL
SELECT id, 'Service Bus' FROM resource_groups WHERE name = 'platform_services';

-- SaaS Applications
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'MySQL Database' FROM resource_groups WHERE name = 'saas_applications'
UNION ALL
SELECT id, 'PostgreSQL Database' FROM resource_groups WHERE name = 'saas_applications'
UNION ALL
SELECT id, 'MongoDB' FROM resource_groups WHERE name = 'saas_applications'
UNION ALL
SELECT id, 'Redis Cache' FROM resource_groups WHERE name = 'saas_applications'
UNION ALL
SELECT id, 'ERP System' FROM resource_groups WHERE name = 'saas_applications'
UNION ALL
SELECT id, 'CRM System' FROM resource_groups WHERE name = 'saas_applications';

-- Data & Analytics
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Data Warehouse' FROM resource_groups WHERE name = 'data_analytics'
UNION ALL
SELECT id, 'Data Lake' FROM resource_groups WHERE name = 'data_analytics'
UNION ALL
SELECT id, 'Stream Analytics' FROM resource_groups WHERE name = 'data_analytics'
UNION ALL
SELECT id, 'Business Intelligence' FROM resource_groups WHERE name = 'data_analytics';

-- Security & Compliance
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Identity Management' FROM resource_groups WHERE name = 'security_compliance'
UNION ALL
SELECT id, 'Key Vault' FROM resource_groups WHERE name = 'security_compliance'
UNION ALL
SELECT id, 'Security Center' FROM resource_groups WHERE name = 'security_compliance'
UNION ALL
SELECT id, 'Compliance Manager' FROM resource_groups WHERE name = 'security_compliance';

-- Networking & CDN
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Load Balancer' FROM resource_groups WHERE name = 'networking_cdn'
UNION ALL
SELECT id, 'CDN' FROM resource_groups WHERE name = 'networking_cdn'
UNION ALL
SELECT id, 'VPN Gateway' FROM resource_groups WHERE name = 'networking_cdn'
UNION ALL
SELECT id, 'Firewall' FROM resource_groups WHERE name = 'networking_cdn';

-- AI & ML
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Machine Learning' FROM resource_groups WHERE name = 'ai_ml'
UNION ALL
SELECT id, 'Cognitive Services' FROM resource_groups WHERE name = 'ai_ml'
UNION ALL
SELECT id, 'Bot Service' FROM resource_groups WHERE name = 'ai_ml'
UNION ALL
SELECT id, 'Computer Vision' FROM resource_groups WHERE name = 'ai_ml';

-- Containers & Kubernetes
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Kubernetes Service' FROM resource_groups WHERE name = 'containers_kubernetes'
UNION ALL
SELECT id, 'Container Registry' FROM resource_groups WHERE name = 'containers_kubernetes'
UNION ALL
SELECT id, 'Container Instances' FROM resource_groups WHERE name = 'containers_kubernetes'
UNION ALL
SELECT id, 'Service Mesh' FROM resource_groups WHERE name = 'containers_kubernetes';

-- Add indexes for better performance
CREATE INDEX idx_resource_group_category ON resource_groups(category);
CREATE INDEX idx_resource_group_active ON resource_groups(is_active);
CREATE INDEX idx_datacenter_resource_group_status ON datacenter_resource_groups(status);