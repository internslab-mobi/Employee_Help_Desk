CREATE TABLE hd_refresh_tokens (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   token VARCHAR(500) NOT NULL UNIQUE,
                                   employee_id BIGINT NOT NULL,
                                   expiry_date DATETIME NOT NULL,
                                   refresh_count INT NOT NULL DEFAULT 0,
                                   created_at DATETIME NOT NULL,
                                   updated_at DATETIME NOT NULL,
                                   FOREIGN KEY (employee_id) REFERENCES hd_employees(id) ON DELETE CASCADE,
                                   INDEX idx_employee_id (employee_id)
);