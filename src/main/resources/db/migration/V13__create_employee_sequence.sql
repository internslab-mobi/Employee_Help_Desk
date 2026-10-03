-- Create sequence table for employee code generation
-- This ensures thread-safe concurrent employee creation
CREATE TABLE hd_sequences (
    name VARCHAR(50) PRIMARY KEY,
    next_value BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Initialize employee sequence
-- Start from 6 since existing employees have codes EMP001-EMP005
INSERT INTO hd_sequences (name, next_value) VALUES ('employee_code', 6);
