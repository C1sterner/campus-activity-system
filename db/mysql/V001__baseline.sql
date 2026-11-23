-- Campus activity registration/review baseline schema.
-- Run only in a new isolated development database. Review D14/D17 before production use.
SET NAMES utf8mb4;
SET time_zone = '+08:00';

CREATE TABLE IF NOT EXISTS sys_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  user_name VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50) NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'student',
  email VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NULL,
  status CHAR(1) NOT NULL DEFAULT '0',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  login_ip VARCHAR(128) NULL,
  login_date DATETIME NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_name (user_name),
  UNIQUE KEY uk_email (email),
  KEY idx_user_role (role),
  CONSTRAINT ck_user_role CHECK (role IN ('student','activity_admin','teacher','admin')),
  CONSTRAINT ck_user_status CHECK (status IN ('0','1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS activity (
  activity_id INT NOT NULL AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  form_schema JSON NULL,
  reviewer_id BIGINT NULL,
  status CHAR(1) NOT NULL DEFAULT '0',
  creator_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (activity_id),
  KEY idx_activity_reviewer (reviewer_id),
  KEY idx_activity_creator (creator_id),
  KEY idx_activity_status_time (status, start_time, end_time),
  CONSTRAINT fk_activity_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(user_id),
  CONSTRAINT fk_activity_creator FOREIGN KEY (creator_id) REFERENCES sys_user(user_id),
  CONSTRAINT ck_activity_status CHECK (status IN ('0','1','2')),
  CONSTRAINT ck_activity_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS registration (
  reg_id INT NOT NULL AUTO_INCREMENT,
  activity_id INT NOT NULL,
  user_id BIGINT NOT NULL,
  form_data JSON NOT NULL,
  attachment_path VARCHAR(255) NULL,
  status CHAR(1) NOT NULL DEFAULT '0',
  version INT NOT NULL DEFAULT 0,
  submit_time DATETIME NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reg_id),
  UNIQUE KEY uk_activity_user (activity_id, user_id),
  KEY idx_registration_user (user_id),
  KEY idx_registration_status (status),
  CONSTRAINT fk_registration_activity FOREIGN KEY (activity_id) REFERENCES activity(activity_id),
  CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
  CONSTRAINT ck_registration_status CHECK (status IN ('0','1','2','3')),
  CONSTRAINT ck_registration_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS review_record (
  review_id INT NOT NULL AUTO_INCREMENT,
  reg_id INT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  result CHAR(1) NOT NULL,
  opinion VARCHAR(500) NULL,
  review_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (review_id),
  KEY idx_review_reg (reg_id),
  CONSTRAINT fk_review_reg FOREIGN KEY (reg_id) REFERENCES registration(reg_id),
  CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(user_id),
  CONSTRAINT ck_review_result CHECK (result IN ('1','2')),
  CONSTRAINT ck_review_reject_opinion CHECK (result <> '2' OR CHAR_LENGTH(TRIM(opinion)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_config (
  config_id INT NOT NULL AUTO_INCREMENT,
  config_key VARCHAR(100) NOT NULL,
  config_value VARCHAR(500) NOT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS operation_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  op_type VARCHAR(50) NOT NULL,
  op_content VARCHAR(1000) NULL,
  op_ip VARCHAR(128) NULL,
  op_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_log_operator_time (operator_id, op_time),
  CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
