USE orchestrix;

-- Add more countries
INSERT IGNORE INTO countries (code, code3, name, region, has_states) VALUES
('DE', 'DEU', 'Germany', 'Europe', TRUE),
('FR', 'FRA', 'France', 'Europe', FALSE),
('IN', 'IND', 'India', 'Asia', TRUE),
('CN', 'CHN', 'China', 'Asia', TRUE),
('JP', 'JPN', 'Japan', 'Asia', TRUE),
('AU', 'AUS', 'Australia', 'Oceania', TRUE),
('BR', 'BRA', 'Brazil', 'South America', TRUE),
('SG', 'SGP', 'Singapore', 'Asia', FALSE),
('AE', 'ARE', 'United Arab Emirates', 'Middle East', TRUE),
('NL', 'NLD', 'Netherlands', 'Europe', FALSE),
('IE', 'IRL', 'Ireland', 'Europe', FALSE),
('KR', 'KOR', 'South Korea', 'Asia', FALSE);

-- Add US states
INSERT IGNORE INTO states (country_id, code, name) 
SELECT id, 'CA', 'California' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'TX', 'Texas' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'NY', 'New York' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'FL', 'Florida' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'VA', 'Virginia' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'WA', 'Washington' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'OR', 'Oregon' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'CO', 'Colorado' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'IL', 'Illinois' FROM countries WHERE code = 'US';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'GA', 'Georgia' FROM countries WHERE code = 'US';

-- Add Canadian provinces
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'ON', 'Ontario' FROM countries WHERE code = 'CA';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'QC', 'Quebec' FROM countries WHERE code = 'CA';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'BC', 'British Columbia' FROM countries WHERE code = 'CA';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'AB', 'Alberta' FROM countries WHERE code = 'CA';

-- Add German states
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'BY', 'Bavaria' FROM countries WHERE code = 'DE';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'HE', 'Hesse' FROM countries WHERE code = 'DE';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'NW', 'North Rhine-Westphalia' FROM countries WHERE code = 'DE';

-- Add Indian states
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'MH', 'Maharashtra' FROM countries WHERE code = 'IN';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'KA', 'Karnataka' FROM countries WHERE code = 'IN';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'DL', 'Delhi' FROM countries WHERE code = 'IN';
INSERT IGNORE INTO states (country_id, code, name)
SELECT id, 'TG', 'Telangana' FROM countries WHERE code = 'IN';

-- Add cities for US
INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'San Francisco', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'CA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Los Angeles', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'CA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'San Jose', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'CA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'New York City', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'NY';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Dallas', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'TX';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Austin', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'TX';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Houston', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'TX';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Miami', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'FL';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Ashburn', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'VA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Seattle', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'WA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Portland', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'OR';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Denver', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'CO';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Chicago', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'IL';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Atlanta', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'US' AND s.code = 'GA';

-- Add cities for Canada
INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Toronto', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'CA' AND s.code = 'ON';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Montreal', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'CA' AND s.code = 'QC';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Vancouver', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'CA' AND s.code = 'BC';

-- Add cities without states
INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'London', TRUE FROM countries WHERE code = 'GB';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Paris', TRUE FROM countries WHERE code = 'FR';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Singapore', TRUE FROM countries WHERE code = 'SG';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Amsterdam', TRUE FROM countries WHERE code = 'NL';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Dublin', TRUE FROM countries WHERE code = 'IE';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Seoul', TRUE FROM countries WHERE code = 'KR';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Tokyo', TRUE FROM countries WHERE code = 'JP';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Osaka', FALSE FROM countries WHERE code = 'JP';

-- Add cities with states for Germany
INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Frankfurt', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'DE' AND s.code = 'HE';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Munich', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'DE' AND s.code = 'BY';

-- Add cities with states for India
INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Mumbai', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'IN' AND s.code = 'MH';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Bangalore', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'IN' AND s.code = 'KA';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'New Delhi', TRUE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'IN' AND s.code = 'DL';

INSERT IGNORE INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Hyderabad', FALSE
FROM countries c JOIN states s ON c.id = s.country_id 
WHERE c.code = 'IN' AND s.code = 'TG';

-- Add cities for UAE
INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Dubai', FALSE FROM countries WHERE code = 'AE';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Abu Dhabi', TRUE FROM countries WHERE code = 'AE';

-- Add cities for Australia
INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Sydney', FALSE FROM countries WHERE code = 'AU';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Melbourne', FALSE FROM countries WHERE code = 'AU';

-- Add cities for Brazil
INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'São Paulo', FALSE FROM countries WHERE code = 'BR';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Rio de Janeiro', FALSE FROM countries WHERE code = 'BR';

-- Add cities for China
INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Beijing', TRUE FROM countries WHERE code = 'CN';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Shanghai', FALSE FROM countries WHERE code = 'CN';

INSERT IGNORE INTO cities (country_id, name, is_capital)
SELECT id, 'Shenzhen', FALSE FROM countries WHERE code = 'CN';