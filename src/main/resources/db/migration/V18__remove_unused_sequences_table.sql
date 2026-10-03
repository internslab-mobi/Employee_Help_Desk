-- Remove the unused hd_sequences table.
-- Employee codes are now derived from Employee.id (EMP + formatted id).
-- This table and its data are no longer referenced by any application code.
DROP TABLE IF EXISTS hd_sequences;
