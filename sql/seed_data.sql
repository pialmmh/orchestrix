USE orchestrix;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS=0;

-- Clear existing data in proper order
TRUNCATE TABLE compute_automation_config;
TRUNCATE TABLE compute_access_credentials;
TRUNCATE TABLE compute_resources;
TRUNCATE TABLE datacenters;
TRUNCATE TABLE cities;
TRUNCATE TABLE states;
TRUNCATE TABLE partners;
TRUNCATE TABLE countries;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS=1;

-- =====================================================
-- COUNTRIES DATA
-- =====================================================
INSERT INTO countries (code, code3, name, region, has_states) VALUES
-- Major developed countries
('US', 'USA', 'United States', 'North America', TRUE),
('CA', 'CAN', 'Canada', 'North America', TRUE),
('GB', 'GBR', 'United Kingdom', 'Europe', TRUE),
('DE', 'DEU', 'Germany', 'Europe', TRUE),
('FR', 'FRA', 'France', 'Europe', FALSE),
('IT', 'ITA', 'Italy', 'Europe', FALSE),
('ES', 'ESP', 'Spain', 'Europe', FALSE),
('NL', 'NLD', 'Netherlands', 'Europe', FALSE),
('CH', 'CHE', 'Switzerland', 'Europe', FALSE),
('SE', 'SWE', 'Sweden', 'Europe', FALSE),
('NO', 'NOR', 'Norway', 'Europe', FALSE),
('DK', 'DNK', 'Denmark', 'Europe', FALSE),
('FI', 'FIN', 'Finland', 'Europe', FALSE),
('AU', 'AUS', 'Australia', 'Oceania', TRUE),
('NZ', 'NZL', 'New Zealand', 'Oceania', FALSE),
('JP', 'JPN', 'Japan', 'Asia', FALSE),
('KR', 'KOR', 'South Korea', 'Asia', FALSE),
('SG', 'SGP', 'Singapore', 'Asia', FALSE),
('HK', 'HKG', 'Hong Kong', 'Asia', FALSE),
('CN', 'CHN', 'China', 'Asia', TRUE),
('IN', 'IND', 'India', 'Asia', TRUE),
('BR', 'BRA', 'Brazil', 'South America', TRUE),
('MX', 'MEX', 'Mexico', 'North America', TRUE),
('AR', 'ARG', 'Argentina', 'South America', TRUE),
('RU', 'RUS', 'Russia', 'Europe', TRUE),
('TR', 'TUR', 'Turkey', 'Asia', FALSE),
('SA', 'SAU', 'Saudi Arabia', 'Asia', FALSE),
('AE', 'ARE', 'United Arab Emirates', 'Asia', FALSE),
('IL', 'ISR', 'Israel', 'Asia', FALSE),
('ZA', 'ZAF', 'South Africa', 'Africa', FALSE),
('EG', 'EGY', 'Egypt', 'Africa', FALSE),
('NG', 'NGA', 'Nigeria', 'Africa', FALSE),
('KE', 'KEN', 'Kenya', 'Africa', FALSE),
('BD', 'BGD', 'Bangladesh', 'Asia', FALSE);

-- =====================================================
-- STATES DATA (for countries with has_states = TRUE)
-- =====================================================

-- United States major states
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'US'), 'CA', 'California'),
((SELECT id FROM countries WHERE code = 'US'), 'NY', 'New York'),
((SELECT id FROM countries WHERE code = 'US'), 'TX', 'Texas'),
((SELECT id FROM countries WHERE code = 'US'), 'FL', 'Florida'),
((SELECT id FROM countries WHERE code = 'US'), 'IL', 'Illinois'),
((SELECT id FROM countries WHERE code = 'US'), 'WA', 'Washington'),
((SELECT id FROM countries WHERE code = 'US'), 'VA', 'Virginia'),
((SELECT id FROM countries WHERE code = 'US'), 'OR', 'Oregon'),
((SELECT id FROM countries WHERE code = 'US'), 'GA', 'Georgia'),
((SELECT id FROM countries WHERE code = 'US'), 'MA', 'Massachusetts'),
((SELECT id FROM countries WHERE code = 'US'), 'OH', 'Ohio'),
((SELECT id FROM countries WHERE code = 'US'), 'PA', 'Pennsylvania'),
((SELECT id FROM countries WHERE code = 'US'), 'AZ', 'Arizona'),
((SELECT id FROM countries WHERE code = 'US'), 'CO', 'Colorado'),
((SELECT id FROM countries WHERE code = 'US'), 'UT', 'Utah');

-- Canada major provinces
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'CA'), 'ON', 'Ontario'),
((SELECT id FROM countries WHERE code = 'CA'), 'BC', 'British Columbia'),
((SELECT id FROM countries WHERE code = 'CA'), 'QC', 'Quebec'),
((SELECT id FROM countries WHERE code = 'CA'), 'AB', 'Alberta'),
((SELECT id FROM countries WHERE code = 'CA'), 'MB', 'Manitoba'),
((SELECT id FROM countries WHERE code = 'CA'), 'SK', 'Saskatchewan'),
((SELECT id FROM countries WHERE code = 'CA'), 'NS', 'Nova Scotia');

-- Germany major states
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'DE'), 'BY', 'Bavaria'),
((SELECT id FROM countries WHERE code = 'DE'), 'BW', 'Baden-Württemberg'),
((SELECT id FROM countries WHERE code = 'DE'), 'NW', 'North Rhine-Westphalia'),
((SELECT id FROM countries WHERE code = 'DE'), 'HE', 'Hesse'),
((SELECT id FROM countries WHERE code = 'DE'), 'NI', 'Lower Saxony'),
((SELECT id FROM countries WHERE code = 'DE'), 'BE', 'Berlin'),
((SELECT id FROM countries WHERE code = 'DE'), 'HH', 'Hamburg');

-- UK nations/regions
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'GB'), 'ENG', 'England'),
((SELECT id FROM countries WHERE code = 'GB'), 'SCT', 'Scotland'),
((SELECT id FROM countries WHERE code = 'GB'), 'WLS', 'Wales'),
((SELECT id FROM countries WHERE code = 'GB'), 'NIR', 'Northern Ireland');

-- Australia states
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'AU'), 'NSW', 'New South Wales'),
((SELECT id FROM countries WHERE code = 'AU'), 'VIC', 'Victoria'),
((SELECT id FROM countries WHERE code = 'AU'), 'QLD', 'Queensland'),
((SELECT id FROM countries WHERE code = 'AU'), 'WA', 'Western Australia'),
((SELECT id FROM countries WHERE code = 'AU'), 'SA', 'South Australia'),
((SELECT id FROM countries WHERE code = 'AU'), 'TAS', 'Tasmania'),
((SELECT id FROM countries WHERE code = 'AU'), 'ACT', 'Australian Capital Territory');

-- Brazil states
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'BR'), 'SP', 'São Paulo'),
((SELECT id FROM countries WHERE code = 'BR'), 'RJ', 'Rio de Janeiro'),
((SELECT id FROM countries WHERE code = 'BR'), 'MG', 'Minas Gerais'),
((SELECT id FROM countries WHERE code = 'BR'), 'RS', 'Rio Grande do Sul'),
((SELECT id FROM countries WHERE code = 'BR'), 'PR', 'Paraná'),
((SELECT id FROM countries WHERE code = 'BR'), 'SC', 'Santa Catarina'),
((SELECT id FROM countries WHERE code = 'BR'), 'BA', 'Bahia'),
((SELECT id FROM countries WHERE code = 'BR'), 'GO', 'Goiás'),
((SELECT id FROM countries WHERE code = 'BR'), 'PE', 'Pernambuco'),
((SELECT id FROM countries WHERE code = 'BR'), 'CE', 'Ceará');

-- India states (major ones)
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'IN'), 'MH', 'Maharashtra'),
((SELECT id FROM countries WHERE code = 'IN'), 'KA', 'Karnataka'),
((SELECT id FROM countries WHERE code = 'IN'), 'TN', 'Tamil Nadu'),
((SELECT id FROM countries WHERE code = 'IN'), 'TG', 'Telangana'),
((SELECT id FROM countries WHERE code = 'IN'), 'AP', 'Andhra Pradesh'),
((SELECT id FROM countries WHERE code = 'IN'), 'GJ', 'Gujarat'),
((SELECT id FROM countries WHERE code = 'IN'), 'RJ', 'Rajasthan'),
((SELECT id FROM countries WHERE code = 'IN'), 'UP', 'Uttar Pradesh'),
((SELECT id FROM countries WHERE code = 'IN'), 'WB', 'West Bengal'),
((SELECT id FROM countries WHERE code = 'IN'), 'DL', 'Delhi'),
((SELECT id FROM countries WHERE code = 'IN'), 'HR', 'Haryana'),
((SELECT id FROM countries WHERE code = 'IN'), 'PB', 'Punjab');

-- China major provinces
INSERT INTO states (country_id, code, name) VALUES
((SELECT id FROM countries WHERE code = 'CN'), 'BJ', 'Beijing'),
((SELECT id FROM countries WHERE code = 'CN'), 'SH', 'Shanghai'),
((SELECT id FROM countries WHERE code = 'CN'), 'GD', 'Guangdong'),
((SELECT id FROM countries WHERE code = 'CN'), 'ZJ', 'Zhejiang'),
((SELECT id FROM countries WHERE code = 'CN'), 'JS', 'Jiangsu'),
((SELECT id FROM countries WHERE code = 'CN'), 'SD', 'Shandong'),
((SELECT id FROM countries WHERE code = 'CN'), 'HN', 'Henan'),
((SELECT id FROM countries WHERE code = 'CN'), 'SC', 'Sichuan'),
((SELECT id FROM countries WHERE code = 'CN'), 'HB', 'Hubei'),
((SELECT id FROM countries WHERE code = 'CN'), 'FJ', 'Fujian');

-- =====================================================
-- CITIES DATA
-- =====================================================

-- Major global cities
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude) VALUES
-- United States
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'NY' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'New York', FALSE, 40.7128, -74.0060),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Los Angeles', FALSE, 34.0522, -118.2437),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'San Francisco', FALSE, 37.7749, -122.4194),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'San Jose', FALSE, 37.3382, -121.8863),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'WA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Seattle', FALSE, 47.6062, -122.3321),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'IL' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Chicago', FALSE, 41.8781, -87.6298),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Dallas', FALSE, 32.7767, -96.7970),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Austin', FALSE, 30.2672, -97.7431),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'VA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Virginia Beach', FALSE, 36.8529, -75.9780),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'OR' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Portland', FALSE, 45.5152, -122.6784),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'GA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Atlanta', FALSE, 33.7490, -84.3880),
((SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'FL' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 'Miami', FALSE, 25.7617, -80.1918),

-- Canada
((SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'ON' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 'Toronto', FALSE, 43.6532, -79.3832),
((SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'BC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 'Vancouver', FALSE, 49.2827, -123.1207),
((SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'QC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 'Montreal', FALSE, 45.5017, -73.5673),
((SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'ON' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 'Ottawa', TRUE, 45.4215, -75.6919),
((SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'AB' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 'Calgary', FALSE, 51.0447, -114.0719),

-- United Kingdom
((SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 'London', TRUE, 51.5074, -0.1278),
((SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 'Manchester', FALSE, 53.4808, -2.2426),
((SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 'Birmingham', FALSE, 52.4862, -1.8904),
((SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 'Edinburgh', FALSE, 55.9533, -3.1883),
((SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 'Glasgow', FALSE, 55.8642, -4.2518),

-- Germany
((SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 'Berlin', TRUE, 52.5200, 13.4050),
((SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BY' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 'Munich', FALSE, 48.1351, 11.5820),
((SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HH' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 'Hamburg', FALSE, 53.5511, 9.9937),
((SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 'Frankfurt', FALSE, 50.1109, 8.6821),

-- Australia
((SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'NSW' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 'Sydney', FALSE, -33.8688, 151.2093),
((SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'VIC' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 'Melbourne', FALSE, -37.8136, 144.9631),
((SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'QLD' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 'Brisbane', FALSE, -27.4698, 153.0251),
((SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'WA' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 'Perth', FALSE, -31.9505, 115.8605),
((SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'ACT' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 'Canberra', TRUE, -35.2809, 149.1300),

-- Major cities without states
((SELECT id FROM countries WHERE code = 'FR'), NULL, 'Paris', TRUE, 48.8566, 2.3522),
((SELECT id FROM countries WHERE code = 'FR'), NULL, 'Lyon', FALSE, 45.7640, 4.8357),
((SELECT id FROM countries WHERE code = 'FR'), NULL, 'Marseille', FALSE, 43.2965, 5.3698),
((SELECT id FROM countries WHERE code = 'IT'), NULL, 'Rome', TRUE, 41.9028, 12.4964),
((SELECT id FROM countries WHERE code = 'IT'), NULL, 'Milan', FALSE, 45.4642, 9.1900),
((SELECT id FROM countries WHERE code = 'ES'), NULL, 'Madrid', TRUE, 40.4168, -3.7038),
((SELECT id FROM countries WHERE code = 'ES'), NULL, 'Barcelona', FALSE, 41.3851, 2.1734),
((SELECT id FROM countries WHERE code = 'NL'), NULL, 'Amsterdam', TRUE, 52.3676, 4.9041),
((SELECT id FROM countries WHERE code = 'CH'), NULL, 'Zurich', FALSE, 47.3769, 8.5417),
((SELECT id FROM countries WHERE code = 'SE'), NULL, 'Stockholm', TRUE, 59.3293, 18.0686),
((SELECT id FROM countries WHERE code = 'NO'), NULL, 'Oslo', TRUE, 59.9139, 10.7522),
((SELECT id FROM countries WHERE code = 'DK'), NULL, 'Copenhagen', TRUE, 55.6761, 12.5683),
((SELECT id FROM countries WHERE code = 'FI'), NULL, 'Helsinki', TRUE, 60.1699, 24.9384),
((SELECT id FROM countries WHERE code = 'JP'), NULL, 'Tokyo', TRUE, 35.6762, 139.6503),
((SELECT id FROM countries WHERE code = 'JP'), NULL, 'Osaka', FALSE, 34.6937, 135.5023),
((SELECT id FROM countries WHERE code = 'KR'), NULL, 'Seoul', TRUE, 37.5665, 126.9780),
((SELECT id FROM countries WHERE code = 'SG'), NULL, 'Singapore', TRUE, 1.3521, 103.8198),
((SELECT id FROM countries WHERE code = 'HK'), NULL, 'Hong Kong', TRUE, 22.3193, 114.1694),

-- India major cities
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'MH' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'Mumbai', FALSE, 19.0760, 72.8777),
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'DL' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'New Delhi', TRUE, 28.6139, 77.2090),
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'KA' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'Bangalore', FALSE, 12.9716, 77.5946),
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'TG' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'Hyderabad', FALSE, 17.3850, 78.4867),
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'TN' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'Chennai', FALSE, 13.0827, 80.2707),
((SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'MH' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 'Pune', FALSE, 18.5204, 73.8567),

-- China major cities
((SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'BJ' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 'Beijing', TRUE, 39.9042, 116.4074),
((SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'SH' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 'Shanghai', FALSE, 31.2304, 121.4737),
((SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'GD' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 'Guangzhou', FALSE, 23.1291, 113.2644),
((SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'GD' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 'Shenzhen', FALSE, 22.5431, 114.0579),

-- Brazil major cities  
((SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'SP' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 'São Paulo', FALSE, -23.5505, -46.6333),
((SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'RJ' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 'Rio de Janeiro', FALSE, -22.9068, -43.1729),

-- Other important cities
((SELECT id FROM countries WHERE code = 'NZ'), NULL, 'Auckland', FALSE, -36.8485, 174.7633),
((SELECT id FROM countries WHERE code = 'NZ'), NULL, 'Wellington', TRUE, -41.2865, 174.7762),
((SELECT id FROM countries WHERE code = 'ZA'), NULL, 'Cape Town', FALSE, -33.9249, 18.4241),
((SELECT id FROM countries WHERE code = 'ZA'), NULL, 'Johannesburg', FALSE, -26.2041, 28.0473),
((SELECT id FROM countries WHERE code = 'AE'), NULL, 'Dubai', FALSE, 25.2048, 55.2708),
((SELECT id FROM countries WHERE code = 'AE'), NULL, 'Abu Dhabi', TRUE, 24.2539, 54.3773),
((SELECT id FROM countries WHERE code = 'SA'), NULL, 'Riyadh', TRUE, 24.7136, 46.6753),
((SELECT id FROM countries WHERE code = 'IL'), NULL, 'Tel Aviv', FALSE, 32.0853, 34.7818),
((SELECT id FROM countries WHERE code = 'TR'), NULL, 'Istanbul', FALSE, 41.0082, 28.9784),
((SELECT id FROM countries WHERE code = 'EG'), NULL, 'Cairo', TRUE, 30.0444, 31.2357);

-- =====================================================
-- PARTNERS DATA (Cloud Providers and Vendors)
-- =====================================================
INSERT INTO partners (name, display_name, type, roles, contact_email, website, status) VALUES
('amazon', 'Amazon Web Services (AWS)', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@aws.amazon.com', 'https://aws.amazon.com', 'ACTIVE'),
('google', 'Google Cloud Platform', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@cloud.google.com', 'https://cloud.google.com', 'ACTIVE'),
('microsoft', 'Microsoft Azure', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@azure.microsoft.com', 'https://azure.microsoft.com', 'ACTIVE'),
('ibm', 'IBM Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@ibm.com', 'https://www.ibm.com/cloud', 'ACTIVE'),
('oracle', 'Oracle Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@oracle.com', 'https://www.oracle.com/cloud', 'ACTIVE'),
('alibaba', 'Alibaba Cloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@alibabacloud.com', 'https://www.alibabacloud.com', 'ACTIVE'),
('digitalocean', 'DigitalOcean', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@digitalocean.com', 'https://www.digitalocean.com', 'ACTIVE'),
('linode', 'Akamai Connected Cloud (Linode)', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@linode.com', 'https://www.linode.com', 'ACTIVE'),
('vultr', 'Vultr', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@vultr.com', 'https://www.vultr.com', 'ACTIVE'),
('ovhcloud', 'OVHcloud', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@ovhcloud.com', 'https://www.ovhcloud.com', 'ACTIVE'),
('hetzner', 'Hetzner', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@hetzner.com', 'https://www.hetzner.com', 'ACTIVE'),
('equinix', 'Equinix Metal', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@equinix.com', 'https://metal.equinix.com', 'ACTIVE'),
('cloudflare', 'Cloudflare', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@cloudflare.com', 'https://www.cloudflare.com', 'ACTIVE'),
('contabo', 'Contabo', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@contabo.com', 'https://contabo.com', 'ACTIVE'),
('scaleway', 'Scaleway', 'cloud-provider', '["cloud-provider", "vendor"]', 'billing@scaleway.com', 'https://www.scaleway.com', 'ACTIVE'),

-- Infrastructure vendors
('dell', 'Dell Technologies', 'vendor', '["hardware-vendor", "vendor"]', 'sales@dell.com', 'https://www.dell.com', 'ACTIVE'),
('hp', 'Hewlett Packard Enterprise', 'vendor', '["hardware-vendor", "vendor"]', 'sales@hpe.com', 'https://www.hpe.com', 'ACTIVE'),
('cisco', 'Cisco Systems', 'vendor', '["network-vendor", "vendor"]', 'sales@cisco.com', 'https://www.cisco.com', 'ACTIVE'),
('juniper', 'Juniper Networks', 'vendor', '["network-vendor", "vendor"]', 'sales@juniper.net', 'https://www.juniper.net', 'ACTIVE'),
('vmware', 'VMware', 'vendor', '["software-vendor", "vendor"]', 'sales@vmware.com', 'https://www.vmware.com', 'ACTIVE'),
('redhat', 'Red Hat', 'vendor', '["software-vendor", "vendor"]', 'sales@redhat.com', 'https://www.redhat.com', 'ACTIVE'),
('canonical', 'Canonical (Ubuntu)', 'vendor', '["software-vendor", "vendor"]', 'sales@canonical.com', 'https://canonical.com', 'ACTIVE');

-- =====================================================
-- DATACENTERS DATA
-- =====================================================
INSERT INTO datacenters (name, country_id, state_id, city_id, type, status, provider, partner_id, latitude, longitude, servers, storage_tb, utilization) VALUES

-- AWS Data Centers
('AWS US East (N. Virginia)', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'VA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'Virginia Beach'), 'cloud', 'ACTIVE', 'AWS', (SELECT id FROM partners WHERE name = 'amazon'), 36.8529, -75.9780, 150000, 5000000, 75),
('AWS US West (Oregon)', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'OR' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'Portland'), 'cloud', 'ACTIVE', 'AWS', (SELECT id FROM partners WHERE name = 'amazon'), 45.5152, -122.6784, 120000, 4000000, 68),
('AWS Europe (Ireland)', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'NIR' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), NULL, 'cloud', 'ACTIVE', 'AWS', (SELECT id FROM partners WHERE name = 'amazon'), 53.4129, -8.2439, 80000, 3000000, 72),
('AWS Asia Pacific (Singapore)', (SELECT id FROM countries WHERE code = 'SG'), NULL, (SELECT id FROM cities WHERE name = 'Singapore'), 'cloud', 'ACTIVE', 'AWS', (SELECT id FROM partners WHERE name = 'amazon'), 1.3521, 103.8198, 60000, 2500000, 70),
('AWS Asia Pacific (Tokyo)', (SELECT id FROM countries WHERE code = 'JP'), NULL, (SELECT id FROM cities WHERE name = 'Tokyo'), 'cloud', 'ACTIVE', 'AWS', (SELECT id FROM partners WHERE name = 'amazon'), 35.6762, 139.6503, 75000, 2800000, 73),

-- Google Cloud Data Centers
('GCP US Central', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'IL' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'Chicago'), 'cloud', 'ACTIVE', 'Google Cloud', (SELECT id FROM partners WHERE name = 'google'), 41.8781, -87.6298, 100000, 3500000, 69),
('GCP Europe West', (SELECT id FROM countries WHERE code = 'NL'), NULL, (SELECT id FROM cities WHERE name = 'Amsterdam'), 'cloud', 'ACTIVE', 'Google Cloud', (SELECT id FROM partners WHERE name = 'google'), 52.3676, 4.9041, 70000, 2800000, 71),
('GCP Asia Southeast', (SELECT id FROM countries WHERE code = 'SG'), NULL, (SELECT id FROM cities WHERE name = 'Singapore'), 'cloud', 'ACTIVE', 'Google Cloud', (SELECT id FROM partners WHERE name = 'google'), 1.3521, 103.8198, 55000, 2200000, 68),

-- Microsoft Azure Data Centers
('Azure East US', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'VA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'Virginia Beach'), 'cloud', 'ACTIVE', 'Azure', (SELECT id FROM partners WHERE name = 'microsoft'), 36.8529, -75.9780, 90000, 3200000, 74),
('Azure West Europe', (SELECT id FROM countries WHERE code = 'NL'), NULL, (SELECT id FROM cities WHERE name = 'Amsterdam'), 'cloud', 'ACTIVE', 'Azure', (SELECT id FROM partners WHERE name = 'microsoft'), 52.3676, 4.9041, 65000, 2600000, 70),
('Azure Southeast Asia', (SELECT id FROM countries WHERE code = 'SG'), NULL, (SELECT id FROM cities WHERE name = 'Singapore'), 'cloud', 'ACTIVE', 'Azure', (SELECT id FROM partners WHERE name = 'microsoft'), 1.3521, 103.8198, 50000, 2100000, 67),

-- IBM Cloud Data Centers
('IBM Cloud Dallas', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'Dallas'), 'cloud', 'ACTIVE', 'IBM Cloud', (SELECT id FROM partners WHERE name = 'ibm'), 32.7767, -96.7970, 35000, 1500000, 65),
('IBM Cloud Frankfurt', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), (SELECT id FROM cities WHERE name = 'Frankfurt'), 'cloud', 'ACTIVE', 'IBM Cloud', (SELECT id FROM partners WHERE name = 'ibm'), 50.1109, 8.6821, 30000, 1200000, 62),

-- DigitalOcean Data Centers
('DigitalOcean NYC1', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'NY' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'New York'), 'cloud', 'ACTIVE', 'DigitalOcean', (SELECT id FROM partners WHERE name = 'digitalocean'), 40.7128, -74.0060, 25000, 800000, 60),
('DigitalOcean SFO3', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'San Francisco'), 'cloud', 'ACTIVE', 'DigitalOcean', (SELECT id FROM partners WHERE name = 'digitalocean'), 37.7749, -122.4194, 22000, 750000, 58),
('DigitalOcean AMS3', (SELECT id FROM countries WHERE code = 'NL'), NULL, (SELECT id FROM cities WHERE name = 'Amsterdam'), 'cloud', 'ACTIVE', 'DigitalOcean', (SELECT id FROM partners WHERE name = 'digitalocean'), 52.3676, 4.9041, 18000, 600000, 55),

-- Linode Data Centers
('Linode Fremont', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'San Jose'), 'cloud', 'ACTIVE', 'Linode', (SELECT id FROM partners WHERE name = 'linode'), 37.3382, -121.8863, 15000, 500000, 52),
('Linode London', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), (SELECT id FROM cities WHERE name = 'London'), 'cloud', 'ACTIVE', 'Linode', (SELECT id FROM partners WHERE name = 'linode'), 51.5074, -0.1278, 12000, 450000, 50),

-- Hetzner Data Centers
('Hetzner Falkenstein', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BY' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), NULL, 'cloud', 'ACTIVE', 'Hetzner', (SELECT id FROM partners WHERE name = 'hetzner'), 50.4783, 12.3705, 20000, 800000, 45),
('Hetzner Helsinki', (SELECT id FROM countries WHERE code = 'FI'), NULL, (SELECT id FROM cities WHERE name = 'Helsinki'), 'cloud', 'ACTIVE', 'Hetzner', (SELECT id FROM partners WHERE name = 'hetzner'), 60.1699, 24.9384, 18000, 700000, 42),

-- OVHcloud Data Centers  
('OVHcloud Gravelines', (SELECT id FROM countries WHERE code = 'FR'), NULL, NULL, 'cloud', 'ACTIVE', 'OVHcloud', (SELECT id FROM partners WHERE name = 'ovhcloud'), 50.9867, 2.1258, 25000, 900000, 48),
('OVHcloud Beauharnois', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'QC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), NULL, 'cloud', 'ACTIVE', 'OVHcloud', (SELECT id FROM partners WHERE name = 'ovhcloud'), 45.3133, -73.8772, 22000, 800000, 46),

-- Vultr Data Centers
('Vultr New Jersey', (SELECT id FROM countries WHERE code = 'US'), NULL, NULL, 'cloud', 'ACTIVE', 'Vultr', (SELECT id FROM partners WHERE name = 'vultr'), 40.0583, -74.4057, 12000, 400000, 40),
('Vultr London', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), (SELECT id FROM cities WHERE name = 'London'), 'cloud', 'ACTIVE', 'Vultr', (SELECT id FROM partners WHERE name = 'vultr'), 51.5074, -0.1278, 10000, 350000, 38),

-- Enterprise Data Centers
('Enterprise DC New York', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'NY' AND country_id = (SELECT id FROM countries WHERE code = 'US')), (SELECT id FROM cities WHERE name = 'New York'), 'enterprise', 'ACTIVE', 'Private', NULL, 40.7128, -74.0060, 500, 50000, 30),
('Enterprise DC London', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), (SELECT id FROM cities WHERE name = 'London'), 'enterprise', 'ACTIVE', 'Private', NULL, 51.5074, -0.1278, 300, 35000, 25),
('Enterprise DC Frankfurt', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), (SELECT id FROM cities WHERE name = 'Frankfurt'), 'enterprise', 'ACTIVE', 'Private', NULL, 50.1109, 8.6821, 400, 40000, 28);

-- =====================================================
-- COMPLETION MESSAGE
-- =====================================================
SELECT 'Database seed data populated successfully!' as message,
       (SELECT COUNT(*) FROM countries) as countries_count,
       (SELECT COUNT(*) FROM states) as states_count, 
       (SELECT COUNT(*) FROM cities) as cities_count,
       (SELECT COUNT(*) FROM partners) as partners_count,
       (SELECT COUNT(*) FROM datacenters) as datacenters_count;