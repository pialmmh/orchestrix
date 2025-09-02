-- Populate states and major cities for key countries

-- United Kingdom
INSERT INTO states (code, name, country_id) VALUES
('ENG', 'England', (SELECT id FROM countries WHERE code = 'GB')),
('SCT', 'Scotland', (SELECT id FROM countries WHERE code = 'GB')),
('WLS', 'Wales', (SELECT id FROM countries WHERE code = 'GB')),
('NIR', 'Northern Ireland', (SELECT id FROM countries WHERE code = 'GB'));

-- Germany (Federal States)
INSERT INTO states (code, name, country_id) VALUES
('BW', 'Baden-Württemberg', (SELECT id FROM countries WHERE code = 'DE')),
('BY', 'Bavaria', (SELECT id FROM countries WHERE code = 'DE')),
('BE', 'Berlin', (SELECT id FROM countries WHERE code = 'DE')),
('BB', 'Brandenburg', (SELECT id FROM countries WHERE code = 'DE')),
('HB', 'Bremen', (SELECT id FROM countries WHERE code = 'DE')),
('HH', 'Hamburg', (SELECT id FROM countries WHERE code = 'DE')),
('HE', 'Hesse', (SELECT id FROM countries WHERE code = 'DE')),
('NI', 'Lower Saxony', (SELECT id FROM countries WHERE code = 'DE')),
('MV', 'Mecklenburg-Vorpommern', (SELECT id FROM countries WHERE code = 'DE')),
('NW', 'North Rhine-Westphalia', (SELECT id FROM countries WHERE code = 'DE')),
('RP', 'Rhineland-Palatinate', (SELECT id FROM countries WHERE code = 'DE')),
('SL', 'Saarland', (SELECT id FROM countries WHERE code = 'DE')),
('SN', 'Saxony', (SELECT id FROM countries WHERE code = 'DE')),
('ST', 'Saxony-Anhalt', (SELECT id FROM countries WHERE code = 'DE')),
('SH', 'Schleswig-Holstein', (SELECT id FROM countries WHERE code = 'DE')),
('TH', 'Thuringia', (SELECT id FROM countries WHERE code = 'DE'));

-- France (Regions)
INSERT INTO states (code, name, country_id) VALUES
('IDF', 'Île-de-France', (SELECT id FROM countries WHERE code = 'FR')),
('ARA', 'Auvergne-Rhône-Alpes', (SELECT id FROM countries WHERE code = 'FR')),
('BFC', 'Bourgogne-Franche-Comté', (SELECT id FROM countries WHERE code = 'FR')),
('BRE', 'Brittany', (SELECT id FROM countries WHERE code = 'FR')),
('CVL', 'Centre-Val de Loire', (SELECT id FROM countries WHERE code = 'FR')),
('COR', 'Corsica', (SELECT id FROM countries WHERE code = 'FR')),
('GES', 'Grand Est', (SELECT id FROM countries WHERE code = 'FR')),
('HDF', 'Hauts-de-France', (SELECT id FROM countries WHERE code = 'FR')),
('NOR', 'Normandy', (SELECT id FROM countries WHERE code = 'FR')),
('NAQ', 'Nouvelle-Aquitaine', (SELECT id FROM countries WHERE code = 'FR')),
('OCC', 'Occitanie', (SELECT id FROM countries WHERE code = 'FR')),
('PDL', 'Pays de la Loire', (SELECT id FROM countries WHERE code = 'FR')),
('PAC', 'Provence-Alpes-Côte d''Azur', (SELECT id FROM countries WHERE code = 'FR'));

-- Brazil (States)
INSERT INTO states (code, name, country_id) VALUES
('AC', 'Acre', (SELECT id FROM countries WHERE code = 'BR')),
('AL', 'Alagoas', (SELECT id FROM countries WHERE code = 'BR')),
('AP', 'Amapá', (SELECT id FROM countries WHERE code = 'BR')),
('AM', 'Amazonas', (SELECT id FROM countries WHERE code = 'BR')),
('BA', 'Bahia', (SELECT id FROM countries WHERE code = 'BR')),
('CE', 'Ceará', (SELECT id FROM countries WHERE code = 'BR')),
('DF', 'Distrito Federal', (SELECT id FROM countries WHERE code = 'BR')),
('ES', 'Espírito Santo', (SELECT id FROM countries WHERE code = 'BR')),
('GO', 'Goiás', (SELECT id FROM countries WHERE code = 'BR')),
('MA', 'Maranhão', (SELECT id FROM countries WHERE code = 'BR')),
('MT', 'Mato Grosso', (SELECT id FROM countries WHERE code = 'BR')),
('MS', 'Mato Grosso do Sul', (SELECT id FROM countries WHERE code = 'BR')),
('MG', 'Minas Gerais', (SELECT id FROM countries WHERE code = 'BR')),
('PA', 'Pará', (SELECT id FROM countries WHERE code = 'BR')),
('PB', 'Paraíba', (SELECT id FROM countries WHERE code = 'BR')),
('PR', 'Paraná', (SELECT id FROM countries WHERE code = 'BR')),
('PE', 'Pernambuco', (SELECT id FROM countries WHERE code = 'BR')),
('PI', 'Piauí', (SELECT id FROM countries WHERE code = 'BR')),
('RJ', 'Rio de Janeiro', (SELECT id FROM countries WHERE code = 'BR')),
('RN', 'Rio Grande do Norte', (SELECT id FROM countries WHERE code = 'BR')),
('RS', 'Rio Grande do Sul', (SELECT id FROM countries WHERE code = 'BR')),
('RO', 'Rondônia', (SELECT id FROM countries WHERE code = 'BR')),
('RR', 'Roraima', (SELECT id FROM countries WHERE code = 'BR')),
('SC', 'Santa Catarina', (SELECT id FROM countries WHERE code = 'BR')),
('SP', 'São Paulo', (SELECT id FROM countries WHERE code = 'BR')),
('SE', 'Sergipe', (SELECT id FROM countries WHERE code = 'BR')),
('TO', 'Tocantins', (SELECT id FROM countries WHERE code = 'BR'));

-- China (Provinces)
INSERT INTO states (code, name, country_id) VALUES
('BJ', 'Beijing', (SELECT id FROM countries WHERE code = 'CN')),
('TJ', 'Tianjin', (SELECT id FROM countries WHERE code = 'CN')),
('HE', 'Hebei', (SELECT id FROM countries WHERE code = 'CN')),
('SX', 'Shanxi', (SELECT id FROM countries WHERE code = 'CN')),
('NM', 'Inner Mongolia', (SELECT id FROM countries WHERE code = 'CN')),
('LN', 'Liaoning', (SELECT id FROM countries WHERE code = 'CN')),
('JL', 'Jilin', (SELECT id FROM countries WHERE code = 'CN')),
('HL', 'Heilongjiang', (SELECT id FROM countries WHERE code = 'CN')),
('SH', 'Shanghai', (SELECT id FROM countries WHERE code = 'CN')),
('JS', 'Jiangsu', (SELECT id FROM countries WHERE code = 'CN')),
('ZJ', 'Zhejiang', (SELECT id FROM countries WHERE code = 'CN')),
('AH', 'Anhui', (SELECT id FROM countries WHERE code = 'CN')),
('FJ', 'Fujian', (SELECT id FROM countries WHERE code = 'CN')),
('JX', 'Jiangxi', (SELECT id FROM countries WHERE code = 'CN')),
('SD', 'Shandong', (SELECT id FROM countries WHERE code = 'CN')),
('HA', 'Henan', (SELECT id FROM countries WHERE code = 'CN')),
('HB', 'Hubei', (SELECT id FROM countries WHERE code = 'CN')),
('HN', 'Hunan', (SELECT id FROM countries WHERE code = 'CN')),
('GD', 'Guangdong', (SELECT id FROM countries WHERE code = 'CN')),
('GX', 'Guangxi', (SELECT id FROM countries WHERE code = 'CN')),
('HI', 'Hainan', (SELECT id FROM countries WHERE code = 'CN')),
('CQ', 'Chongqing', (SELECT id FROM countries WHERE code = 'CN')),
('SC', 'Sichuan', (SELECT id FROM countries WHERE code = 'CN')),
('GZ', 'Guizhou', (SELECT id FROM countries WHERE code = 'CN')),
('YN', 'Yunnan', (SELECT id FROM countries WHERE code = 'CN')),
('XZ', 'Tibet', (SELECT id FROM countries WHERE code = 'CN')),
('SN', 'Shaanxi', (SELECT id FROM countries WHERE code = 'CN')),
('GS', 'Gansu', (SELECT id FROM countries WHERE code = 'CN')),
('QH', 'Qinghai', (SELECT id FROM countries WHERE code = 'CN')),
('NX', 'Ningxia', (SELECT id FROM countries WHERE code = 'CN')),
('XJ', 'Xinjiang', (SELECT id FROM countries WHERE code = 'CN'));

-- Japan (Prefectures - Major ones)
INSERT INTO states (code, name, country_id) VALUES
('TK', 'Tokyo', (SELECT id FROM countries WHERE code = 'JP')),
('OS', 'Osaka', (SELECT id FROM countries WHERE code = 'JP')),
('KY', 'Kyoto', (SELECT id FROM countries WHERE code = 'JP')),
('AI', 'Aichi', (SELECT id FROM countries WHERE code = 'JP')),
('KN', 'Kanagawa', (SELECT id FROM countries WHERE code = 'JP')),
('ST', 'Saitama', (SELECT id FROM countries WHERE code = 'JP')),
('CB', 'Chiba', (SELECT id FROM countries WHERE code = 'JP')),
('HG', 'Hyogo', (SELECT id FROM countries WHERE code = 'JP')),
('FK', 'Fukuoka', (SELECT id FROM countries WHERE code = 'JP')),
('HK', 'Hokkaido', (SELECT id FROM countries WHERE code = 'JP'));

-- Mexico (States)
INSERT INTO states (code, name, country_id) VALUES
('AGU', 'Aguascalientes', (SELECT id FROM countries WHERE code = 'MX')),
('BCN', 'Baja California', (SELECT id FROM countries WHERE code = 'MX')),
('BCS', 'Baja California Sur', (SELECT id FROM countries WHERE code = 'MX')),
('CAM', 'Campeche', (SELECT id FROM countries WHERE code = 'MX')),
('CHP', 'Chiapas', (SELECT id FROM countries WHERE code = 'MX')),
('CHH', 'Chihuahua', (SELECT id FROM countries WHERE code = 'MX')),
('CMX', 'Ciudad de México', (SELECT id FROM countries WHERE code = 'MX')),
('COA', 'Coahuila', (SELECT id FROM countries WHERE code = 'MX')),
('COL', 'Colima', (SELECT id FROM countries WHERE code = 'MX')),
('DUR', 'Durango', (SELECT id FROM countries WHERE code = 'MX')),
('GUA', 'Guanajuato', (SELECT id FROM countries WHERE code = 'MX')),
('GRO', 'Guerrero', (SELECT id FROM countries WHERE code = 'MX')),
('HID', 'Hidalgo', (SELECT id FROM countries WHERE code = 'MX')),
('JAL', 'Jalisco', (SELECT id FROM countries WHERE code = 'MX')),
('MEX', 'México', (SELECT id FROM countries WHERE code = 'MX')),
('MIC', 'Michoacán', (SELECT id FROM countries WHERE code = 'MX')),
('MOR', 'Morelos', (SELECT id FROM countries WHERE code = 'MX')),
('NAY', 'Nayarit', (SELECT id FROM countries WHERE code = 'MX')),
('NLE', 'Nuevo León', (SELECT id FROM countries WHERE code = 'MX')),
('OAX', 'Oaxaca', (SELECT id FROM countries WHERE code = 'MX')),
('PUE', 'Puebla', (SELECT id FROM countries WHERE code = 'MX')),
('QUE', 'Querétaro', (SELECT id FROM countries WHERE code = 'MX')),
('ROO', 'Quintana Roo', (SELECT id FROM countries WHERE code = 'MX')),
('SLP', 'San Luis Potosí', (SELECT id FROM countries WHERE code = 'MX')),
('SIN', 'Sinaloa', (SELECT id FROM countries WHERE code = 'MX')),
('SON', 'Sonora', (SELECT id FROM countries WHERE code = 'MX')),
('TAB', 'Tabasco', (SELECT id FROM countries WHERE code = 'MX')),
('TAM', 'Tamaulipas', (SELECT id FROM countries WHERE code = 'MX')),
('TLA', 'Tlaxcala', (SELECT id FROM countries WHERE code = 'MX')),
('VER', 'Veracruz', (SELECT id FROM countries WHERE code = 'MX')),
('YUC', 'Yucatán', (SELECT id FROM countries WHERE code = 'MX')),
('ZAC', 'Zacatecas', (SELECT id FROM countries WHERE code = 'MX'));

-- Now add major cities for countries
-- United States (Major cities)
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('New York', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'NY' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 40.7128, -74.0060),
('Los Angeles', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 34.0522, -118.2437),
('Chicago', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'IL' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 41.8781, -87.6298),
('Houston', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 29.7604, -95.3698),
('Phoenix', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'AZ' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 33.4484, -112.0740),
('Philadelphia', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'PA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 39.9526, -75.1652),
('San Antonio', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 29.4241, -98.4936),
('San Diego', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 32.7157, -117.1611),
('Dallas', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 32.7767, -96.7970),
('San Jose', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 37.3382, -121.8863),
('Austin', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'TX' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 30.2672, -97.7431),
('Seattle', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'WA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 47.6062, -122.3321),
('Denver', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'CO' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 39.7392, -104.9903),
('Boston', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'MA' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 42.3601, -71.0589),
('Miami', (SELECT id FROM countries WHERE code = 'US'), (SELECT id FROM states WHERE code = 'FL' AND country_id = (SELECT id FROM countries WHERE code = 'US')), 0, 25.7617, -80.1918);

-- United Kingdom
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('London', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 1, 51.5074, -0.1278),
('Birmingham', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 52.4862, -1.8904),
('Manchester', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 53.4808, -2.2426),
('Edinburgh', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 55.9533, -3.1883),
('Glasgow', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'SCT' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 55.8642, -4.2518),
('Cardiff', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'WLS' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 51.4816, -3.1791),
('Belfast', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'NIR' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 54.5973, -5.9301),
('Liverpool', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 53.4084, -2.9916),
('Leeds', (SELECT id FROM countries WHERE code = 'GB'), (SELECT id FROM states WHERE code = 'ENG' AND country_id = (SELECT id FROM countries WHERE code = 'GB')), 0, 53.8008, -1.5491);

-- Germany
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Berlin', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 1, 52.5200, 13.4050),
('Hamburg', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HH' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 53.5511, 9.9937),
('Munich', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BY' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 48.1351, 11.5820),
('Cologne', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'NW' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 50.9375, 6.9603),
('Frankfurt', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'HE' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 50.1109, 8.6821),
('Stuttgart', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'BW' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 48.7758, 9.1829),
('Düsseldorf', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'NW' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 51.2277, 6.7735),
('Dortmund', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'NW' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 51.5136, 7.4653),
('Leipzig', (SELECT id FROM countries WHERE code = 'DE'), (SELECT id FROM states WHERE code = 'SN' AND country_id = (SELECT id FROM countries WHERE code = 'DE')), 0, 51.3397, 12.3731);

-- France
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Paris', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'IDF' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 1, 48.8566, 2.3522),
('Marseille', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'PAC' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 43.2965, 5.3698),
('Lyon', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'ARA' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 45.7640, 4.8357),
('Toulouse', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'OCC' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 43.6047, 1.4442),
('Nice', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'PAC' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 43.7102, 7.2620),
('Nantes', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'PDL' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 47.2184, -1.5536),
('Strasbourg', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'GES' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 48.5734, 7.7521),
('Bordeaux', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'NAQ' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 44.8378, -0.5792),
('Lille', (SELECT id FROM countries WHERE code = 'FR'), (SELECT id FROM states WHERE code = 'HDF' AND country_id = (SELECT id FROM countries WHERE code = 'FR')), 0, 50.6292, 3.0573);

-- Brazil
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Brasília', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'DF' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 1, -15.7801, -47.9292),
('São Paulo', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'SP' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -23.5505, -46.6333),
('Rio de Janeiro', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'RJ' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -22.9068, -43.1729),
('Salvador', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'BA' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -12.9714, -38.5014),
('Fortaleza', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'CE' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -3.7172, -38.5433),
('Belo Horizonte', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'MG' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -19.9167, -43.9345),
('Manaus', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'AM' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -3.1190, -60.0217),
('Curitiba', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'PR' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -25.4284, -49.2733),
('Recife', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'PE' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -8.0476, -34.8770),
('Porto Alegre', (SELECT id FROM countries WHERE code = 'BR'), (SELECT id FROM states WHERE code = 'RS' AND country_id = (SELECT id FROM countries WHERE code = 'BR')), 0, -30.0346, -51.2177);

-- China
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Beijing', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'BJ' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 1, 39.9042, 116.4074),
('Shanghai', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'SH' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 31.2304, 121.4737),
('Guangzhou', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'GD' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 23.1291, 113.2644),
('Shenzhen', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'GD' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 22.5431, 114.0579),
('Chengdu', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'SC' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 30.5728, 104.0668),
('Tianjin', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'TJ' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 39.1420, 117.1767),
('Wuhan', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'HB' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 30.5928, 114.3055),
('Chongqing', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'CQ' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 29.5630, 106.5516),
('Nanjing', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'JS' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 32.0603, 118.7969),
('Xi''an', (SELECT id FROM countries WHERE code = 'CN'), (SELECT id FROM states WHERE code = 'SN' AND country_id = (SELECT id FROM countries WHERE code = 'CN')), 0, 34.3416, 108.9398);

-- Japan
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Tokyo', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'TK' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 1, 35.6762, 139.6503),
('Yokohama', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'KN' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 35.4437, 139.6380),
('Osaka', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'OS' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 34.6937, 135.5023),
('Nagoya', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'AI' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 35.1815, 136.9066),
('Sapporo', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'HK' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 43.0621, 141.3544),
('Kobe', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'HG' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 34.6901, 135.1955),
('Kyoto', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'KY' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 35.0116, 135.7681),
('Fukuoka', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'FK' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 33.5904, 130.4017),
('Kawasaki', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'KN' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 35.5208, 139.7173),
('Saitama', (SELECT id FROM countries WHERE code = 'JP'), (SELECT id FROM states WHERE code = 'ST' AND country_id = (SELECT id FROM countries WHERE code = 'JP')), 0, 35.8617, 139.6455);

-- India
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('New Delhi', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'DL' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 1, 28.6139, 77.2090),
('Mumbai', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'MH' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 19.0760, 72.8777),
('Bangalore', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'KA' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 12.9716, 77.5946),
('Hyderabad', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'TG' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 17.3850, 78.4867),
('Chennai', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'TN' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 13.0827, 80.2707),
('Kolkata', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'WB' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 22.5726, 88.3639),
('Ahmedabad', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'GJ' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 23.0225, 72.5714),
('Pune', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'MH' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 18.5204, 73.8567),
('Surat', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'GJ' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 21.1702, 72.8311),
('Jaipur', (SELECT id FROM countries WHERE code = 'IN'), (SELECT id FROM states WHERE code = 'RJ' AND country_id = (SELECT id FROM countries WHERE code = 'IN')), 0, 26.9124, 75.7873);

-- Canada
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Ottawa', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'ON' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 1, 45.4215, -75.6972),
('Toronto', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'ON' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 43.6532, -79.3832),
('Montreal', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'QC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 45.5017, -73.5673),
('Vancouver', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'BC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 49.2827, -123.1207),
('Calgary', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'AB' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 51.0447, -114.0719),
('Edmonton', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'AB' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 53.5461, -113.4938),
('Quebec City', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'QC' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 46.8139, -71.2080),
('Winnipeg', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'MB' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 49.8951, -97.1384),
('Halifax', (SELECT id FROM countries WHERE code = 'CA'), (SELECT id FROM states WHERE code = 'NS' AND country_id = (SELECT id FROM countries WHERE code = 'CA')), 0, 44.6488, -63.5752);

-- Australia
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Canberra', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'ACT' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 1, -35.2809, 149.1300),
('Sydney', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'NSW' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -33.8688, 151.2093),
('Melbourne', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'VIC' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -37.8136, 144.9631),
('Brisbane', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'QLD' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -27.4698, 153.0251),
('Perth', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'WA' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -31.9505, 115.8605),
('Adelaide', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'SA' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -34.9285, 138.6007),
('Gold Coast', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'QLD' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -28.0167, 153.4000),
('Hobart', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'TAS' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -42.8821, 147.3272),
('Darwin', (SELECT id FROM countries WHERE code = 'AU'), (SELECT id FROM states WHERE code = 'NT' AND country_id = (SELECT id FROM countries WHERE code = 'AU')), 0, -12.4634, 130.8456);

-- Mexico
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Mexico City', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'CMX' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 1, 19.4326, -99.1332),
('Guadalajara', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'JAL' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 20.6597, -103.3496),
('Monterrey', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'NLE' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 25.6866, -100.3161),
('Puebla', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'PUE' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 19.0413, -98.2062),
('Tijuana', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'BCN' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 32.5149, -117.0382),
('León', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'GUA' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 21.1251, -101.6869),
('Juárez', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'CHH' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 31.6904, -106.4245),
('Zapopan', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'JAL' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 20.7211, -103.3847),
('Cancún', (SELECT id FROM countries WHERE code = 'MX'), (SELECT id FROM states WHERE code = 'ROO' AND country_id = (SELECT id FROM countries WHERE code = 'MX')), 0, 21.1619, -86.8515);

-- Add major cities for countries without states
-- Italy
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Rome', (SELECT id FROM countries WHERE code = 'IT'), NULL, 1, 41.9028, 12.4964),
('Milan', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 45.4642, 9.1900),
('Naples', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 40.8518, 14.2681),
('Turin', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 45.0703, 7.6869),
('Palermo', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 38.1157, 13.3615),
('Genoa', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 44.4056, 8.9463),
('Bologna', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 44.4949, 11.3426),
('Florence', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 43.7696, 11.2558),
('Venice', (SELECT id FROM countries WHERE code = 'IT'), NULL, 0, 45.4408, 12.3155);

-- Spain
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Madrid', (SELECT id FROM countries WHERE code = 'ES'), NULL, 1, 40.4168, -3.7038),
('Barcelona', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 41.3851, 2.1734),
('Valencia', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 39.4699, -0.3763),
('Seville', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 37.3891, -5.9845),
('Zaragoza', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 41.6488, -0.8891),
('Málaga', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 36.7213, -4.4214),
('Murcia', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 37.9922, -1.1307),
('Palma', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 39.5696, 2.6502),
('Bilbao', (SELECT id FROM countries WHERE code = 'ES'), NULL, 0, 43.2630, -2.9350);

-- Russia
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Moscow', (SELECT id FROM countries WHERE code = 'RU'), NULL, 1, 55.7558, 37.6173),
('Saint Petersburg', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 59.9311, 30.3609),
('Novosibirsk', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 55.0084, 82.9357),
('Yekaterinburg', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 56.8389, 60.6057),
('Nizhny Novgorod', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 56.2965, 43.9361),
('Kazan', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 55.8304, 49.0661),
('Chelyabinsk', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 55.1644, 61.4368),
('Omsk', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 54.9885, 73.3242),
('Samara', (SELECT id FROM countries WHERE code = 'RU'), NULL, 0, 53.2415, 50.2212);

-- South Korea
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Seoul', (SELECT id FROM countries WHERE code = 'KR'), NULL, 1, 37.5665, 126.9780),
('Busan', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 35.1796, 129.0756),
('Incheon', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 37.4563, 126.7052),
('Daegu', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 35.8714, 128.6014),
('Daejeon', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 36.3504, 127.3845),
('Gwangju', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 35.1595, 126.8526),
('Suwon', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 37.2636, 127.0286),
('Ulsan', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 35.5384, 129.3114),
('Sejong', (SELECT id FROM countries WHERE code = 'KR'), NULL, 0, 36.4801, 127.2890);

-- Turkey
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Ankara', (SELECT id FROM countries WHERE code = 'TR'), NULL, 1, 39.9334, 32.8597),
('Istanbul', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 41.0082, 28.9784),
('Izmir', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 38.4192, 27.1287),
('Bursa', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 40.1826, 29.0665),
('Adana', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 36.9914, 35.3308),
('Gaziantep', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 37.0662, 37.3833),
('Konya', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 37.8667, 32.4833),
('Antalya', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 36.8969, 30.7133),
('Mersin', (SELECT id FROM countries WHERE code = 'TR'), NULL, 0, 36.8121, 34.6415);

-- Argentina
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Buenos Aires', (SELECT id FROM countries WHERE code = 'AR'), NULL, 1, -34.6037, -58.3816),
('Córdoba', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -31.4201, -64.1888),
('Rosario', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -32.9468, -60.6393),
('Mendoza', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -32.8895, -68.8458),
('San Miguel de Tucumán', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -26.8083, -65.2176),
('La Plata', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -34.9215, -57.9545),
('Mar del Plata', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -38.0055, -57.5426),
('Salta', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -24.7821, -65.4232),
('Santa Fe', (SELECT id FROM countries WHERE code = 'AR'), NULL, 0, -31.6333, -60.7000);

-- South Africa
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Pretoria', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 1, -25.7479, 28.2293),
('Cape Town', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -33.9249, 18.4241),
('Johannesburg', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -26.2041, 28.0473),
('Durban', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -29.8587, 31.0218),
('Port Elizabeth', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -33.9608, 25.6022),
('Bloemfontein', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -29.0852, 26.1596),
('East London', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -33.0153, 27.9116),
('Pietermaritzburg', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -29.6006, 30.3794),
('Kimberley', (SELECT id FROM countries WHERE code = 'ZA'), NULL, 0, -28.7282, 24.7499);

-- Egypt
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Cairo', (SELECT id FROM countries WHERE code = 'EG'), NULL, 1, 30.0444, 31.2357),
('Alexandria', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 31.2001, 29.9187),
('Giza', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 30.0131, 31.2089),
('Shubra El Kheima', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 30.1286, 31.2422),
('Port Said', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 31.2653, 32.3019),
('Suez', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 29.9737, 32.5263),
('Luxor', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 25.6872, 32.6396),
('Aswan', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 24.0889, 32.8998),
('Mansoura', (SELECT id FROM countries WHERE code = 'EG'), NULL, 0, 31.0409, 31.3785);

-- Nigeria
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Abuja', (SELECT id FROM countries WHERE code = 'NG'), NULL, 1, 9.0765, 7.3986),
('Lagos', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 6.5244, 3.3792),
('Kano', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 12.0022, 8.5919),
('Ibadan', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 7.3775, 3.9470),
('Port Harcourt', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 4.8156, 7.0498),
('Benin City', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 6.3350, 5.6037),
('Maiduguri', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 11.8333, 13.1500),
('Zaria', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 11.0855, 7.7199),
('Aba', (SELECT id FROM countries WHERE code = 'NG'), NULL, 0, 5.1066, 7.3668);

-- Thailand
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Bangkok', (SELECT id FROM countries WHERE code = 'TH'), NULL, 1, 13.7563, 100.5018),
('Nonthaburi', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 13.8621, 100.5144),
('Nakhon Ratchasima', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 14.9799, 102.0977),
('Chiang Mai', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 18.7883, 98.9853),
('Hat Yai', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 7.0089, 100.4760),
('Udon Thani', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 17.4138, 102.7877),
('Pak Kret', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 13.9130, 100.4989),
('Khon Kaen', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 16.4419, 102.8360),
('Phuket', (SELECT id FROM countries WHERE code = 'TH'), NULL, 0, 7.8804, 98.3923);

-- Indonesia
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Jakarta', (SELECT id FROM countries WHERE code = 'ID'), NULL, 1, -6.2088, 106.8456),
('Surabaya', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -7.2575, 112.7521),
('Bandung', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -6.9175, 107.6191),
('Medan', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, 3.5952, 98.6722),
('Semarang', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -6.9932, 110.4203),
('Makassar', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -5.1477, 119.4327),
('Palembang', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -2.9761, 104.7754),
('Tangerang', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -6.1702, 106.6403),
('Depok', (SELECT id FROM countries WHERE code = 'ID'), NULL, 0, -6.4025, 106.7942);

-- Philippines
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Manila', (SELECT id FROM countries WHERE code = 'PH'), NULL, 1, 14.5995, 120.9842),
('Quezon City', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.6760, 121.0437),
('Davao', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 7.1907, 125.4553),
('Caloocan', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.6490, 120.9840),
('Cebu City', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 10.3157, 123.8854),
('Zamboanga City', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 6.9214, 122.0790),
('Taguig', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.5243, 121.0792),
('Antipolo', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.6254, 121.1246),
('Pasig', (SELECT id FROM countries WHERE code = 'PH'), NULL, 0, 14.5453, 121.0859);

-- Vietnam
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Hanoi', (SELECT id FROM countries WHERE code = 'VN'), NULL, 1, 21.0285, 105.8542),
('Ho Chi Minh City', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 10.8231, 106.6297),
('Da Nang', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 16.0544, 108.2022),
('Haiphong', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 20.8449, 106.6881),
('Can Tho', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 10.0452, 105.7469),
('Bien Hoa', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 10.9574, 106.8426),
('Nha Trang', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 12.2388, 109.1967),
('Hue', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 16.4637, 107.5909),
('Vung Tau', (SELECT id FROM countries WHERE code = 'VN'), NULL, 0, 10.4113, 107.1362);

-- Malaysia
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Kuala Lumpur', (SELECT id FROM countries WHERE code = 'MY'), NULL, 1, 3.1390, 101.6869),
('George Town', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 5.4141, 100.3288),
('Ipoh', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 4.5975, 101.0901),
('Shah Alam', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 3.0738, 101.5183),
('Petaling Jaya', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 3.1073, 101.6067),
('Johor Bahru', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 1.4927, 103.7414),
('Malacca City', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 2.1896, 102.2501),
('Kota Kinabalu', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 5.9804, 116.0735),
('Kuching', (SELECT id FROM countries WHERE code = 'MY'), NULL, 0, 1.5533, 110.3592);

-- Singapore (city-state)
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Singapore', (SELECT id FROM countries WHERE code = 'SG'), NULL, 1, 1.3521, 103.8198);

-- Netherlands
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Amsterdam', (SELECT id FROM countries WHERE code = 'NL'), NULL, 1, 52.3676, 4.9041),
('Rotterdam', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 51.9244, 4.4777),
('The Hague', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 52.0705, 4.3007),
('Utrecht', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 52.0907, 5.1214),
('Eindhoven', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 51.4416, 5.4697),
('Tilburg', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 51.5555, 5.0913),
('Groningen', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 53.2194, 6.5665),
('Almere', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 52.3508, 5.2647),
('Breda', (SELECT id FROM countries WHERE code = 'NL'), NULL, 0, 51.5719, 4.7683);

-- Belgium
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Brussels', (SELECT id FROM countries WHERE code = 'BE'), NULL, 1, 50.8503, 4.3517),
('Antwerp', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 51.2194, 4.4025),
('Ghent', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 51.0543, 3.7174),
('Charleroi', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 50.4108, 4.4446),
('Liège', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 50.6326, 5.5797),
('Bruges', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 51.2093, 3.2247),
('Namur', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 50.4674, 4.8720),
('Leuven', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 50.8798, 4.7005),
('Mons', (SELECT id FROM countries WHERE code = 'BE'), NULL, 0, 50.4542, 3.9517);

-- Switzerland
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Bern', (SELECT id FROM countries WHERE code = 'CH'), NULL, 1, 46.9480, 7.4474),
('Zurich', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.3769, 8.5417),
('Geneva', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 46.2044, 6.1432),
('Basel', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.5596, 7.5886),
('Lausanne', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 46.5197, 6.6323),
('Winterthur', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.5001, 8.7231),
('Lucerne', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.0502, 8.3093),
('St. Gallen', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 47.4245, 9.3767),
('Lugano', (SELECT id FROM countries WHERE code = 'CH'), NULL, 0, 46.0037, 8.9511);

-- Austria
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Vienna', (SELECT id FROM countries WHERE code = 'AT'), NULL, 1, 48.2082, 16.3738),
('Graz', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 47.0707, 15.4395),
('Linz', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 48.3069, 14.2858),
('Salzburg', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 47.8095, 13.0550),
('Innsbruck', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 47.2692, 11.4041),
('Klagenfurt', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 46.6365, 14.3122),
('Villach', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 46.6103, 13.8558),
('Wels', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 48.1575, 14.0248),
('St. Pölten', (SELECT id FROM countries WHERE code = 'AT'), NULL, 0, 48.2047, 15.6256);

-- Poland
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Warsaw', (SELECT id FROM countries WHERE code = 'PL'), NULL, 1, 52.2297, 21.0122),
('Kraków', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 50.0647, 19.9450),
('Łódź', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 51.7592, 19.4560),
('Wrocław', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 51.1079, 17.0385),
('Poznań', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 52.4064, 16.9252),
('Gdańsk', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 54.3520, 18.6466),
('Szczecin', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 53.4285, 14.5528),
('Bydgoszcz', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 53.1235, 18.0084),
('Lublin', (SELECT id FROM countries WHERE code = 'PL'), NULL, 0, 51.2465, 22.5684);

-- Saudi Arabia
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Riyadh', (SELECT id FROM countries WHERE code = 'SA'), NULL, 1, 24.7136, 46.6753),
('Jeddah', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 21.4858, 39.1925),
('Mecca', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 21.4225, 39.8262),
('Medina', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 24.4686, 39.6142),
('Dammam', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 26.4207, 50.0888),
('Khobar', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 26.2172, 50.1971),
('Taif', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 21.2703, 40.4158),
('Tabuk', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 28.3838, 36.5550),
('Buraidah', (SELECT id FROM countries WHERE code = 'SA'), NULL, 0, 26.3267, 43.9750);

-- UAE
INSERT INTO cities (name, country_id, state_id, is_capital, latitude, longitude) VALUES
('Abu Dhabi', (SELECT id FROM countries WHERE code = 'AE'), NULL, 1, 24.4539, 54.3773),
('Dubai', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.2048, 55.2708),
('Sharjah', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.3463, 55.4209),
('Al Ain', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 24.2075, 55.7446),
('Ajman', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.4052, 55.5136),
('Ras Al Khaimah', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.7895, 55.9432),
('Fujairah', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.1288, 56.3264),
('Umm Al Quwain', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.5644, 55.5551),
('Khor Fakkan', (SELECT id FROM countries WHERE code = 'AE'), NULL, 0, 25.3394, 56.3420);