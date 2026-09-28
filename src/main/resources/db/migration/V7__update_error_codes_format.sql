-- Update existing error codes from ER-XXX to ERR_XXX format and add new error codes

-- Update existing error codes
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

-- Insert new error codes
INSERT INTO hd_error_codes (code, message, description, http_status, is_active, created_at, updated_at) VALUES
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
('ERR_999', 'Unknown error / fallback error', 'Fallback error when requested error code is unavailable', 500, TRUE, NOW(), NOW());
