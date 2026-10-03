-- Make employee_code nullable to allow two-step save process
ALTER TABLE hd_employees MODIFY COLUMN employee_code VARCHAR(50) NULL;

-- Update existing employee codes to match their IDs
-- This ensures employeeCode follows the pattern EMP### where ### is the employee ID
UPDATE hd_employees SET employee_code = CONCAT('EMP', LPAD(id, 3, '0')) WHERE employee_code IS NULL OR employee_code != CONCAT('EMP', LPAD(id, 3, '0'));
