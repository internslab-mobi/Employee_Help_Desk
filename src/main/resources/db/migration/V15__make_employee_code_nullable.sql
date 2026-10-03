-- Make employee_code nullable to allow two-step save process
-- This migration ensures the column can be NULL during employee creation
ALTER TABLE hd_employees MODIFY COLUMN employee_code VARCHAR(50) NULL;
