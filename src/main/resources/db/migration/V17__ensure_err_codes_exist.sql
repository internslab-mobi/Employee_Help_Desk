-- Ensure ERR_ error codes exist and are properly formatted
-- This migration is idempotent and safe to run even if V7 was partially applied
-- It uses INSERT IGNORE to avoid duplicate key errors

-- Ensure core ERR_ codes exist (these are used by GlobalExceptionController)
INSERT IGNORE INTO hd_error_codes (code, message, description, http_status, is_active, created_at, updated_at) VALUES
('ERR_001', 'Resource not found', 'Resource not found error', 404, TRUE, NOW(), NOW()),
('ERR_002', 'Validation failed', 'Validation failed error', 400, TRUE, NOW(), NOW()),
('ERR_003', 'Duplicate resource', 'Duplicate resource error', 409, TRUE, NOW(), NOW()),
('ERR_004', 'Authentication failed', 'Authentication failed error', 401, TRUE, NOW(), NOW()),
('ERR_005', 'Access denied', 'Access denied error', 403, TRUE, NOW(), NOW()),
('ERR_006', 'Invalid request', 'Invalid request error', 400, TRUE, NOW(), NOW()),
('ERR_007', 'Business rule violation', 'Business rule violation error', 400, TRUE, NOW(), NOW()),
('ERR_008', 'Database/data integrity error', 'Database/data integrity error', 500, TRUE, NOW(), NOW()),
('ERR_009', 'JWT/token error', 'JWT/token error', 401, TRUE, NOW(), NOW()),
('ERR_010', 'Internal server error', 'Internal server error', 500, TRUE, NOW(), NOW()),
('ERR_011', 'Ticket operation not allowed', 'Invalid ticket status transition or operation', 400, TRUE, NOW(), NOW()),
('ERR_012', 'SLA operation failed', 'Invalid SLA operation or calculation failure', 400, TRUE, NOW(), NOW()),
('ERR_013', 'Attachment error', 'Invalid attachment, unsupported type, or size violation', 400, TRUE, NOW(), NOW()),
('ERR_014', 'Message operation failed', 'Invalid ticket message operation', 400, TRUE, NOW(), NOW()),
('ERR_015', 'Notification error', 'Notification processing failure', 500, TRUE, NOW(), NOW()),
('ERR_016', 'Email sending failed', 'Email notification could not be sent', 500, TRUE, NOW(), NOW()),
('ERR_017', 'Refresh token error', 'Invalid, expired, revoked refresh token or refresh limit exceeded', 401, TRUE, NOW(), NOW()),
('ERR_018', 'Feedback operation failed', 'Invalid feedback or reopen feedback operation', 400, TRUE, NOW(), NOW()),
('ERR_019', 'FAQ operation failed', 'Invalid FAQ operation', 400, TRUE, NOW(), NOW()),
('ERR_020', 'Error code unavailable', 'Reserved/fallback error when requested error code is unavailable', 500, TRUE, NOW(), NOW()),
('ERR_021', 'Invalid or expired OTP', 'The provided OTP is invalid or has expired', 400, TRUE, NOW(), NOW()),
('ERR_022', 'OTP already used', 'The provided OTP has already been used', 400, TRUE, NOW(), NOW()),
('ERR_023', 'First login required', 'Account requires first login with temporary password and OTP', 403, TRUE, NOW(), NOW()),
('ERR_024', 'Password mismatch', 'Password and confirm password do not match', 400, TRUE, NOW(), NOW()),
('ERR_025', 'First login not required', 'First login password reset is not required for this account', 400, TRUE, NOW(), NOW()),
('ERR_999', 'Unknown error / fallback error', 'Fallback error when requested error code is unavailable', 500, TRUE, NOW(), NOW());

-- Update any remaining ER-XXX codes to ERR_XXX format (idempotent)
UPDATE hd_error_codes SET code = 'ERR_001' WHERE code = 'ER-001';
UPDATE hd_error_codes SET code = 'ERR_002' WHERE code = 'ER-002';
UPDATE hd_error_codes SET code = 'ERR_003' WHERE code = 'ER-003';
UPDATE hd_error_codes SET code = 'ERR_004' WHERE code = 'ER-004';
UPDATE hd_error_codes SET code = 'ERR_005' WHERE code = 'ER-005';
UPDATE hd_error_codes SET code = 'ERR_006' WHERE code = 'ER-006';
UPDATE hd_error_codes SET code = 'ERR_007' WHERE code = 'ER-007';
UPDATE hd_error_codes SET code = 'ERR_008' WHERE code = 'ER-008';
UPDATE hd_error_codes SET code = 'ERR_009' WHERE code = 'ER-009';
UPDATE hd_error_codes SET code = 'ERR_010' WHERE code = 'ER-010';
