-- Cloud Infrastructure Hierarchy Sample Data
-- This script creates sample data for the cloud hierarchy

-- Insert sample clouds
INSERT INTO clouds (name, description, client_name, deployment_region, status, created_at, updated_at) VALUES
('TelcoBright Production Cloud', 'Main production cloud infrastructure for TelcoBright services', 'TelcoBright', 'Asia-Pacific', 'ACTIVE', NOW(), NOW()),
('Development Cloud', 'Development and testing environment', 'TelcoBright', 'US-East', 'ACTIVE', NOW(), NOW());

-- Update existing datacenters to link to clouds and add DR flags
UPDATE datacenters SET cloud_id = 1, is_dr_site = false WHERE name = 'Dhaka DC';
UPDATE datacenters SET cloud_id = 1, is_dr_site = true WHERE name = 'Singapore DC' AND id = (SELECT id FROM datacenters WHERE name = 'Singapore DC' LIMIT 1);
UPDATE datacenters SET cloud_id = 2, is_dr_site = false WHERE name LIKE '%Test%' OR name LIKE '%Dev%';

-- Insert sample computes
INSERT INTO computes (name, description, compute_type, hostname, ip_address, operating_system, cpu_cores, memory_gb, disk_gb, status, cloud_id, datacenter_id, created_at, updated_at) VALUES
('mustafa-pc', 'Development workstation', 'DEDICATED', 'mustafa-desktop', '192.168.1.100', 'Ubuntu 22.04 LTS', 16, 64, 2048, 'ACTIVE', 1, 
  (SELECT id FROM datacenters WHERE name = 'Dhaka DC' LIMIT 1), NOW(), NOW()),
('prod-server-01', 'Main production server', 'DEDICATED', 'prod01.telcobright.com', '10.0.1.10', 'Ubuntu 20.04 LTS Server', 32, 128, 4096, 'ACTIVE', 1, 
  (SELECT id FROM datacenters WHERE name = 'Dhaka DC' LIMIT 1), NOW(), NOW()),
('test-vm-01', 'Testing virtual machine', 'VM', 'test-vm01', '10.0.2.10', 'Ubuntu 22.04 LTS', 8, 16, 500, 'ACTIVE', 2, 
  (SELECT id FROM datacenters WHERE name LIKE '%Test%' LIMIT 1), NOW(), NOW());

-- Insert sample containers
INSERT INTO containers (name, description, container_type, container_id, image, image_version, status, ip_address, exposed_ports, environment_vars, mount_points, cpu_limit, memory_limit, auto_start, compute_id, created_at, updated_at) VALUES
-- LXD containers on mustafa-pc
('dev-container', 'Main development container', 'LXD', 'dev-container', 'ubuntu/jammy', '22.04', 'RUNNING', '10.0.3.10', '22,80,443,8080', 'ENVIRONMENT=development', '/home/mustafa/projects:/workspace', '4.0', '8GB', true, 
  (SELECT id FROM computes WHERE name = 'mustafa-pc'), NOW(), NOW()),
('clarity', 'Clarity analytics container', 'LXD', 'clarity-lxd', 'ubuntu/jammy', '22.04', 'RUNNING', '10.0.3.11', '3000,8000', 'NODE_ENV=development', '/var/clarity:/data', '2.0', '4GB', true, 
  (SELECT id FROM computes WHERE name = 'mustafa-pc'), NOW(), NOW()),
('freeswitch', 'FreeSWITCH telecom container', 'LXD', 'freeswitch-lxd', 'ubuntu/focal', '20.04', 'RUNNING', '10.0.3.12', '5060,5080,8021', 'FS_ENV=development', '/etc/freeswitch:/etc/freeswitch', '4.0', '8GB', true, 
  (SELECT id FROM computes WHERE name = 'mustafa-pc'), NOW(), NOW()),

-- Docker containers on mustafa-pc
('mysql', 'MySQL database container', 'DOCKER', 'mysql-dev', 'mysql', '8.0', 'RUNNING', '127.0.0.1', '3306', 'MYSQL_ROOT_PASSWORD=123456,MYSQL_DATABASE=orchestrix', '/var/lib/mysql:/var/lib/mysql', '2.0', '4GB', true, 
  (SELECT id FROM computes WHERE name = 'mustafa-pc'), NOW(), NOW()),

-- Production containers
('prod-web', 'Production web application', 'DOCKER', 'prod-web-001', 'nginx', '1.21', 'RUNNING', '10.0.1.20', '80,443', 'ENV=production', '/var/www:/usr/share/nginx/html', '4.0', '8GB', true, 
  (SELECT id FROM computes WHERE name = 'prod-server-01'), NOW(), NOW()),
('prod-api', 'Production API service', 'DOCKER', 'prod-api-001', 'orchestrix/api', 'v1.2.3', 'RUNNING', '10.0.1.21', '8080', 'SPRING_PROFILES_ACTIVE=production', '/app/logs:/logs', '8.0', '16GB', true, 
  (SELECT id FROM computes WHERE name = 'prod-server-01'), NOW(), NOW()),

-- Test containers
('test-app', 'Testing application container', 'PODMAN', 'test-app-pod', 'orchestrix/test', 'latest', 'RUNNING', '10.0.2.20', '8080,9090', 'ENV=test', '/tmp/test:/app/test', '2.0', '4GB', false, 
  (SELECT id FROM computes WHERE name = 'test-vm-01'), NOW(), NOW());

-- Insert sample storage systems
INSERT INTO storages (name, description, storage_type, capacity_gb, used_gb, available_gb, protocol, mount_path, status, cloud_id, datacenter_id, created_at, updated_at) VALUES
('Primary SAN Storage', 'Main SAN storage for production', 'SAN', 10240, 6144, 4096, 'iSCSI', '/mnt/san-primary', 'ACTIVE', 1, 
  (SELECT id FROM datacenters WHERE name = 'Dhaka DC' LIMIT 1), NOW(), NOW()),
('Backup NAS Storage', 'Network attached storage for backups', 'NAS', 20480, 8192, 12288, 'NFS', '/mnt/nas-backup', 'ACTIVE', 1, 
  (SELECT id FROM datacenters WHERE name = 'Dhaka DC' LIMIT 1), NOW(), NOW()),
('Local Dev Storage', 'Local development storage', 'LOCAL', 2048, 1024, 1024, 'Local', '/dev/storage', 'ACTIVE', 2, NULL, NOW(), NOW());

-- Insert sample networking configurations
INSERT INTO networking (name, description, network_type, network_cidr, vlan_id, gateway, dns_servers, dhcp_enabled, status, cloud_id, datacenter_id, created_at, updated_at) VALUES
('Production VLAN', 'Main production network', 'VLAN', '10.0.1.0/24', 100, '10.0.1.1', '8.8.8.8,1.1.1.1', true, 'ACTIVE', 1, 
  (SELECT id FROM datacenters WHERE name = 'Dhaka DC' LIMIT 1), NOW(), NOW()),
('Development VLAN', 'Development network segment', 'VLAN', '10.0.2.0/24', 200, '10.0.2.1', '8.8.8.8,1.1.1.1', true, 'ACTIVE', 2, 
  (SELECT id FROM datacenters WHERE name LIKE '%Test%' LIMIT 1), NOW(), NOW()),
('Container Network', 'Container overlay network', 'OVERLAY', '10.0.3.0/24', NULL, '10.0.3.1', '8.8.8.8', false, 'ACTIVE', 1, NULL, NOW(), NOW());

-- Verify data insertion
SELECT 'Clouds:' as entity, COUNT(*) as count FROM clouds
UNION ALL
SELECT 'Datacenters:', COUNT(*) FROM datacenters WHERE cloud_id IS NOT NULL
UNION ALL  
SELECT 'Computes:', COUNT(*) FROM computes
UNION ALL
SELECT 'Containers:', COUNT(*) FROM containers
UNION ALL
SELECT 'Storages:', COUNT(*) FROM storages
UNION ALL
SELECT 'Networks:', COUNT(*) FROM networking;