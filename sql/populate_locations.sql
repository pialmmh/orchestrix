USE orchestrix;

-- Populate countries
INSERT INTO countries (code, code3, name, region, has_states) VALUES
('US', 'USA', 'United States', 'North America', TRUE),
('CA', 'CAN', 'Canada', 'North America', TRUE),
('MX', 'MEX', 'Mexico', 'North America', TRUE),
('BR', 'BRA', 'Brazil', 'South America', TRUE),
('AR', 'ARG', 'Argentina', 'South America', TRUE),
('GB', 'GBR', 'United Kingdom', 'Europe', FALSE),
('DE', 'DEU', 'Germany', 'Europe', TRUE),
('FR', 'FRA', 'France', 'Europe', FALSE),
('IT', 'ITA', 'Italy', 'Europe', FALSE),
('ES', 'ESP', 'Spain', 'Europe', FALSE),
('NL', 'NLD', 'Netherlands', 'Europe', FALSE),
('SE', 'SWE', 'Sweden', 'Europe', FALSE),
('NO', 'NOR', 'Norway', 'Europe', FALSE),
('FI', 'FIN', 'Finland', 'Europe', FALSE),
('DK', 'DNK', 'Denmark', 'Europe', FALSE),
('CH', 'CHE', 'Switzerland', 'Europe', FALSE),
('AT', 'AUT', 'Austria', 'Europe', FALSE),
('BE', 'BEL', 'Belgium', 'Europe', FALSE),
('PL', 'POL', 'Poland', 'Europe', FALSE),
('CZ', 'CZE', 'Czech Republic', 'Europe', FALSE),
('RU', 'RUS', 'Russia', 'Europe/Asia', TRUE),
('CN', 'CHN', 'China', 'Asia', TRUE),
('JP', 'JPN', 'Japan', 'Asia', TRUE),
('KR', 'KOR', 'South Korea', 'Asia', FALSE),
('IN', 'IND', 'India', 'Asia', TRUE),
('SG', 'SGP', 'Singapore', 'Asia', FALSE),
('MY', 'MYS', 'Malaysia', 'Asia', TRUE),
('TH', 'THA', 'Thailand', 'Asia', FALSE),
('ID', 'IDN', 'Indonesia', 'Asia', TRUE),
('PH', 'PHL', 'Philippines', 'Asia', FALSE),
('VN', 'VNM', 'Vietnam', 'Asia', FALSE),
('AU', 'AUS', 'Australia', 'Oceania', TRUE),
('NZ', 'NZL', 'New Zealand', 'Oceania', FALSE),
('ZA', 'ZAF', 'South Africa', 'Africa', TRUE),
('EG', 'EGY', 'Egypt', 'Africa', FALSE),
('NG', 'NGA', 'Nigeria', 'Africa', TRUE),
('KE', 'KEN', 'Kenya', 'Africa', FALSE),
('IL', 'ISR', 'Israel', 'Middle East', FALSE),
('AE', 'ARE', 'United Arab Emirates', 'Middle East', TRUE),
('SA', 'SAU', 'Saudi Arabia', 'Middle East', FALSE),
('TR', 'TUR', 'Turkey', 'Europe/Asia', FALSE),
('IE', 'IRL', 'Ireland', 'Europe', FALSE),
('PT', 'PRT', 'Portugal', 'Europe', FALSE),
('GR', 'GRC', 'Greece', 'Europe', FALSE),
('HK', 'HKG', 'Hong Kong', 'Asia', FALSE),
('TW', 'TWN', 'Taiwan', 'Asia', FALSE),
('CL', 'CHL', 'Chile', 'South America', FALSE),
('CO', 'COL', 'Colombia', 'South America', FALSE),
('PE', 'PER', 'Peru', 'South America', FALSE);

-- Populate US states
INSERT INTO states (country_id, code, name) 
SELECT id, 'CA', 'California' FROM countries WHERE code = 'US'
UNION SELECT id, 'TX', 'Texas' FROM countries WHERE code = 'US'
UNION SELECT id, 'FL', 'Florida' FROM countries WHERE code = 'US'
UNION SELECT id, 'NY', 'New York' FROM countries WHERE code = 'US'
UNION SELECT id, 'PA', 'Pennsylvania' FROM countries WHERE code = 'US'
UNION SELECT id, 'IL', 'Illinois' FROM countries WHERE code = 'US'
UNION SELECT id, 'OH', 'Ohio' FROM countries WHERE code = 'US'
UNION SELECT id, 'GA', 'Georgia' FROM countries WHERE code = 'US'
UNION SELECT id, 'NC', 'North Carolina' FROM countries WHERE code = 'US'
UNION SELECT id, 'MI', 'Michigan' FROM countries WHERE code = 'US'
UNION SELECT id, 'NJ', 'New Jersey' FROM countries WHERE code = 'US'
UNION SELECT id, 'VA', 'Virginia' FROM countries WHERE code = 'US'
UNION SELECT id, 'WA', 'Washington' FROM countries WHERE code = 'US'
UNION SELECT id, 'AZ', 'Arizona' FROM countries WHERE code = 'US'
UNION SELECT id, 'MA', 'Massachusetts' FROM countries WHERE code = 'US'
UNION SELECT id, 'TN', 'Tennessee' FROM countries WHERE code = 'US'
UNION SELECT id, 'IN', 'Indiana' FROM countries WHERE code = 'US'
UNION SELECT id, 'MO', 'Missouri' FROM countries WHERE code = 'US'
UNION SELECT id, 'MD', 'Maryland' FROM countries WHERE code = 'US'
UNION SELECT id, 'WI', 'Wisconsin' FROM countries WHERE code = 'US'
UNION SELECT id, 'CO', 'Colorado' FROM countries WHERE code = 'US'
UNION SELECT id, 'MN', 'Minnesota' FROM countries WHERE code = 'US'
UNION SELECT id, 'SC', 'South Carolina' FROM countries WHERE code = 'US'
UNION SELECT id, 'AL', 'Alabama' FROM countries WHERE code = 'US'
UNION SELECT id, 'LA', 'Louisiana' FROM countries WHERE code = 'US'
UNION SELECT id, 'KY', 'Kentucky' FROM countries WHERE code = 'US'
UNION SELECT id, 'OR', 'Oregon' FROM countries WHERE code = 'US'
UNION SELECT id, 'OK', 'Oklahoma' FROM countries WHERE code = 'US'
UNION SELECT id, 'CT', 'Connecticut' FROM countries WHERE code = 'US'
UNION SELECT id, 'UT', 'Utah' FROM countries WHERE code = 'US'
UNION SELECT id, 'IA', 'Iowa' FROM countries WHERE code = 'US'
UNION SELECT id, 'NV', 'Nevada' FROM countries WHERE code = 'US'
UNION SELECT id, 'AR', 'Arkansas' FROM countries WHERE code = 'US'
UNION SELECT id, 'MS', 'Mississippi' FROM countries WHERE code = 'US'
UNION SELECT id, 'KS', 'Kansas' FROM countries WHERE code = 'US'
UNION SELECT id, 'NM', 'New Mexico' FROM countries WHERE code = 'US'
UNION SELECT id, 'NE', 'Nebraska' FROM countries WHERE code = 'US'
UNION SELECT id, 'ID', 'Idaho' FROM countries WHERE code = 'US'
UNION SELECT id, 'WV', 'West Virginia' FROM countries WHERE code = 'US'
UNION SELECT id, 'HI', 'Hawaii' FROM countries WHERE code = 'US'
UNION SELECT id, 'NH', 'New Hampshire' FROM countries WHERE code = 'US'
UNION SELECT id, 'ME', 'Maine' FROM countries WHERE code = 'US'
UNION SELECT id, 'RI', 'Rhode Island' FROM countries WHERE code = 'US'
UNION SELECT id, 'MT', 'Montana' FROM countries WHERE code = 'US'
UNION SELECT id, 'DE', 'Delaware' FROM countries WHERE code = 'US'
UNION SELECT id, 'SD', 'South Dakota' FROM countries WHERE code = 'US'
UNION SELECT id, 'ND', 'North Dakota' FROM countries WHERE code = 'US'
UNION SELECT id, 'AK', 'Alaska' FROM countries WHERE code = 'US'
UNION SELECT id, 'VT', 'Vermont' FROM countries WHERE code = 'US'
UNION SELECT id, 'WY', 'Wyoming' FROM countries WHERE code = 'US';

-- Populate Canadian provinces
INSERT INTO states (country_id, code, name)
SELECT id, 'ON', 'Ontario' FROM countries WHERE code = 'CA'
UNION SELECT id, 'QC', 'Quebec' FROM countries WHERE code = 'CA'
UNION SELECT id, 'BC', 'British Columbia' FROM countries WHERE code = 'CA'
UNION SELECT id, 'AB', 'Alberta' FROM countries WHERE code = 'CA'
UNION SELECT id, 'MB', 'Manitoba' FROM countries WHERE code = 'CA'
UNION SELECT id, 'SK', 'Saskatchewan' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NS', 'Nova Scotia' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NB', 'New Brunswick' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NL', 'Newfoundland and Labrador' FROM countries WHERE code = 'CA'
UNION SELECT id, 'PE', 'Prince Edward Island' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NT', 'Northwest Territories' FROM countries WHERE code = 'CA'
UNION SELECT id, 'YT', 'Yukon' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NU', 'Nunavut' FROM countries WHERE code = 'CA';

-- Populate German states
INSERT INTO states (country_id, code, name)
SELECT id, 'BW', 'Baden-Württemberg' FROM countries WHERE code = 'DE'
UNION SELECT id, 'BY', 'Bavaria' FROM countries WHERE code = 'DE'
UNION SELECT id, 'BE', 'Berlin' FROM countries WHERE code = 'DE'
UNION SELECT id, 'BB', 'Brandenburg' FROM countries WHERE code = 'DE'
UNION SELECT id, 'HB', 'Bremen' FROM countries WHERE code = 'DE'
UNION SELECT id, 'HH', 'Hamburg' FROM countries WHERE code = 'DE'
UNION SELECT id, 'HE', 'Hesse' FROM countries WHERE code = 'DE'
UNION SELECT id, 'NI', 'Lower Saxony' FROM countries WHERE code = 'DE'
UNION SELECT id, 'MV', 'Mecklenburg-Vorpommern' FROM countries WHERE code = 'DE'
UNION SELECT id, 'NW', 'North Rhine-Westphalia' FROM countries WHERE code = 'DE'
UNION SELECT id, 'RP', 'Rhineland-Palatinate' FROM countries WHERE code = 'DE'
UNION SELECT id, 'SL', 'Saarland' FROM countries WHERE code = 'DE'
UNION SELECT id, 'SN', 'Saxony' FROM countries WHERE code = 'DE'
UNION SELECT id, 'ST', 'Saxony-Anhalt' FROM countries WHERE code = 'DE'
UNION SELECT id, 'SH', 'Schleswig-Holstein' FROM countries WHERE code = 'DE'
UNION SELECT id, 'TH', 'Thuringia' FROM countries WHERE code = 'DE';

-- Populate Indian states (major ones)
INSERT INTO states (country_id, code, name)
SELECT id, 'MH', 'Maharashtra' FROM countries WHERE code = 'IN'
UNION SELECT id, 'KA', 'Karnataka' FROM countries WHERE code = 'IN'
UNION SELECT id, 'TN', 'Tamil Nadu' FROM countries WHERE code = 'IN'
UNION SELECT id, 'DL', 'Delhi' FROM countries WHERE code = 'IN'
UNION SELECT id, 'UP', 'Uttar Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'WB', 'West Bengal' FROM countries WHERE code = 'IN'
UNION SELECT id, 'GJ', 'Gujarat' FROM countries WHERE code = 'IN'
UNION SELECT id, 'RJ', 'Rajasthan' FROM countries WHERE code = 'IN'
UNION SELECT id, 'AP', 'Andhra Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'TG', 'Telangana' FROM countries WHERE code = 'IN'
UNION SELECT id, 'KL', 'Kerala' FROM countries WHERE code = 'IN'
UNION SELECT id, 'PB', 'Punjab' FROM countries WHERE code = 'IN'
UNION SELECT id, 'HR', 'Haryana' FROM countries WHERE code = 'IN'
UNION SELECT id, 'MP', 'Madhya Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'BR', 'Bihar' FROM countries WHERE code = 'IN';

-- Populate Australian states
INSERT INTO states (country_id, code, name)
SELECT id, 'NSW', 'New South Wales' FROM countries WHERE code = 'AU'
UNION SELECT id, 'VIC', 'Victoria' FROM countries WHERE code = 'AU'
UNION SELECT id, 'QLD', 'Queensland' FROM countries WHERE code = 'AU'
UNION SELECT id, 'WA', 'Western Australia' FROM countries WHERE code = 'AU'
UNION SELECT id, 'SA', 'South Australia' FROM countries WHERE code = 'AU'
UNION SELECT id, 'TAS', 'Tasmania' FROM countries WHERE code = 'AU'
UNION SELECT id, 'ACT', 'Australian Capital Territory' FROM countries WHERE code = 'AU'
UNION SELECT id, 'NT', 'Northern Territory' FROM countries WHERE code = 'AU';

-- Populate Brazilian states (major ones)
INSERT INTO states (country_id, code, name)
SELECT id, 'SP', 'São Paulo' FROM countries WHERE code = 'BR'
UNION SELECT id, 'RJ', 'Rio de Janeiro' FROM countries WHERE code = 'BR'
UNION SELECT id, 'MG', 'Minas Gerais' FROM countries WHERE code = 'BR'
UNION SELECT id, 'RS', 'Rio Grande do Sul' FROM countries WHERE code = 'BR'
UNION SELECT id, 'PR', 'Paraná' FROM countries WHERE code = 'BR'
UNION SELECT id, 'SC', 'Santa Catarina' FROM countries WHERE code = 'BR'
UNION SELECT id, 'BA', 'Bahia' FROM countries WHERE code = 'BR'
UNION SELECT id, 'DF', 'Distrito Federal' FROM countries WHERE code = 'BR'
UNION SELECT id, 'GO', 'Goiás' FROM countries WHERE code = 'BR'
UNION SELECT id, 'PE', 'Pernambuco' FROM countries WHERE code = 'BR';

-- Populate major cities
-- US Cities
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'New York City', FALSE, 40.7128, -74.0060
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'NY'
UNION SELECT c.id, s.id, 'Los Angeles', FALSE, 34.0522, -118.2437
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA'
UNION SELECT c.id, s.id, 'San Francisco', FALSE, 37.7749, -122.4194
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA'
UNION SELECT c.id, s.id, 'San Jose', FALSE, 37.3382, -121.8863
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA'
UNION SELECT c.id, s.id, 'Chicago', FALSE, 41.8781, -87.6298
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'IL'
UNION SELECT c.id, s.id, 'Houston', FALSE, 29.7604, -95.3698
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX'
UNION SELECT c.id, s.id, 'Dallas', FALSE, 32.7767, -96.7970
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX'
UNION SELECT c.id, s.id, 'Austin', FALSE, 30.2672, -97.7431
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX'
UNION SELECT c.id, s.id, 'Phoenix', FALSE, 33.4484, -112.0740
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'AZ'
UNION SELECT c.id, s.id, 'Philadelphia', FALSE, 39.9526, -75.1652
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'PA'
UNION SELECT c.id, s.id, 'Miami', FALSE, 25.7617, -80.1918
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'FL'
UNION SELECT c.id, s.id, 'Atlanta', FALSE, 33.7490, -84.3880
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'GA'
UNION SELECT c.id, s.id, 'Washington', TRUE, 38.9072, -77.0369
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'VA'
UNION SELECT c.id, s.id, 'Boston', FALSE, 42.3601, -71.0589
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'MA'
UNION SELECT c.id, s.id, 'Seattle', FALSE, 47.6062, -122.3321
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'WA'
UNION SELECT c.id, s.id, 'Denver', FALSE, 39.7392, -104.9903
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CO'
UNION SELECT c.id, s.id, 'Las Vegas', FALSE, 36.1699, -115.1398
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'NV'
UNION SELECT c.id, s.id, 'Portland', FALSE, 45.5152, -122.6784
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'OR'
UNION SELECT c.id, s.id, 'San Diego', FALSE, 32.7157, -117.1611
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA'
UNION SELECT c.id, s.id, 'Ashburn', FALSE, 39.0438, -77.4874
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'VA';

-- Canada Cities
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'Toronto', FALSE, 43.6532, -79.3832
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'ON'
UNION SELECT c.id, s.id, 'Montreal', FALSE, 45.5017, -73.5673
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'QC'
UNION SELECT c.id, s.id, 'Vancouver', FALSE, 49.2827, -123.1207
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'BC'
UNION SELECT c.id, s.id, 'Calgary', FALSE, 51.0447, -114.0719
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'AB'
UNION SELECT c.id, s.id, 'Ottawa', TRUE, 45.4215, -75.6972
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'ON';

-- European Cities (without states)
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'London', TRUE, 51.5074, -0.1278 FROM countries WHERE code = 'GB'
UNION SELECT id, 'Manchester', FALSE, 53.4808, -2.2426 FROM countries WHERE code = 'GB'
UNION SELECT id, 'Birmingham', FALSE, 52.4862, -1.8904 FROM countries WHERE code = 'GB'
UNION SELECT id, 'Paris', TRUE, 48.8566, 2.3522 FROM countries WHERE code = 'FR'
UNION SELECT id, 'Marseille', FALSE, 43.2965, 5.3698 FROM countries WHERE code = 'FR'
UNION SELECT id, 'Amsterdam', TRUE, 52.3676, 4.9041 FROM countries WHERE code = 'NL'
UNION SELECT id, 'Stockholm', TRUE, 59.3293, 18.0686 FROM countries WHERE code = 'SE'
UNION SELECT id, 'Copenhagen', TRUE, 55.6761, 12.5683 FROM countries WHERE code = 'DK'
UNION SELECT id, 'Oslo', TRUE, 59.9139, 10.7522 FROM countries WHERE code = 'NO'
UNION SELECT id, 'Helsinki', TRUE, 60.1699, 24.9384 FROM countries WHERE code = 'FI'
UNION SELECT id, 'Dublin', TRUE, 53.3498, -6.2603 FROM countries WHERE code = 'IE'
UNION SELECT id, 'Madrid', TRUE, 40.4168, -3.7038 FROM countries WHERE code = 'ES'
UNION SELECT id, 'Barcelona', FALSE, 41.3851, 2.1734 FROM countries WHERE code = 'ES'
UNION SELECT id, 'Milan', FALSE, 45.4642, 9.1900 FROM countries WHERE code = 'IT'
UNION SELECT id, 'Rome', TRUE, 41.9028, 12.4964 FROM countries WHERE code = 'IT'
UNION SELECT id, 'Zurich', FALSE, 47.3769, 8.5417 FROM countries WHERE code = 'CH'
UNION SELECT id, 'Geneva', FALSE, 46.2044, 6.1432 FROM countries WHERE code = 'CH'
UNION SELECT id, 'Vienna', TRUE, 48.2082, 16.3738 FROM countries WHERE code = 'AT'
UNION SELECT id, 'Brussels', TRUE, 50.8503, 4.3517 FROM countries WHERE code = 'BE'
UNION SELECT id, 'Warsaw', TRUE, 52.2297, 21.0122 FROM countries WHERE code = 'PL'
UNION SELECT id, 'Prague', TRUE, 50.0755, 14.4378 FROM countries WHERE code = 'CZ'
UNION SELECT id, 'Lisbon', TRUE, 38.7223, -9.1393 FROM countries WHERE code = 'PT'
UNION SELECT id, 'Athens', TRUE, 37.9838, 23.7275 FROM countries WHERE code = 'GR';

-- German Cities (with states)
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'Frankfurt', FALSE, 50.1109, 8.6821
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'DE' AND s.code = 'HE'
UNION SELECT c.id, s.id, 'Munich', FALSE, 48.1351, 11.5820
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'DE' AND s.code = 'BY'
UNION SELECT c.id, s.id, 'Berlin', TRUE, 52.5200, 13.4050
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'DE' AND s.code = 'BE'
UNION SELECT c.id, s.id, 'Hamburg', FALSE, 53.5511, 9.9937
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'DE' AND s.code = 'HH'
UNION SELECT c.id, s.id, 'Cologne', FALSE, 50.9375, 6.9603
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'DE' AND s.code = 'NW';

-- Asian Cities
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Singapore', TRUE, 1.3521, 103.8198 FROM countries WHERE code = 'SG'
UNION SELECT id, 'Hong Kong', FALSE, 22.3193, 114.1694 FROM countries WHERE code = 'HK'
UNION SELECT id, 'Tokyo', TRUE, 35.6762, 139.6503 FROM countries WHERE code = 'JP'
UNION SELECT id, 'Osaka', FALSE, 34.6937, 135.5023 FROM countries WHERE code = 'JP'
UNION SELECT id, 'Seoul', TRUE, 37.5665, 126.9780 FROM countries WHERE code = 'KR'
UNION SELECT id, 'Bangkok', TRUE, 13.7563, 100.5018 FROM countries WHERE code = 'TH'
UNION SELECT id, 'Kuala Lumpur', TRUE, 3.1390, 101.6869 FROM countries WHERE code = 'MY'
UNION SELECT id, 'Jakarta', TRUE, -6.2088, 106.8456 FROM countries WHERE code = 'ID'
UNION SELECT id, 'Manila', TRUE, 14.5995, 120.9842 FROM countries WHERE code = 'PH'
UNION SELECT id, 'Ho Chi Minh City', FALSE, 10.8231, 106.6297 FROM countries WHERE code = 'VN'
UNION SELECT id, 'Hanoi', TRUE, 21.0285, 105.8542 FROM countries WHERE code = 'VN'
UNION SELECT id, 'Taipei', TRUE, 25.0330, 121.5654 FROM countries WHERE code = 'TW';

-- Chinese Cities (with states/provinces)
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Beijing', TRUE, 39.9042, 116.4074 FROM countries WHERE code = 'CN'
UNION SELECT id, 'Shanghai', FALSE, 31.2304, 121.4737 FROM countries WHERE code = 'CN'
UNION SELECT id, 'Shenzhen', FALSE, 22.5431, 114.0579 FROM countries WHERE code = 'CN'
UNION SELECT id, 'Guangzhou', FALSE, 23.1291, 113.2644 FROM countries WHERE code = 'CN'
UNION SELECT id, 'Chengdu', FALSE, 30.5728, 104.0668 FROM countries WHERE code = 'CN';

-- Indian Cities (with states)
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'Mumbai', FALSE, 19.0760, 72.8777
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'MH'
UNION SELECT c.id, s.id, 'Bangalore', FALSE, 12.9716, 77.5946
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'KA'
UNION SELECT c.id, s.id, 'Chennai', FALSE, 13.0827, 80.2707
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'TN'
UNION SELECT c.id, s.id, 'New Delhi', TRUE, 28.6139, 77.2090
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'DL'
UNION SELECT c.id, s.id, 'Hyderabad', FALSE, 17.3850, 78.4867
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'TG'
UNION SELECT c.id, s.id, 'Pune', FALSE, 18.5204, 73.8567
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'MH'
UNION SELECT c.id, s.id, 'Kolkata', FALSE, 22.5726, 88.3639
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'WB';

-- Australian Cities
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'Sydney', FALSE, -33.8688, 151.2093
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'NSW'
UNION SELECT c.id, s.id, 'Melbourne', FALSE, -37.8136, 144.9631
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'VIC'
UNION SELECT c.id, s.id, 'Brisbane', FALSE, -27.4698, 153.0251
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'QLD'
UNION SELECT c.id, s.id, 'Perth', FALSE, -31.9505, 115.8605
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'WA'
UNION SELECT c.id, s.id, 'Canberra', TRUE, -35.2809, 149.1300
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'ACT';

-- South American Cities
INSERT INTO cities (country_id, state_id, name, is_capital, latitude, longitude)
SELECT c.id, s.id, 'São Paulo', FALSE, -23.5505, -46.6333
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'BR' AND s.code = 'SP'
UNION SELECT c.id, s.id, 'Rio de Janeiro', FALSE, -22.9068, -43.1729
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'BR' AND s.code = 'RJ'
UNION SELECT c.id, s.id, 'Brasília', TRUE, -15.8267, -47.9218
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'BR' AND s.code = 'DF';

INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Buenos Aires', TRUE, -34.6037, -58.3816 FROM countries WHERE code = 'AR'
UNION SELECT id, 'Santiago', TRUE, -33.4489, -70.6693 FROM countries WHERE code = 'CL'
UNION SELECT id, 'Bogotá', TRUE, 4.7110, -74.0721 FROM countries WHERE code = 'CO'
UNION SELECT id, 'Lima', TRUE, -12.0464, -77.0428 FROM countries WHERE code = 'PE'
UNION SELECT id, 'Mexico City', TRUE, 19.4326, -99.1332 FROM countries WHERE code = 'MX';

-- Middle East Cities
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Dubai', FALSE, 25.2048, 55.2708 FROM countries WHERE code = 'AE'
UNION SELECT id, 'Abu Dhabi', TRUE, 24.4539, 54.3773 FROM countries WHERE code = 'AE'
UNION SELECT id, 'Tel Aviv', FALSE, 32.0853, 34.7818 FROM countries WHERE code = 'IL'
UNION SELECT id, 'Jerusalem', TRUE, 31.7683, 35.2137 FROM countries WHERE code = 'IL'
UNION SELECT id, 'Riyadh', TRUE, 24.7136, 46.6753 FROM countries WHERE code = 'SA'
UNION SELECT id, 'Istanbul', FALSE, 41.0082, 28.9784 FROM countries WHERE code = 'TR'
UNION SELECT id, 'Ankara', TRUE, 39.9334, 32.8597 FROM countries WHERE code = 'TR';

-- African Cities
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Cape Town', FALSE, -33.9249, 18.4241 FROM countries WHERE code = 'ZA'
UNION SELECT id, 'Johannesburg', FALSE, -26.2041, 28.0473 FROM countries WHERE code = 'ZA'
UNION SELECT id, 'Pretoria', TRUE, -25.7479, 28.2293 FROM countries WHERE code = 'ZA'
UNION SELECT id, 'Cairo', TRUE, 30.0444, 31.2357 FROM countries WHERE code = 'EG'
UNION SELECT id, 'Lagos', FALSE, 6.5244, 3.3792 FROM countries WHERE code = 'NG'
UNION SELECT id, 'Abuja', TRUE, 9.0765, 7.3986 FROM countries WHERE code = 'NG'
UNION SELECT id, 'Nairobi', TRUE, -1.2921, 36.8219 FROM countries WHERE code = 'KE';

-- Oceania Cities (additional)
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Auckland', FALSE, -36.8485, 174.7633 FROM countries WHERE code = 'NZ'
UNION SELECT id, 'Wellington', TRUE, -41.2865, 174.7762 FROM countries WHERE code = 'NZ';

-- Russian Cities
INSERT INTO cities (country_id, name, is_capital, latitude, longitude)
SELECT id, 'Moscow', TRUE, 55.7558, 37.6173 FROM countries WHERE code = 'RU'
UNION SELECT id, 'St. Petersburg', FALSE, 59.9311, 30.3609 FROM countries WHERE code = 'RU';