-- Populate states and cities for select countries
-- This is a simplified version with key locations

-- Skip deletion to preserve existing data

-- United States - already populated in original script

-- United Kingdom
INSERT IGNORE INTO states (code, name, country_id) VALUES
('ENG', 'England', (SELECT id FROM countries WHERE code = 'GB')),
('SCT', 'Scotland', (SELECT id FROM countries WHERE code = 'GB')),
('WLS', 'Wales', (SELECT id FROM countries WHERE code = 'GB')),
('NIR', 'Northern Ireland', (SELECT id FROM countries WHERE code = 'GB'));

-- Add cities for all countries (without states where not applicable)
-- For countries without states, state_id will be NULL

-- Major global cities without states
INSERT IGNORE INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
-- Italy
('Rome', (SELECT id FROM countries WHERE code = 'IT'), NULL, 1, 41.9028, 12.4964),
('Milan', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 45.4642, 9.1900),
('Naples', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 40.8518, 14.2681),

-- Spain
('Madrid', (SELECT id FROM countries WHERE code = 'ES'), NULL, 1, 40.4168, -3.7038),
('Barcelona', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 41.3851, 2.1734),
('Valencia', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 39.4699, -0.3763),

-- France (without using states for now)
('Paris', (SELECT id FROM countries WHERE code = 'FR'), NULL, 1, 48.8566, 2.3522),
('Marseille', (SELECT id FROM countries WHERE code = 'FR'), NULL, 0, 43.2965, 5.3698),
('Lyon', (SELECT id FROM countries WHERE code = 'FR'), NULL, 0, 45.7640, 4.8357),

-- Germany (without using states for now)
('Berlin', (SELECT id FROM countries WHERE code = 'DE'), NULL, 1, 52.5200, 13.4050),
('Hamburg', (SELECT id FROM countries WHERE code = 'DE'), NULL, 0, 53.5511, 9.9937),
('Munich', (SELECT id FROM countries WHERE code = 'DE'), NULL, 0, 48.1351, 11.5820),
('Frankfurt', (SELECT id FROM countries WHERE code = 'DE'), NULL, 0, 50.1109, 8.6821),

-- Netherlands
('Amsterdam', (SELECT id FROM countries WHERE code = 'NL'), NULL, 1, 52.3676, 4.9041),
('Rotterdam', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 51.9244, 4.4777),
('The Hague', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 52.0705, 4.3007),

-- Belgium
('Brussels', (SELECT id FROM countries WHERE code = 'BE'), NULL, 1, 50.8503, 4.3517),
('Antwerp', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 51.2194, 4.4025),

-- Switzerland
('Bern', (SELECT id FROM countries WHERE code = 'CH'), NULL, 1, 46.9480, 7.4474),
('Zurich', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.3769, 8.5417),
('Geneva', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 46.2044, 6.1432),

-- Austria
('Vienna', (SELECT id FROM countries WHERE code = 'AT'), NULL, 1, 48.2082, 16.3738),
('Graz', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 47.0707, 15.4395),

-- Poland
('Warsaw', (SELECT id FROM countries WHERE code = 'PL'), NULL, 1, 52.2297, 21.0122),
('Kraków', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 50.0647, 19.9450),

-- Russia
('Moscow', (SELECT id FROM countries WHERE code = 'RU'), NULL, 1, 55.7558, 37.6173),
('Saint Petersburg', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 59.9311, 30.3609),

-- Turkey
('Ankara', (SELECT id FROM countries WHERE code = 'TR'), NULL, 1, 39.9334, 32.8597),
('Istanbul', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 41.0082, 28.9784),

-- Japan
('Tokyo', (SELECT id FROM countries WHERE code = 'JP'), NULL, 1, 35.6762, 139.6503),
('Osaka', (SELECT id FROM countries WHERE code = 'JP'), NULL, 0, 34.6937, 135.5023),
('Yokohama', (SELECT id FROM countries WHERE code = 'JP'), NULL, 0, 35.4437, 139.6380),

-- China
('Beijing', (SELECT id FROM countries WHERE code = 'CN'), NULL, 1, 39.9042, 116.4074),
('Shanghai', (SELECT id FROM countries WHERE code = 'CN'), NULL, 0, 31.2304, 121.4737),
('Guangzhou', (SELECT id FROM countries WHERE code = 'CN'), NULL, 0, 23.1291, 113.2644),
('Shenzhen', (SELECT id FROM countries WHERE code = 'CN'), NULL, 0, 22.5431, 114.0579),

-- South Korea
('Seoul', (SELECT id FROM countries WHERE code = 'KR'), NULL, 1, 37.5665, 126.9780),
('Busan', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 35.1796, 129.0756),

-- India (without using states for now)
('New Delhi', (SELECT id FROM countries WHERE code = 'IN'), NULL, 1, 28.6139, 77.2090),
('Mumbai', (SELECT id FROM countries WHERE code = 'IN'), NULL, 0, 19.0760, 72.8777),
('Bangalore', (SELECT id FROM countries WHERE code = 'IN'), NULL, 0, 12.9716, 77.5946),
('Hyderabad', (SELECT id FROM countries WHERE code = 'IN'), NULL, 0, 17.3850, 78.4867),

-- Thailand
('Bangkok', (SELECT id FROM countries WHERE code = 'TH'), NULL, 1, 13.7563, 100.5018),
('Chiang Mai', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 18.7883, 98.9853),

-- Vietnam
('Hanoi', (SELECT id FROM countries WHERE code = 'VN'), NULL, 1, 21.0285, 105.8542),
('Ho Chi Minh City', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 10.8231, 106.6297),

-- Indonesia
('Jakarta', (SELECT id FROM countries WHERE code = 'ID'), NULL, 1, -6.2088, 106.8456),
('Surabaya', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -7.2575, 112.7521),

-- Philippines
('Manila', (SELECT id FROM countries WHERE code = 'PH'), NULL, 1, 14.5995, 120.9842),
('Quezon City', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.6760, 121.0437),

-- Malaysia
('Kuala Lumpur', (SELECT id FROM countries WHERE code = 'MY'), NULL, 1, 3.1390, 101.6869),
('George Town', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 5.4141, 100.3288),

-- Singapore
('Singapore', (SELECT id FROM countries WHERE code = 'SG'), NULL, 1, 1.3521, 103.8198),

-- Australia (without using states for now)
('Canberra', (SELECT id FROM countries WHERE code = 'AU'), NULL, 1, -35.2809, 149.1300),
('Sydney', (SELECT id FROM countries WHERE code = 'AU'), NULL, 0, -33.8688, 151.2093),
('Melbourne', (SELECT id FROM countries WHERE code = 'AU'), NULL, 0, -37.8136, 144.9631),
('Brisbane', (SELECT id FROM countries WHERE code = 'AU'), NULL, 0, -27.4698, 153.0251),
('Perth', (SELECT id FROM countries WHERE code = 'AU'), NULL, 0, -31.9505, 115.8605),

-- New Zealand
('Wellington', (SELECT id FROM countries WHERE code = 'NZ'), NULL, 1, -41.2865, 174.7762),
('Auckland', (SELECT id FROM countries WHERE code = 'NZ'), NULL, 0, -36.8485, 174.7633),

-- Canada (without using states for now)
('Ottawa', (SELECT id FROM countries WHERE code = 'CA'), NULL, 1, 45.4215, -75.6972),
('Toronto', (SELECT id FROM countries WHERE code = 'CA'), NULL, 0, 43.6532, -79.3832),
('Montreal', (SELECT id FROM countries WHERE code = 'CA'), NULL, 0, 45.5017, -73.5673),
('Vancouver', (SELECT id FROM countries WHERE code = 'CA'), NULL, 0, 49.2827, -123.1207),

-- United States (without using states for now)
('Washington', (SELECT id FROM countries WHERE code = 'US'), NULL, 1, 38.9072, -77.0369),
('New York', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 40.7128, -74.0060),
('Los Angeles', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 34.0522, -118.2437),
('Chicago', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 41.8781, -87.6298),
('Houston', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 29.7604, -95.3698),
('Phoenix', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 33.4484, -112.0740),
('Philadelphia', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 39.9526, -75.1652),
('San Antonio', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 29.4241, -98.4936),
('San Diego', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 32.7157, -117.1611),
('Dallas', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 32.7767, -96.7970),
('San Jose', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 37.3382, -121.8863),
('Austin', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 30.2672, -97.7431),
('Seattle', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 47.6062, -122.3321),
('Denver', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 39.7392, -104.9903),
('Boston', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 42.3601, -71.0589),
('San Francisco', (SELECT id FROM countries WHERE code = 'US'), NULL, 0, 37.7749, -122.4194),

-- Mexico
('Mexico City', (SELECT id FROM countries WHERE code = 'MX'), NULL, 1, 19.4326, -99.1332),
('Guadalajara', (SELECT id FROM countries WHERE code = 'MX'), NULL, 0, 20.6597, -103.3496),
('Monterrey', (SELECT id FROM countries WHERE code = 'MX'), NULL, 0, 25.6866, -100.3161),

-- Brazil
('Brasília', (SELECT id FROM countries WHERE code = 'BR'), NULL, 1, -15.7801, -47.9292),
('São Paulo', (SELECT id FROM countries WHERE code = 'BR'), NULL, 0, -23.5505, -46.6333),
('Rio de Janeiro', (SELECT id FROM countries WHERE code = 'BR'), NULL, 0, -22.9068, -43.1729),

-- Argentina
('Buenos Aires', (SELECT id FROM countries WHERE code = 'AR'), NULL, 1, -34.6037, -58.3816),
('Córdoba', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -31.4201, -64.1888),

-- Chile
('Santiago', (SELECT id FROM countries WHERE code = 'CL'), NULL, 1, -33.4489, -70.6693),
('Valparaíso', (SELECT id FROM countries WHERE code = 'CL'), NULL, 0, -33.0458, -71.6197),

-- Colombia
('Bogotá', (SELECT id FROM countries WHERE code = 'CO'), NULL, 1, 4.7110, -74.0721),
('Medellín', (SELECT id FROM countries WHERE code = 'CO'), NULL, 0, 6.2476, -75.5658),

-- Peru
('Lima', (SELECT id FROM countries WHERE code = 'PE'), NULL, 1, -12.0464, -77.0428),
('Arequipa', (SELECT id FROM countries WHERE code = 'PE'), NULL, 0, -16.4090, -71.5375),

-- Venezuela
('Caracas', (SELECT id FROM countries WHERE code = 'VE'), NULL, 1, 10.4806, -66.9036),
('Maracaibo', (SELECT id FROM countries WHERE code = 'VE'), NULL, 0, 10.6544, -71.6400),

-- South Africa
('Pretoria', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 1, -25.7479, 28.2293),
('Cape Town', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -33.9249, 18.4241),
('Johannesburg', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -26.2041, 28.0473),
('Durban', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -29.8587, 31.0218),

-- Egypt
('Cairo', (SELECT id FROM countries WHERE code = 'EG'), NULL, 1, 30.0444, 31.2357),
('Alexandria', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 31.2001, 29.9187),

-- Nigeria
('Abuja', (SELECT id FROM countries WHERE code = 'NG'), NULL, 1, 9.0765, 7.3986),
('Lagos', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 6.5244, 3.3792),

-- Kenya
('Nairobi', (SELECT id FROM countries WHERE code = 'KE'), NULL, 1, -1.2921, 36.8219),
('Mombasa', (SELECT id FROM countries WHERE code = 'KE'), NULL, 0, -4.0435, 39.6682),

-- Morocco
('Rabat', (SELECT id FROM countries WHERE code = 'MA'), NULL, 1, 33.9716, -6.8498),
('Casablanca', (SELECT id FROM countries WHERE code = 'MA'), NULL, 0, 33.5731, -7.5898),

-- Saudi Arabia
('Riyadh', (SELECT id FROM countries WHERE code = 'SA'), NULL, 1, 24.7136, 46.6753),
('Jeddah', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 21.4858, 39.1925),

-- UAE
('Abu Dhabi', (SELECT id FROM countries WHERE code = 'AE'), NULL, 1, 24.4539, 54.3773),
('Dubai', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.2048, 55.2708),

-- Israel
('Jerusalem', (SELECT id FROM countries WHERE code = 'IL'), NULL, 1, 31.7683, 35.2137),
('Tel Aviv', (SELECT id FROM countries WHERE code = 'IL'), NULL, 0, 32.0853, 34.7818),

-- Iran
('Tehran', (SELECT id FROM countries WHERE code = 'IR'), NULL, 1, 35.6892, 51.3890),
('Isfahan', (SELECT id FROM countries WHERE code = 'IR'), NULL, 0, 32.6546, 51.6680),

-- Pakistan
('Islamabad', (SELECT id FROM countries WHERE code = 'PK'), NULL, 1, 33.6844, 73.0479),
('Karachi', (SELECT id FROM countries WHERE code = 'PK'), NULL, 0, 24.8607, 67.0011),
('Lahore', (SELECT id FROM countries WHERE code = 'PK'), NULL, 0, 31.5204, 74.3587),

-- Bangladesh
('Dhaka', (SELECT id FROM countries WHERE code = 'BD'), NULL, 1, 23.8103, 90.4125),
('Chittagong', (SELECT id FROM countries WHERE code = 'BD'), NULL, 0, 22.3569, 91.7832),

-- UK cities with states
('London', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 1, 51.5074, -0.1278),
('Birmingham', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 52.4862, -1.8904),
('Manchester', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 53.4808, -2.2426),
('Edinburgh', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 55.9533, -3.1883),
('Glasgow', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 55.8642, -4.2518),
('Cardiff', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'WLS' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 51.4816, -3.1791),
('Belfast', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'NIR' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 54.5973, -5.9301);