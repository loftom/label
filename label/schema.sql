CREATE DATABASE IF NOT EXISTS label_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE label_system;

CREATE TABLE patients (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_no VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128),
  gender ENUM('M','F','U') DEFAULT 'U',
  dob DATE,
  contact VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(512) NOT NULL,
  modality VARCHAR(64),
  study_uid VARCHAR(128),
  series_uid VARCHAR(128),
  instance_number INT,
  uploaded_by VARCHAR(64),
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE annotations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  image_id BIGINT NOT NULL,
  annotator VARCHAR(64),
  json_body JSON NOT NULL,
  label_summary VARCHAR(512),
  version INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
