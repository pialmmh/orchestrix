-- Add comprehensive fields to compute table
ALTER TABLE computes
    -- Hardware Information
    ADD COLUMN brand VARCHAR(100),
    ADD COLUMN model VARCHAR(100),
    ADD COLUMN serial_number VARCHAR(100),
    ADD COLUMN asset_tag VARCHAR(100),
    ADD COLUMN rack_location VARCHAR(50),
    ADD COLUMN rack_unit INTEGER,
    ADD COLUMN power_consumption_watts INTEGER,
    ADD COLUMN thermal_output_btu INTEGER,
    
    -- OS and Software
    ADD COLUMN os_type VARCHAR(50),
    ADD COLUMN os_distribution VARCHAR(100),
    ADD COLUMN os_version VARCHAR(50),
    ADD COLUMN kernel_version VARCHAR(100),
    ADD COLUMN firmware_version VARCHAR(100),
    ADD COLUMN bios_version VARCHAR(100),
    
    -- Compute Roles and Purpose
    ADD COLUMN compute_role VARCHAR(50), -- DATABASE, APPLICATION, WEB, CACHE, QUEUE, LOAD_BALANCER, etc.
    ADD COLUMN purpose TEXT,
    ADD COLUMN environment_type VARCHAR(50), -- PRODUCTION, STAGING, DEVELOPMENT, TEST
    ADD COLUMN service_tier VARCHAR(50), -- CRITICAL, HIGH, MEDIUM, LOW
    ADD COLUMN business_unit VARCHAR(100),
    ADD COLUMN cost_center VARCHAR(50),
    
    -- Container and Virtualization Support
    ADD COLUMN virtualization_type VARCHAR(50), -- KVM, VMWARE, HYPERV, XEN, DOCKER, LXC, etc.
    ADD COLUMN supports_containers BOOLEAN DEFAULT false,
    ADD COLUMN container_runtime VARCHAR(50), -- DOCKER, CONTAINERD, CRIO, etc.
    ADD COLUMN orchestration_platform VARCHAR(50), -- KUBERNETES, OPENSHIFT, SWARM, etc.
    ADD COLUMN max_containers INTEGER,
    ADD COLUMN current_containers INTEGER DEFAULT 0,
    
    -- Network Configuration
    ADD COLUMN primary_mac_address VARCHAR(17),
    ADD COLUMN management_ip VARCHAR(45),
    ADD COLUMN public_ip VARCHAR(45),
    ADD COLUMN private_ip VARCHAR(45),
    ADD COLUMN ipmi_ip VARCHAR(45),
    ADD COLUMN vlan_ids TEXT, -- JSON array of VLAN IDs
    ADD COLUMN network_interfaces_count INTEGER DEFAULT 1,
    ADD COLUMN network_speed_gbps INTEGER,
    
    -- Storage Configuration
    ADD COLUMN storage_type VARCHAR(50), -- SSD, HDD, NVME, HYBRID
    ADD COLUMN storage_raid_level VARCHAR(10), -- RAID0, RAID1, RAID5, RAID10, etc.
    ADD COLUMN total_storage_gb INTEGER,
    ADD COLUMN used_storage_gb INTEGER DEFAULT 0,
    ADD COLUMN storage_iops INTEGER,
    
    -- Performance Metrics
    ADD COLUMN cpu_benchmark_score INTEGER,
    ADD COLUMN memory_bandwidth_gbps DECIMAL(10,2),
    ADD COLUMN network_latency_ms DECIMAL(10,2),
    ADD COLUMN uptime_days INTEGER DEFAULT 0,
    ADD COLUMN last_reboot_date TIMESTAMP,
    
    -- Compliance and Security
    ADD COLUMN compliance_status VARCHAR(50),
    ADD COLUMN security_zone VARCHAR(50), -- DMZ, INTERNAL, EXTERNAL, etc.
    ADD COLUMN encryption_enabled BOOLEAN DEFAULT false,
    ADD COLUMN last_security_scan_date DATE,
    ADD COLUMN patch_level VARCHAR(50),
    ADD COLUMN antivirus_status VARCHAR(50),
    
    -- Maintenance and Support
    ADD COLUMN warranty_expiry_date DATE,
    ADD COLUMN support_contract_id VARCHAR(100),
    ADD COLUMN maintenance_window VARCHAR(100),
    ADD COLUMN last_maintenance_date DATE,
    ADD COLUMN next_maintenance_date DATE,
    
    -- Monitoring and Management
    ADD COLUMN monitoring_enabled BOOLEAN DEFAULT true,
    ADD COLUMN monitoring_agent VARCHAR(100),
    ADD COLUMN management_tool VARCHAR(100), -- ANSIBLE, PUPPET, CHEF, etc.
    ADD COLUMN backup_enabled BOOLEAN DEFAULT false,
    ADD COLUMN backup_schedule VARCHAR(100),
    ADD COLUMN last_backup_date TIMESTAMP,
    
    -- Additional Metadata
    ADD COLUMN tags TEXT, -- JSON array of tags
    ADD COLUMN notes TEXT,
    ADD COLUMN custom_attributes TEXT; -- JSON object for custom fields

-- Create index for frequently queried fields
CREATE INDEX idx_computes_compute_role ON computes(compute_role);
CREATE INDEX idx_computes_environment_type ON computes(environment_type);
CREATE INDEX idx_computes_brand_model ON computes(brand, model);
CREATE INDEX idx_computes_os_type ON computes(os_type);
CREATE INDEX idx_computes_compliance_status ON computes(compliance_status);
CREATE INDEX idx_computes_warranty_expiry ON computes(warranty_expiry_date);