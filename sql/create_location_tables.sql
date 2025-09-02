-- Create database if not exists
CREATE DATABASE IF NOT EXISTS orchestrix;
USE orchestrix;

-- Drop existing tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS cities;
DROP TABLE IF EXISTS states;
DROP TABLE IF EXISTS countries;

-- Create countries table
CREATE TABLE countries (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(2) NOT NULL UNIQUE,
    code3 VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    has_states BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_country_code (code),
    INDEX idx_country_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create states table
CREATE TABLE states (
    id INT PRIMARY KEY AUTO_INCREMENT,
    country_id INT NOT NULL,
    code VARCHAR(10),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
    INDEX idx_state_country (country_id),
    INDEX idx_state_name (name),
    UNIQUE KEY unique_state_country (country_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create cities table
CREATE TABLE cities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    country_id INT NOT NULL,
    state_id INT,
    name VARCHAR(100) NOT NULL,
    is_capital BOOLEAN DEFAULT FALSE,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
    FOREIGN KEY (state_id) REFERENCES states(id) ON DELETE SET NULL,
    INDEX idx_city_country (country_id),
    INDEX idx_city_state (state_id),
    INDEX idx_city_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create datacenters table with proper foreign keys
CREATE TABLE IF NOT EXISTS datacenters (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    country_id INT,
    state_id INT,
    city_id INT,
    location_other VARCHAR(255),
    type VARCHAR(50),
    status VARCHAR(50),
    provider VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    servers INT DEFAULT 0,
    storage_tb INT DEFAULT 0,
    utilization INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE SET NULL,
    FOREIGN KEY (state_id) REFERENCES states(id) ON DELETE SET NULL,
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE SET NULL,
    INDEX idx_datacenter_location (country_id, state_id, city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;