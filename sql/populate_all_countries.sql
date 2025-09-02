USE orchestrix;

-- Clear existing data
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE cities;
TRUNCATE TABLE states;
TRUNCATE TABLE countries;
SET FOREIGN_KEY_CHECKS=1;

-- Insert comprehensive list of countries
INSERT INTO countries (code, code3, name, region, has_states) VALUES
-- North America
('US', 'USA', 'United States', 'North America', TRUE),
('CA', 'CAN', 'Canada', 'North America', TRUE),
('MX', 'MEX', 'Mexico', 'North America', TRUE),
('GT', 'GTM', 'Guatemala', 'North America', FALSE),
('CU', 'CUB', 'Cuba', 'North America', FALSE),
('HT', 'HTI', 'Haiti', 'North America', FALSE),
('DO', 'DOM', 'Dominican Republic', 'North America', FALSE),
('HN', 'HND', 'Honduras', 'North America', FALSE),
('NI', 'NIC', 'Nicaragua', 'North America', FALSE),
('SV', 'SLV', 'El Salvador', 'North America', FALSE),
('CR', 'CRI', 'Costa Rica', 'North America', FALSE),
('PA', 'PAN', 'Panama', 'North America', FALSE),
('JM', 'JAM', 'Jamaica', 'North America', FALSE),
('TT', 'TTO', 'Trinidad and Tobago', 'North America', FALSE),
('BB', 'BRB', 'Barbados', 'North America', FALSE),
('BS', 'BHS', 'Bahamas', 'North America', FALSE),
('BZ', 'BLZ', 'Belize', 'North America', FALSE),

-- South America
('BR', 'BRA', 'Brazil', 'South America', TRUE),
('AR', 'ARG', 'Argentina', 'South America', TRUE),
('CO', 'COL', 'Colombia', 'South America', FALSE),
('PE', 'PER', 'Peru', 'South America', FALSE),
('VE', 'VEN', 'Venezuela', 'South America', TRUE),
('CL', 'CHL', 'Chile', 'South America', FALSE),
('EC', 'ECU', 'Ecuador', 'South America', FALSE),
('BO', 'BOL', 'Bolivia', 'South America', FALSE),
('PY', 'PRY', 'Paraguay', 'South America', FALSE),
('UY', 'URY', 'Uruguay', 'South America', FALSE),
('GY', 'GUY', 'Guyana', 'South America', FALSE),
('SR', 'SUR', 'Suriname', 'South America', FALSE),
('GF', 'GUF', 'French Guiana', 'South America', FALSE),

-- Europe
('GB', 'GBR', 'United Kingdom', 'Europe', TRUE),
('FR', 'FRA', 'France', 'Europe', FALSE),
('DE', 'DEU', 'Germany', 'Europe', TRUE),
('IT', 'ITA', 'Italy', 'Europe', FALSE),
('ES', 'ESP', 'Spain', 'Europe', TRUE),
('PT', 'PRT', 'Portugal', 'Europe', FALSE),
('NL', 'NLD', 'Netherlands', 'Europe', FALSE),
('BE', 'BEL', 'Belgium', 'Europe', FALSE),
('LU', 'LUX', 'Luxembourg', 'Europe', FALSE),
('CH', 'CHE', 'Switzerland', 'Europe', TRUE),
('AT', 'AUT', 'Austria', 'Europe', TRUE),
('IE', 'IRL', 'Ireland', 'Europe', FALSE),
('SE', 'SWE', 'Sweden', 'Europe', FALSE),
('NO', 'NOR', 'Norway', 'Europe', FALSE),
('FI', 'FIN', 'Finland', 'Europe', FALSE),
('DK', 'DNK', 'Denmark', 'Europe', FALSE),
('IS', 'ISL', 'Iceland', 'Europe', FALSE),
('PL', 'POL', 'Poland', 'Europe', FALSE),
('CZ', 'CZE', 'Czech Republic', 'Europe', FALSE),
('SK', 'SVK', 'Slovakia', 'Europe', FALSE),
('HU', 'HUN', 'Hungary', 'Europe', FALSE),
('RO', 'ROU', 'Romania', 'Europe', FALSE),
('BG', 'BGR', 'Bulgaria', 'Europe', FALSE),
('HR', 'HRV', 'Croatia', 'Europe', FALSE),
('RS', 'SRB', 'Serbia', 'Europe', FALSE),
('SI', 'SVN', 'Slovenia', 'Europe', FALSE),
('BA', 'BIH', 'Bosnia and Herzegovina', 'Europe', FALSE),
('ME', 'MNE', 'Montenegro', 'Europe', FALSE),
('MK', 'MKD', 'North Macedonia', 'Europe', FALSE),
('AL', 'ALB', 'Albania', 'Europe', FALSE),
('GR', 'GRC', 'Greece', 'Europe', FALSE),
('CY', 'CYP', 'Cyprus', 'Europe', FALSE),
('MT', 'MLT', 'Malta', 'Europe', FALSE),
('EE', 'EST', 'Estonia', 'Europe', FALSE),
('LV', 'LVA', 'Latvia', 'Europe', FALSE),
('LT', 'LTU', 'Lithuania', 'Europe', FALSE),
('BY', 'BLR', 'Belarus', 'Europe', FALSE),
('UA', 'UKR', 'Ukraine', 'Europe', FALSE),
('MD', 'MDA', 'Moldova', 'Europe', FALSE),
('MC', 'MCO', 'Monaco', 'Europe', FALSE),
('LI', 'LIE', 'Liechtenstein', 'Europe', FALSE),
('SM', 'SMR', 'San Marino', 'Europe', FALSE),
('VA', 'VAT', 'Vatican City', 'Europe', FALSE),
('AD', 'AND', 'Andorra', 'Europe', FALSE),

-- Asia
('CN', 'CHN', 'China', 'Asia', TRUE),
('IN', 'IND', 'India', 'Asia', TRUE),
('ID', 'IDN', 'Indonesia', 'Asia', TRUE),
('PK', 'PAK', 'Pakistan', 'Asia', TRUE),
('BD', 'BGD', 'Bangladesh', 'Asia', FALSE),
('RU', 'RUS', 'Russia', 'Europe/Asia', TRUE),
('JP', 'JPN', 'Japan', 'Asia', TRUE),
('PH', 'PHL', 'Philippines', 'Asia', FALSE),
('VN', 'VNM', 'Vietnam', 'Asia', FALSE),
('TR', 'TUR', 'Turkey', 'Europe/Asia', FALSE),
('IR', 'IRN', 'Iran', 'Asia', FALSE),
('TH', 'THA', 'Thailand', 'Asia', FALSE),
('MM', 'MMR', 'Myanmar', 'Asia', FALSE),
('KR', 'KOR', 'South Korea', 'Asia', FALSE),
('IQ', 'IRQ', 'Iraq', 'Asia', FALSE),
('AF', 'AFG', 'Afghanistan', 'Asia', FALSE),
('SA', 'SAU', 'Saudi Arabia', 'Asia', FALSE),
('UZ', 'UZB', 'Uzbekistan', 'Asia', FALSE),
('MY', 'MYS', 'Malaysia', 'Asia', TRUE),
('NP', 'NPL', 'Nepal', 'Asia', FALSE),
('YE', 'YEM', 'Yemen', 'Asia', FALSE),
('KP', 'PRK', 'North Korea', 'Asia', FALSE),
('LK', 'LKA', 'Sri Lanka', 'Asia', FALSE),
('KZ', 'KAZ', 'Kazakhstan', 'Asia', FALSE),
('SY', 'SYR', 'Syria', 'Asia', FALSE),
('KH', 'KHM', 'Cambodia', 'Asia', FALSE),
('JO', 'JOR', 'Jordan', 'Asia', FALSE),
('AZ', 'AZE', 'Azerbaijan', 'Asia', FALSE),
('AE', 'ARE', 'United Arab Emirates', 'Asia', TRUE),
('TJ', 'TJK', 'Tajikistan', 'Asia', FALSE),
('IL', 'ISR', 'Israel', 'Asia', FALSE),
('HK', 'HKG', 'Hong Kong', 'Asia', FALSE),
('LA', 'LAO', 'Laos', 'Asia', FALSE),
('LB', 'LBN', 'Lebanon', 'Asia', FALSE),
('SG', 'SGP', 'Singapore', 'Asia', FALSE),
('OM', 'OMN', 'Oman', 'Asia', FALSE),
('PS', 'PSE', 'Palestine', 'Asia', FALSE),
('KW', 'KWT', 'Kuwait', 'Asia', FALSE),
('GE', 'GEO', 'Georgia', 'Asia', FALSE),
('MN', 'MNG', 'Mongolia', 'Asia', FALSE),
('AM', 'ARM', 'Armenia', 'Asia', FALSE),
('QA', 'QAT', 'Qatar', 'Asia', FALSE),
('BH', 'BHR', 'Bahrain', 'Asia', FALSE),
('TL', 'TLS', 'Timor-Leste', 'Asia', FALSE),
('MO', 'MAC', 'Macau', 'Asia', FALSE),
('BN', 'BRN', 'Brunei', 'Asia', FALSE),
('BT', 'BTN', 'Bhutan', 'Asia', FALSE),
('MV', 'MDV', 'Maldives', 'Asia', FALSE),
('TW', 'TWN', 'Taiwan', 'Asia', FALSE),
('KG', 'KGZ', 'Kyrgyzstan', 'Asia', FALSE),
('TM', 'TKM', 'Turkmenistan', 'Asia', FALSE),

-- Africa
('NG', 'NGA', 'Nigeria', 'Africa', TRUE),
('ET', 'ETH', 'Ethiopia', 'Africa', TRUE),
('EG', 'EGY', 'Egypt', 'Africa', FALSE),
('CD', 'COD', 'DR Congo', 'Africa', FALSE),
('ZA', 'ZAF', 'South Africa', 'Africa', TRUE),
('TZ', 'TZA', 'Tanzania', 'Africa', FALSE),
('KE', 'KEN', 'Kenya', 'Africa', FALSE),
('UG', 'UGA', 'Uganda', 'Africa', FALSE),
('DZ', 'DZA', 'Algeria', 'Africa', FALSE),
('SD', 'SDN', 'Sudan', 'Africa', TRUE),
('MA', 'MAR', 'Morocco', 'Africa', FALSE),
('AO', 'AGO', 'Angola', 'Africa', FALSE),
('GH', 'GHA', 'Ghana', 'Africa', FALSE),
('MZ', 'MOZ', 'Mozambique', 'Africa', FALSE),
('MG', 'MDG', 'Madagascar', 'Africa', FALSE),
('CM', 'CMR', 'Cameroon', 'Africa', FALSE),
('CI', 'CIV', 'Ivory Coast', 'Africa', FALSE),
('NE', 'NER', 'Niger', 'Africa', FALSE),
('BF', 'BFA', 'Burkina Faso', 'Africa', FALSE),
('ML', 'MLI', 'Mali', 'Africa', FALSE),
('MW', 'MWI', 'Malawi', 'Africa', FALSE),
('ZM', 'ZMB', 'Zambia', 'Africa', FALSE),
('SN', 'SEN', 'Senegal', 'Africa', FALSE),
('SO', 'SOM', 'Somalia', 'Africa', TRUE),
('TD', 'TCD', 'Chad', 'Africa', FALSE),
('ZW', 'ZWE', 'Zimbabwe', 'Africa', FALSE),
('GN', 'GIN', 'Guinea', 'Africa', FALSE),
('RW', 'RWA', 'Rwanda', 'Africa', FALSE),
('BJ', 'BEN', 'Benin', 'Africa', FALSE),
('TN', 'TUN', 'Tunisia', 'Africa', FALSE),
('BI', 'BDI', 'Burundi', 'Africa', FALSE),
('SS', 'SSD', 'South Sudan', 'Africa', FALSE),
('TG', 'TGO', 'Togo', 'Africa', FALSE),
('LY', 'LBY', 'Libya', 'Africa', FALSE),
('SL', 'SLE', 'Sierra Leone', 'Africa', FALSE),
('LR', 'LBR', 'Liberia', 'Africa', FALSE),
('MR', 'MRT', 'Mauritania', 'Africa', FALSE),
('ER', 'ERI', 'Eritrea', 'Africa', FALSE),
('GM', 'GMB', 'Gambia', 'Africa', FALSE),
('BW', 'BWA', 'Botswana', 'Africa', FALSE),
('NA', 'NAM', 'Namibia', 'Africa', FALSE),
('GA', 'GAB', 'Gabon', 'Africa', FALSE),
('LS', 'LSO', 'Lesotho', 'Africa', FALSE),
('GW', 'GNB', 'Guinea-Bissau', 'Africa', FALSE),
('GQ', 'GNQ', 'Equatorial Guinea', 'Africa', FALSE),
('MU', 'MUS', 'Mauritius', 'Africa', FALSE),
('SZ', 'SWZ', 'Eswatini', 'Africa', FALSE),
('DJ', 'DJI', 'Djibouti', 'Africa', FALSE),
('KM', 'COM', 'Comoros', 'Africa', FALSE),
('CV', 'CPV', 'Cape Verde', 'Africa', FALSE),
('ST', 'STP', 'Sao Tome and Principe', 'Africa', FALSE),
('SC', 'SYC', 'Seychelles', 'Africa', FALSE),
('CF', 'CAF', 'Central African Republic', 'Africa', FALSE),
('CG', 'COG', 'Republic of the Congo', 'Africa', FALSE),

-- Oceania
('AU', 'AUS', 'Australia', 'Oceania', TRUE),
('PG', 'PNG', 'Papua New Guinea', 'Oceania', FALSE),
('NZ', 'NZL', 'New Zealand', 'Oceania', FALSE),
('FJ', 'FJI', 'Fiji', 'Oceania', FALSE),
('SB', 'SLB', 'Solomon Islands', 'Oceania', FALSE),
('VU', 'VUT', 'Vanuatu', 'Oceania', FALSE),
('WS', 'WSM', 'Samoa', 'Oceania', FALSE),
('KI', 'KIR', 'Kiribati', 'Oceania', FALSE),
('TO', 'TON', 'Tonga', 'Oceania', FALSE),
('FM', 'FSM', 'Micronesia', 'Oceania', TRUE),
('PW', 'PLW', 'Palau', 'Oceania', FALSE),
('MH', 'MHL', 'Marshall Islands', 'Oceania', FALSE),
('TV', 'TUV', 'Tuvalu', 'Oceania', FALSE),
('NR', 'NRU', 'Nauru', 'Oceania', FALSE),
('CK', 'COK', 'Cook Islands', 'Oceania', FALSE),
('NU', 'NIU', 'Niue', 'Oceania', FALSE),
('TK', 'TKL', 'Tokelau', 'Oceania', FALSE),
('GU', 'GUM', 'Guam', 'Oceania', FALSE),
('PF', 'PYF', 'French Polynesia', 'Oceania', FALSE),
('NC', 'NCL', 'New Caledonia', 'Oceania', FALSE);

-- Add some major states for key countries
-- United States
INSERT INTO states (country_id, code, name)
SELECT id, 'AL', 'Alabama' FROM countries WHERE code = 'US'
UNION SELECT id, 'AK', 'Alaska' FROM countries WHERE code = 'US'
UNION SELECT id, 'AZ', 'Arizona' FROM countries WHERE code = 'US'
UNION SELECT id, 'AR', 'Arkansas' FROM countries WHERE code = 'US'
UNION SELECT id, 'CA', 'California' FROM countries WHERE code = 'US'
UNION SELECT id, 'CO', 'Colorado' FROM countries WHERE code = 'US'
UNION SELECT id, 'CT', 'Connecticut' FROM countries WHERE code = 'US'
UNION SELECT id, 'DE', 'Delaware' FROM countries WHERE code = 'US'
UNION SELECT id, 'FL', 'Florida' FROM countries WHERE code = 'US'
UNION SELECT id, 'GA', 'Georgia' FROM countries WHERE code = 'US'
UNION SELECT id, 'HI', 'Hawaii' FROM countries WHERE code = 'US'
UNION SELECT id, 'ID', 'Idaho' FROM countries WHERE code = 'US'
UNION SELECT id, 'IL', 'Illinois' FROM countries WHERE code = 'US'
UNION SELECT id, 'IN', 'Indiana' FROM countries WHERE code = 'US'
UNION SELECT id, 'IA', 'Iowa' FROM countries WHERE code = 'US'
UNION SELECT id, 'KS', 'Kansas' FROM countries WHERE code = 'US'
UNION SELECT id, 'KY', 'Kentucky' FROM countries WHERE code = 'US'
UNION SELECT id, 'LA', 'Louisiana' FROM countries WHERE code = 'US'
UNION SELECT id, 'ME', 'Maine' FROM countries WHERE code = 'US'
UNION SELECT id, 'MD', 'Maryland' FROM countries WHERE code = 'US'
UNION SELECT id, 'MA', 'Massachusetts' FROM countries WHERE code = 'US'
UNION SELECT id, 'MI', 'Michigan' FROM countries WHERE code = 'US'
UNION SELECT id, 'MN', 'Minnesota' FROM countries WHERE code = 'US'
UNION SELECT id, 'MS', 'Mississippi' FROM countries WHERE code = 'US'
UNION SELECT id, 'MO', 'Missouri' FROM countries WHERE code = 'US'
UNION SELECT id, 'MT', 'Montana' FROM countries WHERE code = 'US'
UNION SELECT id, 'NE', 'Nebraska' FROM countries WHERE code = 'US'
UNION SELECT id, 'NV', 'Nevada' FROM countries WHERE code = 'US'
UNION SELECT id, 'NH', 'New Hampshire' FROM countries WHERE code = 'US'
UNION SELECT id, 'NJ', 'New Jersey' FROM countries WHERE code = 'US'
UNION SELECT id, 'NM', 'New Mexico' FROM countries WHERE code = 'US'
UNION SELECT id, 'NY', 'New York' FROM countries WHERE code = 'US'
UNION SELECT id, 'NC', 'North Carolina' FROM countries WHERE code = 'US'
UNION SELECT id, 'ND', 'North Dakota' FROM countries WHERE code = 'US'
UNION SELECT id, 'OH', 'Ohio' FROM countries WHERE code = 'US'
UNION SELECT id, 'OK', 'Oklahoma' FROM countries WHERE code = 'US'
UNION SELECT id, 'OR', 'Oregon' FROM countries WHERE code = 'US'
UNION SELECT id, 'PA', 'Pennsylvania' FROM countries WHERE code = 'US'
UNION SELECT id, 'RI', 'Rhode Island' FROM countries WHERE code = 'US'
UNION SELECT id, 'SC', 'South Carolina' FROM countries WHERE code = 'US'
UNION SELECT id, 'SD', 'South Dakota' FROM countries WHERE code = 'US'
UNION SELECT id, 'TN', 'Tennessee' FROM countries WHERE code = 'US'
UNION SELECT id, 'TX', 'Texas' FROM countries WHERE code = 'US'
UNION SELECT id, 'UT', 'Utah' FROM countries WHERE code = 'US'
UNION SELECT id, 'VT', 'Vermont' FROM countries WHERE code = 'US'
UNION SELECT id, 'VA', 'Virginia' FROM countries WHERE code = 'US'
UNION SELECT id, 'WA', 'Washington' FROM countries WHERE code = 'US'
UNION SELECT id, 'WV', 'West Virginia' FROM countries WHERE code = 'US'
UNION SELECT id, 'WI', 'Wisconsin' FROM countries WHERE code = 'US'
UNION SELECT id, 'WY', 'Wyoming' FROM countries WHERE code = 'US'
UNION SELECT id, 'DC', 'Washington DC' FROM countries WHERE code = 'US';

-- Canada provinces
INSERT INTO states (country_id, code, name)
SELECT id, 'AB', 'Alberta' FROM countries WHERE code = 'CA'
UNION SELECT id, 'BC', 'British Columbia' FROM countries WHERE code = 'CA'
UNION SELECT id, 'MB', 'Manitoba' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NB', 'New Brunswick' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NL', 'Newfoundland and Labrador' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NT', 'Northwest Territories' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NS', 'Nova Scotia' FROM countries WHERE code = 'CA'
UNION SELECT id, 'NU', 'Nunavut' FROM countries WHERE code = 'CA'
UNION SELECT id, 'ON', 'Ontario' FROM countries WHERE code = 'CA'
UNION SELECT id, 'PE', 'Prince Edward Island' FROM countries WHERE code = 'CA'
UNION SELECT id, 'QC', 'Quebec' FROM countries WHERE code = 'CA'
UNION SELECT id, 'SK', 'Saskatchewan' FROM countries WHERE code = 'CA'
UNION SELECT id, 'YT', 'Yukon' FROM countries WHERE code = 'CA';

-- India states
INSERT INTO states (country_id, code, name)
SELECT id, 'AP', 'Andhra Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'AR', 'Arunachal Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'AS', 'Assam' FROM countries WHERE code = 'IN'
UNION SELECT id, 'BR', 'Bihar' FROM countries WHERE code = 'IN'
UNION SELECT id, 'CT', 'Chhattisgarh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'GA', 'Goa' FROM countries WHERE code = 'IN'
UNION SELECT id, 'GJ', 'Gujarat' FROM countries WHERE code = 'IN'
UNION SELECT id, 'HR', 'Haryana' FROM countries WHERE code = 'IN'
UNION SELECT id, 'HP', 'Himachal Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'JK', 'Jammu and Kashmir' FROM countries WHERE code = 'IN'
UNION SELECT id, 'JH', 'Jharkhand' FROM countries WHERE code = 'IN'
UNION SELECT id, 'KA', 'Karnataka' FROM countries WHERE code = 'IN'
UNION SELECT id, 'KL', 'Kerala' FROM countries WHERE code = 'IN'
UNION SELECT id, 'MP', 'Madhya Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'MH', 'Maharashtra' FROM countries WHERE code = 'IN'
UNION SELECT id, 'MN', 'Manipur' FROM countries WHERE code = 'IN'
UNION SELECT id, 'ML', 'Meghalaya' FROM countries WHERE code = 'IN'
UNION SELECT id, 'MZ', 'Mizoram' FROM countries WHERE code = 'IN'
UNION SELECT id, 'NL', 'Nagaland' FROM countries WHERE code = 'IN'
UNION SELECT id, 'OR', 'Odisha' FROM countries WHERE code = 'IN'
UNION SELECT id, 'PB', 'Punjab' FROM countries WHERE code = 'IN'
UNION SELECT id, 'RJ', 'Rajasthan' FROM countries WHERE code = 'IN'
UNION SELECT id, 'SK', 'Sikkim' FROM countries WHERE code = 'IN'
UNION SELECT id, 'TN', 'Tamil Nadu' FROM countries WHERE code = 'IN'
UNION SELECT id, 'TG', 'Telangana' FROM countries WHERE code = 'IN'
UNION SELECT id, 'TR', 'Tripura' FROM countries WHERE code = 'IN'
UNION SELECT id, 'UP', 'Uttar Pradesh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'UT', 'Uttarakhand' FROM countries WHERE code = 'IN'
UNION SELECT id, 'WB', 'West Bengal' FROM countries WHERE code = 'IN'
UNION SELECT id, 'AN', 'Andaman and Nicobar Islands' FROM countries WHERE code = 'IN'
UNION SELECT id, 'CH', 'Chandigarh' FROM countries WHERE code = 'IN'
UNION SELECT id, 'DN', 'Dadra and Nagar Haveli' FROM countries WHERE code = 'IN'
UNION SELECT id, 'DD', 'Daman and Diu' FROM countries WHERE code = 'IN'
UNION SELECT id, 'DL', 'Delhi' FROM countries WHERE code = 'IN'
UNION SELECT id, 'LD', 'Lakshadweep' FROM countries WHERE code = 'IN'
UNION SELECT id, 'PY', 'Puducherry' FROM countries WHERE code = 'IN';

-- Australia states/territories
INSERT INTO states (country_id, code, name)
SELECT id, 'NSW', 'New South Wales' FROM countries WHERE code = 'AU'
UNION SELECT id, 'VIC', 'Victoria' FROM countries WHERE code = 'AU'
UNION SELECT id, 'QLD', 'Queensland' FROM countries WHERE code = 'AU'
UNION SELECT id, 'WA', 'Western Australia' FROM countries WHERE code = 'AU'
UNION SELECT id, 'SA', 'South Australia' FROM countries WHERE code = 'AU'
UNION SELECT id, 'TAS', 'Tasmania' FROM countries WHERE code = 'AU'
UNION SELECT id, 'ACT', 'Australian Capital Territory' FROM countries WHERE code = 'AU'
UNION SELECT id, 'NT', 'Northern Territory' FROM countries WHERE code = 'AU';

-- Add major cities for important countries
-- US Cities
INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'New York City', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'NY';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Los Angeles', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'San Francisco', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Chicago', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'IL';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Houston', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Dallas', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Miami', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'FL';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Atlanta', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'GA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Seattle', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'WA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Boston', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'MA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Phoenix', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'AZ';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Denver', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CO';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Las Vegas', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'NV';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Portland', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'OR';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'San Diego', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'San Jose', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'CA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Austin', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'TX';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Ashburn', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'VA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Washington', TRUE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'US' AND s.code = 'DC';

-- Cities without states (capitals and major cities)
INSERT INTO cities (country_id, name, is_capital) VALUES
((SELECT id FROM countries WHERE code = 'GB'), 'London', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Manchester', FALSE),
((SELECT id FROM countries WHERE code = 'GB'), 'Birmingham', FALSE),
((SELECT id FROM countries WHERE code = 'GB'), 'Edinburgh', FALSE),
((SELECT id FROM countries WHERE code = 'GB'), 'Glasgow', FALSE),
((SELECT id FROM countries WHERE code = 'FR'), 'Paris', TRUE),
((SELECT id FROM countries WHERE code = 'FR'), 'Marseille', FALSE),
((SELECT id FROM countries WHERE code = 'FR'), 'Lyon', FALSE),
((SELECT id FROM countries WHERE code = 'DE'), 'Berlin', TRUE),
((SELECT id FROM countries WHERE code = 'DE'), 'Frankfurt', FALSE),
((SELECT id FROM countries WHERE code = 'DE'), 'Munich', FALSE),
((SELECT id FROM countries WHERE code = 'DE'), 'Hamburg', FALSE),
((SELECT id FROM countries WHERE code = 'IT'), 'Rome', TRUE),
((SELECT id FROM countries WHERE code = 'IT'), 'Milan', FALSE),
((SELECT id FROM countries WHERE code = 'IT'), 'Naples', FALSE),
((SELECT id FROM countries WHERE code = 'ES'), 'Madrid', TRUE),
((SELECT id FROM countries WHERE code = 'ES'), 'Barcelona', FALSE),
((SELECT id FROM countries WHERE code = 'ES'), 'Valencia', FALSE),
((SELECT id FROM countries WHERE code = 'NL'), 'Amsterdam', TRUE),
((SELECT id FROM countries WHERE code = 'NL'), 'Rotterdam', FALSE),
((SELECT id FROM countries WHERE code = 'BE'), 'Brussels', TRUE),
((SELECT id FROM countries WHERE code = 'CH'), 'Zurich', FALSE),
((SELECT id FROM countries WHERE code = 'CH'), 'Geneva', FALSE),
((SELECT id FROM countries WHERE code = 'CH'), 'Bern', TRUE),
((SELECT id FROM countries WHERE code = 'AT'), 'Vienna', TRUE),
((SELECT id FROM countries WHERE code = 'SE'), 'Stockholm', TRUE),
((SELECT id FROM countries WHERE code = 'NO'), 'Oslo', TRUE),
((SELECT id FROM countries WHERE code = 'DK'), 'Copenhagen', TRUE),
((SELECT id FROM countries WHERE code = 'FI'), 'Helsinki', TRUE),
((SELECT id FROM countries WHERE code = 'PL'), 'Warsaw', TRUE),
((SELECT id FROM countries WHERE code = 'CZ'), 'Prague', TRUE),
((SELECT id FROM countries WHERE code = 'HU'), 'Budapest', TRUE),
((SELECT id FROM countries WHERE code = 'RO'), 'Bucharest', TRUE),
((SELECT id FROM countries WHERE code = 'GR'), 'Athens', TRUE),
((SELECT id FROM countries WHERE code = 'PT'), 'Lisbon', TRUE),
((SELECT id FROM countries WHERE code = 'PT'), 'Porto', FALSE),
((SELECT id FROM countries WHERE code = 'IE'), 'Dublin', TRUE),
((SELECT id FROM countries WHERE code = 'RU'), 'Moscow', TRUE),
((SELECT id FROM countries WHERE code = 'RU'), 'St. Petersburg', FALSE),
((SELECT id FROM countries WHERE code = 'UA'), 'Kyiv', TRUE),
((SELECT id FROM countries WHERE code = 'TR'), 'Istanbul', FALSE),
((SELECT id FROM countries WHERE code = 'TR'), 'Ankara', TRUE),
((SELECT id FROM countries WHERE code = 'EG'), 'Cairo', TRUE),
((SELECT id FROM countries WHERE code = 'ZA'), 'Johannesburg', FALSE),
((SELECT id FROM countries WHERE code = 'ZA'), 'Cape Town', FALSE),
((SELECT id FROM countries WHERE code = 'ZA'), 'Pretoria', TRUE),
((SELECT id FROM countries WHERE code = 'NG'), 'Lagos', FALSE),
((SELECT id FROM countries WHERE code = 'NG'), 'Abuja', TRUE),
((SELECT id FROM countries WHERE code = 'KE'), 'Nairobi', TRUE),
((SELECT id FROM countries WHERE code = 'MA'), 'Casablanca', FALSE),
((SELECT id FROM countries WHERE code = 'MA'), 'Rabat', TRUE),
((SELECT id FROM countries WHERE code = 'JP'), 'Tokyo', TRUE),
((SELECT id FROM countries WHERE code = 'JP'), 'Osaka', FALSE),
((SELECT id FROM countries WHERE code = 'JP'), 'Kyoto', FALSE),
((SELECT id FROM countries WHERE code = 'JP'), 'Yokohama', FALSE),
((SELECT id FROM countries WHERE code = 'CN'), 'Beijing', TRUE),
((SELECT id FROM countries WHERE code = 'CN'), 'Shanghai', FALSE),
((SELECT id FROM countries WHERE code = 'CN'), 'Shenzhen', FALSE),
((SELECT id FROM countries WHERE code = 'CN'), 'Guangzhou', FALSE),
((SELECT id FROM countries WHERE code = 'CN'), 'Hong Kong', FALSE),
((SELECT id FROM countries WHERE code = 'KR'), 'Seoul', TRUE),
((SELECT id FROM countries WHERE code = 'KR'), 'Busan', FALSE),
((SELECT id FROM countries WHERE code = 'TW'), 'Taipei', TRUE),
((SELECT id FROM countries WHERE code = 'TH'), 'Bangkok', TRUE),
((SELECT id FROM countries WHERE code = 'VN'), 'Ho Chi Minh City', FALSE),
((SELECT id FROM countries WHERE code = 'VN'), 'Hanoi', TRUE),
((SELECT id FROM countries WHERE code = 'SG'), 'Singapore', TRUE),
((SELECT id FROM countries WHERE code = 'MY'), 'Kuala Lumpur', TRUE),
((SELECT id FROM countries WHERE code = 'ID'), 'Jakarta', TRUE),
((SELECT id FROM countries WHERE code = 'PH'), 'Manila', TRUE),
((SELECT id FROM countries WHERE code = 'AE'), 'Dubai', FALSE),
((SELECT id FROM countries WHERE code = 'AE'), 'Abu Dhabi', TRUE),
((SELECT id FROM countries WHERE code = 'SA'), 'Riyadh', TRUE),
((SELECT id FROM countries WHERE code = 'SA'), 'Jeddah', FALSE),
((SELECT id FROM countries WHERE code = 'IL'), 'Tel Aviv', FALSE),
((SELECT id FROM countries WHERE code = 'IL'), 'Jerusalem', TRUE),
((SELECT id FROM countries WHERE code = 'QA'), 'Doha', TRUE),
((SELECT id FROM countries WHERE code = 'KW'), 'Kuwait City', TRUE),
((SELECT id FROM countries WHERE code = 'BH'), 'Manama', TRUE),
((SELECT id FROM countries WHERE code = 'OM'), 'Muscat', TRUE),
((SELECT id FROM countries WHERE code = 'JO'), 'Amman', TRUE),
((SELECT id FROM countries WHERE code = 'LB'), 'Beirut', TRUE),
((SELECT id FROM countries WHERE code = 'PK'), 'Karachi', FALSE),
((SELECT id FROM countries WHERE code = 'PK'), 'Islamabad', TRUE),
((SELECT id FROM countries WHERE code = 'BD'), 'Dhaka', TRUE),
((SELECT id FROM countries WHERE code = 'LK'), 'Colombo', TRUE),
((SELECT id FROM countries WHERE code = 'NZ'), 'Auckland', FALSE),
((SELECT id FROM countries WHERE code = 'NZ'), 'Wellington', TRUE),
((SELECT id FROM countries WHERE code = 'MX'), 'Mexico City', TRUE),
((SELECT id FROM countries WHERE code = 'MX'), 'Guadalajara', FALSE),
((SELECT id FROM countries WHERE code = 'BR'), 'São Paulo', FALSE),
((SELECT id FROM countries WHERE code = 'BR'), 'Rio de Janeiro', FALSE),
((SELECT id FROM countries WHERE code = 'BR'), 'Brasília', TRUE),
((SELECT id FROM countries WHERE code = 'AR'), 'Buenos Aires', TRUE),
((SELECT id FROM countries WHERE code = 'CL'), 'Santiago', TRUE),
((SELECT id FROM countries WHERE code = 'CO'), 'Bogotá', TRUE),
((SELECT id FROM countries WHERE code = 'PE'), 'Lima', TRUE),
((SELECT id FROM countries WHERE code = 'VE'), 'Caracas', TRUE),
((SELECT id FROM countries WHERE code = 'EC'), 'Quito', TRUE),
((SELECT id FROM countries WHERE code = 'UY'), 'Montevideo', TRUE);

-- Add cities for Canada
INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Toronto', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'ON';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Montreal', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'QC';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Vancouver', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'BC';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Calgary', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'AB';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Ottawa', TRUE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'CA' AND s.code = 'ON';

-- Add cities for India
INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Mumbai', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'MH';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Bangalore', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'KA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Chennai', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'TN';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'New Delhi', TRUE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'DL';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Hyderabad', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'TG';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Pune', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'MH';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Kolkata', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'IN' AND s.code = 'WB';

-- Add cities for Australia
INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Sydney', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'NSW';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Melbourne', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'VIC';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Brisbane', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'QLD';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Perth', FALSE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'WA';

INSERT INTO cities (country_id, state_id, name, is_capital)
SELECT c.id, s.id, 'Canberra', TRUE
FROM countries c JOIN states s ON c.id = s.country_id WHERE c.code = 'AU' AND s.code = 'ACT';

SELECT 'Data population completed!' as status;
SELECT COUNT(*) as total_countries FROM countries;
SELECT COUNT(*) as total_states FROM states;
SELECT COUNT(*) as total_cities FROM cities;