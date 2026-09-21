-- Add authentication columns to hd_employees table (nullable initially for existing data)
ALTER TABLE hd_employees 
ADD COLUMN password_hash VARCHAR(255),
ADD COLUMN role VARCHAR(30);
