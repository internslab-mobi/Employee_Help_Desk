-- Update existing employees with test passwords and roles
-- BCrypt hashes will be set by application initializer on first startup
-- This migration sets temporary values that will be replaced

-- Ravi Kumar (ID 1) - EMPLOYEE
UPDATE hd_employees 
SET password_hash = 'TEMP_HASH', role = 'EMPLOYEE'
WHERE id = 1 AND employee_code = 'EMP001';

-- Priya Sharma (ID 2) - EMPLOYEE  
UPDATE hd_employees 
SET password_hash = 'TEMP_HASH', role = 'EMPLOYEE'
WHERE id = 2 AND employee_code = 'EMP002';

-- Arun Raj (ID 3) - AGENT
UPDATE hd_employees 
SET password_hash = 'TEMP_HASH', role = 'AGENT'
WHERE id = 3 AND employee_code = 'EMP003';

-- Meena Joseph (ID 4) - MANAGER
UPDATE hd_employees 
SET password_hash = 'TEMP_HASH', role = 'MANAGER'
WHERE id = 4 AND employee_code = 'EMP004';

-- Vikram Singh (ID 5) - ADMIN
UPDATE hd_employees 
SET password_hash = 'TEMP_HASH', role = 'ADMIN'
WHERE id = 5 AND employee_code = 'EMP005';

-- Make columns NOT NULL after data is populated
ALTER TABLE hd_employees 
MODIFY COLUMN password_hash VARCHAR(255) NOT NULL,
MODIFY COLUMN role VARCHAR(30) NOT NULL;
