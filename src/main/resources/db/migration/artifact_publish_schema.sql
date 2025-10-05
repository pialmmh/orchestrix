-- Artifact Publishing Schema
-- Database: orchestrix
-- Purpose: Track LXC container artifacts and their publish locations

-- Table: publish_location
-- Stores publish location configurations (Google Drive via rclone)
CREATE TABLE IF NOT EXISTS publish_location (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,                   -- e.g., "Production GDrive"
    type ENUM('gdrive') NOT NULL,                 -- Currently only Google Drive with rclone
    rclone_remote VARCHAR(100) NOT NULL,          -- Rclone remote name (e.g., "gdrive")
    rclone_target_dir VARCHAR(500),               -- Target directory (e.g., "lxc-containers/go-id")
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rclone_remote (rclone_remote),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Publish location configurations';

-- Table: artifact
-- Stores artifact metadata (LXC container images)
CREATE TABLE IF NOT EXISTS artifact (
    id INT PRIMARY KEY AUTO_INCREMENT,
    artifact_type VARCHAR(50) NOT NULL,           -- e.g., 'lxc-container'
    name VARCHAR(200) NOT NULL,                   -- e.g., 'go-id'
    version VARCHAR(50) NOT NULL,                 -- e.g., 'v1'
    file_name VARCHAR(500) NOT NULL,              -- e.g., 'go-id-v1-1759653102.tar.gz'
    file_size_bytes BIGINT,                       -- File size in bytes
    md5_hash VARCHAR(32) NOT NULL,                -- MD5 hash for verification
    local_path VARCHAR(1000),                     -- Local generated/ folder path
    build_timestamp BIGINT NOT NULL,              -- Unix timestamp of build
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_artifact_type (artifact_type),
    INDEX idx_name_version (name, version),
    INDEX idx_build_timestamp (build_timestamp),
    UNIQUE KEY uk_file_name (file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Artifact metadata';

-- Table: artifact_publish
-- Tracks artifact publishes to remote locations
CREATE TABLE IF NOT EXISTS artifact_publish (
    id INT PRIMARY KEY AUTO_INCREMENT,
    artifact_id INT NOT NULL,
    publish_location_id INT NOT NULL,
    remote_path VARCHAR(1000) NOT NULL,           -- Full path in remote storage
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE,                -- MD5 verified after upload
    verification_timestamp TIMESTAMP NULL,         -- When MD5 verification completed
    FOREIGN KEY (artifact_id) REFERENCES artifact(id) ON DELETE CASCADE,
    FOREIGN KEY (publish_location_id) REFERENCES publish_location(id) ON DELETE RESTRICT,
    UNIQUE KEY uk_artifact_location (artifact_id, publish_location_id),
    INDEX idx_published_at (published_at),
    INDEX idx_verified (verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Artifact publish records';

-- Insert default Google Drive publish location (example)
INSERT IGNORE INTO publish_location (name, type, rclone_remote, rclone_target_dir, is_active)
VALUES ('Default Google Drive', 'gdrive', 'gdrive', 'lxc-containers', TRUE);
