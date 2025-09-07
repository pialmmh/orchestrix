-- Add Network Element resource group
INSERT INTO resource_groups (name, display_name, description, category, is_active, sort_order, icon, color) VALUES
('network_element', 'Network Element', 'Network infrastructure devices and equipment for inventory tracking', 'IaaS', TRUE, 9, 'router', '#FF5722');

-- Insert service types for Network Element resource group
INSERT INTO resource_group_services (resource_group_id, service_type) 
SELECT id, 'Router' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Switch' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Firewall' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Load Balancer' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Access Point' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Gateway' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'VPN Device' FROM resource_groups WHERE name = 'network_element'
UNION ALL
SELECT id, 'Network Controller' FROM resource_groups WHERE name = 'network_element';

-- Create network_devices table for inventory tracking
CREATE TABLE network_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    device_type VARCHAR(50) NOT NULL, -- Router, Switch, Firewall, etc.
    vendor VARCHAR(100),
    model VARCHAR(100),
    version VARCHAR(50),
    serial_number VARCHAR(100),
    mac_address VARCHAR(18),
    
    -- Network Configuration
    management_ip VARCHAR(45),
    management_port INT DEFAULT 22,
    management_protocol VARCHAR(20) DEFAULT 'SSH', -- SSH, HTTPS, HTTP, SNMP
    subnet VARCHAR(45),
    gateway VARCHAR(45),
    dns_servers TEXT,
    vlan_id INT,
    
    -- Physical Location
    datacenter_id INT,
    resource_pool_id BIGINT,
    rack_position VARCHAR(50),
    port_count INT DEFAULT 0,
    power_consumption_watts INT DEFAULT 0,
    
    -- Operational Info
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, MAINTENANCE, ERROR
    operational_status VARCHAR(50) DEFAULT 'UNKNOWN', -- UP, DOWN, DEGRADED, UNKNOWN
    last_seen TIMESTAMP,
    uptime_hours BIGINT DEFAULT 0,
    
    -- Performance & Capacity
    cpu_utilization_percent DECIMAL(5,2) DEFAULT 0.0,
    memory_utilization_percent DECIMAL(5,2) DEFAULT 0.0,
    bandwidth_utilization_mbps DECIMAL(10,2) DEFAULT 0.0,
    total_memory_mb INT DEFAULT 0,
    available_storage_gb INT DEFAULT 0,
    
    -- Configuration & Management
    firmware_version VARCHAR(100),
    configuration_backup_path VARCHAR(500),
    last_backup_date TIMESTAMP,
    snmp_community VARCHAR(100),
    ssh_username VARCHAR(100),
    ssh_key_path VARCHAR(500),
    
    -- Business & Compliance
    environment VARCHAR(50) DEFAULT 'production', -- production, staging, development, test
    criticality VARCHAR(50) DEFAULT 'medium', -- low, medium, high, critical
    compliance_zone VARCHAR(100),
    cost_center VARCHAR(100),
    owner_contact VARCHAR(255),
    support_contract VARCHAR(255),
    
    -- Monitoring & Alerting
    monitoring_enabled BOOLEAN DEFAULT true,
    alert_email VARCHAR(255),
    alert_threshold_cpu DECIMAL(5,2) DEFAULT 80.0,
    alert_threshold_memory DECIMAL(5,2) DEFAULT 85.0,
    alert_threshold_bandwidth DECIMAL(5,2) DEFAULT 90.0,
    
    -- Metadata
    description TEXT,
    tags TEXT, -- JSON array of tags
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Foreign key constraints
    FOREIGN KEY (datacenter_id) REFERENCES datacenters(id) ON DELETE SET NULL,
    FOREIGN KEY (resource_pool_id) REFERENCES resource_pools(id) ON DELETE SET NULL
);

-- Create indexes for better performance
CREATE INDEX idx_network_devices_datacenter ON network_devices(datacenter_id);
CREATE INDEX idx_network_devices_resource_pool ON network_devices(resource_pool_id);
CREATE INDEX idx_network_devices_device_type ON network_devices(device_type);
CREATE INDEX idx_network_devices_status ON network_devices(status);
CREATE INDEX idx_network_devices_operational_status ON network_devices(operational_status);
CREATE INDEX idx_network_devices_management_ip ON network_devices(management_ip);
CREATE INDEX idx_network_devices_vendor ON network_devices(vendor);
CREATE INDEX idx_network_devices_environment ON network_devices(environment);

-- Insert sample router data for testing
INSERT INTO network_devices (
    name, display_name, device_type, vendor, model, version, serial_number,
    management_ip, management_port, management_protocol, status, operational_status,
    datacenter_id, description, environment, criticality, owner_contact,
    port_count, power_consumption_watts, cpu_utilization_percent, memory_utilization_percent,
    total_memory_mb, firmware_version, created_by
) VALUES
('RT-CORE-01', 'Core Router 01', 'Router', 'Cisco', 'ISR4331', '16.09.04', 'SN123456789',
    '192.168.1.1', 22, 'SSH', 'ACTIVE', 'UP',
    (SELECT id FROM datacenters LIMIT 1), 'Primary core router for main datacenter', 'production', 'critical', 'network-admin@company.com',
    4, 50, 15.5, 32.1,
    2048, '16.09.04', 'system'),
    
('RT-EDGE-01', 'Edge Router 01', 'Router', 'Juniper', 'MX204', '19.4R3-S2.6', 'JN987654321',
    '192.168.1.10', 22, 'SSH', 'ACTIVE', 'UP',
    (SELECT id FROM datacenters LIMIT 1), 'Edge router for external connections', 'production', 'high', 'network-admin@company.com',
    8, 75, 22.8, 45.2,
    4096, '19.4R3-S2.6', 'system'),
    
('RT-LAB-01', 'Lab Router 01', 'Router', 'MikroTik', 'CCR1036-12G-4S', '6.49.6', 'MT555666777',
    '192.168.100.1', 22, 'SSH', 'ACTIVE', 'UP',
    (SELECT id FROM datacenters LIMIT 1), 'Router for lab and testing environment', 'development', 'low', 'lab-admin@company.com',
    12, 30, 8.3, 28.7,
    1024, '6.49.6', 'system');