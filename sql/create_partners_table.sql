-- Create partners table for managing cloud providers and vendors
CREATE TABLE IF NOT EXISTS partners (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'cloud-provider', 'vendor', 'both'
    roles JSON, -- Store multiple roles as JSON array
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    website VARCHAR(255),
    billing_account_id VARCHAR(100),
    api_key VARCHAR(255),
    api_secret VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_partner_type (type),
    INDEX idx_partner_status (status)
);

-- Add partner_id to datacenters table to link with cloud provider partners
ALTER TABLE datacenters 
ADD COLUMN partner_id INT AFTER provider,
ADD CONSTRAINT fk_datacenter_partner 
    FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE SET NULL,
ADD INDEX idx_datacenter_partner (partner_id);

-- Insert major cloud providers as partners
INSERT INTO partners (name, display_name, type, roles, contact_email, website, status) VALUES
('amazon', 'Amazon Web Services (AWS)', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@aws.amazon.com', 'https://aws.amazon.com', 'ACTIVE'),
('google', 'Google Cloud Platform', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@cloud.google.com', 'https://cloud.google.com', 'ACTIVE'),
('microsoft', 'Microsoft Azure', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@azure.microsoft.com', 'https://azure.microsoft.com', 'ACTIVE'),
('ibm', 'IBM Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@ibm.com', 'https://www.ibm.com/cloud', 'ACTIVE'),
('oracle', 'Oracle Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@oracle.com', 'https://www.oracle.com/cloud', 'ACTIVE'),
('alibaba', 'Alibaba Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@alibabacloud.com', 'https://www.alibabacloud.com', 'ACTIVE'),
('digitalocean', 'DigitalOcean', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@digitalocean.com', 'https://www.digitalocean.com', 'ACTIVE'),
('linode', 'Linode', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@linode.com', 'https://www.linode.com', 'ACTIVE'),
('vultr', 'Vultr', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@vultr.com', 'https://www.vultr.com', 'ACTIVE'),
('ovhcloud', 'OVHcloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@ovhcloud.com', 'https://www.ovhcloud.com', 'ACTIVE'),
('hetzner', 'Hetzner', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@hetzner.com', 'https://www.hetzner.com', 'ACTIVE'),
('equinix', 'Equinix Metal', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@equinix.com', 'https://metal.equinix.com', 'ACTIVE');

-- Update existing datacenters to link with partners based on provider field
UPDATE datacenters d
LEFT JOIN partners p ON 
    (d.provider = 'AWS' AND p.name = 'amazon') OR
    (d.provider = 'Google Cloud' AND p.name = 'google') OR
    (d.provider = 'Azure' AND p.name = 'microsoft') OR
    (d.provider = 'IBM Cloud' AND p.name = 'ibm') OR
    (d.provider = 'Oracle Cloud' AND p.name = 'oracle') OR
    (d.provider = 'Alibaba Cloud' AND p.name = 'alibaba') OR
    (d.provider = 'DigitalOcean' AND p.name = 'digitalocean') OR
    (d.provider = 'Linode' AND p.name = 'linode') OR
    (d.provider = 'Vultr' AND p.name = 'vultr') OR
    (d.provider = 'OVHcloud' AND p.name = 'ovhcloud') OR
    (d.provider = 'Hetzner' AND p.name = 'hetzner') OR
    (d.provider = 'Equinix' AND p.name = 'equinix')
SET d.partner_id = p.id
WHERE p.id IS NOT NULL;