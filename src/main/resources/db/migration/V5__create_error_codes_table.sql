CREATE TABLE hd_error_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    http_status INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_error_codes_code UNIQUE (code)
);

INSERT INTO hd_error_codes (code, message, description, http_status, is_active, created_at, updated_at) VALUES
('ER-001', 'Resource not found', 'Resource not found error', 404, TRUE, NOW(), NOW()),
('ER-002', 'Validation failed', 'Validation failed error', 400, TRUE, NOW(), NOW()),
('ER-003', 'Duplicate resource', 'Duplicate resource error', 409, TRUE, NOW(), NOW()),
('ER-004', 'Authentication failed', 'Authentication failed error', 401, TRUE, NOW(), NOW()),
('ER-005', 'Access denied', 'Access denied error', 403, TRUE, NOW(), NOW()),
('ER-006', 'Invalid request', 'Invalid request error', 400, TRUE, NOW(), NOW()),
('ER-007', 'Business rule violation', 'Business rule violation error', 400, TRUE, NOW(), NOW()),
('ER-008', 'Database/data integrity error', 'Database/data integrity error', 500, TRUE, NOW(), NOW()),
('ER-009', 'JWT/token error', 'JWT/token error', 401, TRUE, NOW(), NOW()),
('ER-010', 'Internal server error', 'Internal server error', 500, TRUE, NOW(), NOW());
