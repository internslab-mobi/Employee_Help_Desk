-- Add NOT NULL constraint to timezone column
-- First, backfill any NULL timezone values with UTC as a safe default
UPDATE hd_employees 
SET timezone = 'UTC' 
WHERE timezone IS NULL OR timezone = '';

-- Then make the timezone column NOT NULL
ALTER TABLE hd_employees 
MODIFY COLUMN timezone VARCHAR(50) NOT NULL;
