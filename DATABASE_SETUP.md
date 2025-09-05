# Database Setup Guide

This guide explains how to set up and populate the Orchestrix database with seed data.

## Prerequisites

- MySQL server running on `127.0.0.1:3306`
- Database user: `root` with password: `123456`
- Database: `orchestrix` (will be created automatically by Spring Boot)

## Quick Setup

### 1. Test Database Connection
```bash
./test-db-connection.sh
```

### 2. Populate Seed Data
```bash
./populate-seed-data.sh
```

## What Gets Populated

### Countries (34 entries)
- Major developed countries with proper ISO codes
- Includes regions: North America, Europe, Asia, Oceania, South America, Africa
- Countries with states/provinces marked with `has_states = TRUE`

### States/Provinces (72 entries)
- US states (California, New York, Texas, etc.)
- Canadian provinces (Ontario, British Columbia, Quebec, etc.)
- German states (Bavaria, Baden-Württemberg, etc.)
- UK nations (England, Scotland, Wales, Northern Ireland)
- Australian states (NSW, Victoria, Queensland, etc.)
- Brazilian states (São Paulo, Rio de Janeiro, etc.)
- Indian states (Maharashtra, Karnataka, Tamil Nadu, etc.)
- Chinese provinces (Beijing, Shanghai, Guangdong, etc.)

### Cities (71 entries)
- Major global cities with coordinates
- Capital cities marked with `is_capital = TRUE`
- Includes tech hubs: Silicon Valley, Seattle, Austin, Bangalore, etc.
- Financial centers: New York, London, Hong Kong, Singapore, etc.

### Partners (22 entries)
**Cloud Providers:**
- Amazon Web Services (AWS)
- Google Cloud Platform
- Microsoft Azure
- IBM Cloud
- Oracle Cloud
- Alibaba Cloud
- DigitalOcean
- Linode (Akamai)
- Vultr
- OVHcloud
- Hetzner
- Equinix Metal
- Cloudflare
- Contabo
- Scaleway

**Infrastructure Vendors:**
- Dell Technologies
- Hewlett Packard Enterprise
- Cisco Systems
- Juniper Networks
- VMware
- Red Hat
- Canonical (Ubuntu)

### Datacenters (27 entries)
- AWS regions: US East, US West, Europe, Asia Pacific
- Google Cloud regions: US Central, Europe West, Asia Southeast
- Microsoft Azure regions: East US, West Europe, Southeast Asia
- Other cloud provider data centers
- Sample enterprise data centers

## Database Schema

### Key Relationships
```
countries (1) -> (many) states
countries (1) -> (many) cities
states (1) -> (many) cities
partners (1) -> (many) datacenters
countries (1) -> (many) datacenters
cities (1) -> (many) datacenters
```

### JSON Fields
- `partners.roles` - Array of roles: ["cloud-provider", "vendor", "hardware-vendor", etc.]
- `compute_resources.tags` - Array of tags for categorization

## Manual Database Operations

### Connect to Database
```bash
mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix
```

### Sample Queries
```sql
-- Countries with states
SELECT name, region FROM countries WHERE has_states = TRUE;

-- Major cloud providers
SELECT display_name, website FROM partners WHERE type = 'cloud-provider';

-- Capital cities
SELECT ci.name, co.name as country 
FROM cities ci 
JOIN countries co ON ci.country_id = co.id 
WHERE ci.is_capital = TRUE;

-- AWS data centers
SELECT d.name, ci.name as city, co.name as country 
FROM datacenters d
LEFT JOIN cities ci ON d.city_id = ci.id
JOIN countries co ON d.country_id = co.id
JOIN partners p ON d.partner_id = p.id
WHERE p.name = 'amazon';
```

## Reset Database
To clear and repopulate all seed data:
```bash
./populate-seed-data.sh
```

The script automatically clears existing data before inserting new records.

## Troubleshooting

### Connection Issues
- Ensure MySQL is running: `sudo systemctl status mysql`
- Check if database exists: `SHOW DATABASES;`
- Verify credentials: `root/123456`
- Confirm host accessibility: `127.0.0.1:3306`

### Permission Issues
- Make scripts executable: `chmod +x *.sh`
- Check MySQL user permissions: `GRANT ALL PRIVILEGES ON orchestrix.* TO 'root'@'localhost';`

### Data Issues
- Check for foreign key constraints
- Verify JSON format for partners.roles
- Ensure proper character encoding (UTF-8)