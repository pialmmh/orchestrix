-- Add Regions table
CREATE TABLE IF NOT EXISTS regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    geographic_area VARCHAR(100), -- e.g., 'North America', 'Europe', 'Asia Pacific'
    cloud_id BIGINT NOT NULL,
    compliance_zones VARCHAR(255), -- e.g., 'GDPR,HIPAA'
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cloud_id) REFERENCES clouds(id) ON DELETE CASCADE,
    INDEX idx_regions_cloud (cloud_id),
    INDEX idx_regions_status (status)
);

-- Add Availability Zones table
CREATE TABLE IF NOT EXISTS availability_zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    region_id BIGINT NOT NULL,
    zone_type VARCHAR(50) DEFAULT 'STANDARD', -- STANDARD, EDGE, LOCAL
    is_default BOOLEAN DEFAULT FALSE,
    capabilities VARCHAR(255), -- e.g., 'GPU,HIGH_MEMORY,HIGH_COMPUTE'
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_zone_per_region (region_id, code),
    INDEX idx_az_region (region_id),
    INDEX idx_az_status (status)
);

-- Add Environment table
CREATE TABLE IF NOT EXISTS environments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL, -- PRODUCTION, STAGING, DEVELOPMENT, QA, DR
    partner_id INT,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES partners(id),
    UNIQUE KEY unique_env_per_partner (partner_id, code),
    INDEX idx_env_partner (partner_id),
    INDEX idx_env_type (type)
);

-- Add Resource Pools/Clusters table
CREATE TABLE IF NOT EXISTS resource_pools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- COMPUTE, STORAGE, KUBERNETES, VMWARE, OPENSTACK
    datacenter_id INT,
    total_cpu_cores INT DEFAULT 0,
    total_memory_gb INT DEFAULT 0,
    total_storage_tb INT DEFAULT 0,
    used_cpu_cores INT DEFAULT 0,
    used_memory_gb INT DEFAULT 0,
    used_storage_tb INT DEFAULT 0,
    hypervisor VARCHAR(50), -- VMWARE, KVM, HYPERV, XEN
    orchestrator VARCHAR(50), -- KUBERNETES, OPENSHIFT, DOCKER_SWARM
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (datacenter_id) REFERENCES datacenters(id) ON DELETE CASCADE,
    INDEX idx_pool_datacenter (datacenter_id),
    INDEX idx_pool_type (type)
);

-- Update datacenters table with new columns
ALTER TABLE datacenters 
ADD COLUMN IF NOT EXISTS availability_zone_id BIGINT,
ADD COLUMN IF NOT EXISTS tier INT DEFAULT 3 CHECK (tier >= 1 AND tier <= 4),
ADD COLUMN IF NOT EXISTS dr_paired_with INT,
ADD COLUMN IF NOT EXISTS environment_id BIGINT,
ADD CONSTRAINT fk_datacenter_az FOREIGN KEY (availability_zone_id) REFERENCES availability_zones(id),
ADD CONSTRAINT fk_datacenter_dr FOREIGN KEY (dr_paired_with) REFERENCES datacenters(id),
ADD CONSTRAINT fk_datacenter_env FOREIGN KEY (environment_id) REFERENCES environments(id);

-- Update computes table to link to resource pools
ALTER TABLE computes
ADD COLUMN IF NOT EXISTS resource_pool_id BIGINT,
ADD COLUMN IF NOT EXISTS hypervisor VARCHAR(50),
ADD COLUMN IF NOT EXISTS is_physical BOOLEAN DEFAULT FALSE,
ADD CONSTRAINT fk_compute_pool FOREIGN KEY (resource_pool_id) REFERENCES resource_pools(id);

-- Add Tags table for flexible labeling
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(100) NOT NULL,
    value VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL, -- CLOUD, REGION, AZ, DATACENTER, COMPUTE, etc.
    entity_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tags_entity (entity_type, entity_id),
    INDEX idx_tags_key (key_name)
);

-- Insert some default data
INSERT INTO environments (name, code, type, partner_id, status) 
SELECT 'Production', 'PROD', 'PRODUCTION', p.id, 'ACTIVE'
FROM partners p
WHERE p.name = 'self' AND NOT EXISTS (SELECT 1 FROM environments WHERE code = 'PROD');

INSERT INTO environments (name, code, type, partner_id, status)
SELECT 'Development', 'DEV', 'DEVELOPMENT', p.id, 'ACTIVE'
FROM partners p
WHERE p.name = 'self' AND NOT EXISTS (SELECT 1 FROM environments WHERE code = 'DEV');

-- Add sample regions for existing clouds
INSERT INTO regions (name, code, geographic_area, cloud_id, compliance_zones)
SELECT 'Bangladesh Central', 'bd-central', 'South Asia', c.id, 'SOC2'
FROM clouds c
WHERE c.name = 'Telcobright' AND NOT EXISTS (SELECT 1 FROM regions WHERE code = 'bd-central');

-- Add sample availability zones
INSERT INTO availability_zones (name, code, region_id, zone_type, is_default)
SELECT 'BD-Central-1A', 'bd-central-1a', r.id, 'STANDARD', TRUE
FROM regions r
WHERE r.code = 'bd-central' AND NOT EXISTS (SELECT 1 FROM availability_zones WHERE code = 'bd-central-1a');

INSERT INTO availability_zones (name, code, region_id, zone_type, is_default)
SELECT 'BD-Central-1B', 'bd-central-1b', r.id, 'STANDARD', FALSE
FROM regions r
WHERE r.code = 'bd-central' AND NOT EXISTS (SELECT 1 FROM availability_zones WHERE code = 'bd-central-1b');