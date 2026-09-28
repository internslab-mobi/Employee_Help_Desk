-- Add error codes for OTP and first-login scenarios
INSERT INTO hd_error_codes (code, message, description, http_status, is_active, created_at, updated_at) VALUES
('ERR_021', 'Invalid or expired OTP', 'The provided OTP is invalid or has expired', 400, true, NOW(), NOW()),
('ERR_022', 'OTP already used', 'The provided OTP has already been used', 400, true, NOW(), NOW()),
('ERR_023', 'First login required', 'Account requires first login with temporary password and OTP', 403, true, NOW(), NOW()),
('ERR_024', 'Password mismatch', 'Password and confirm password do not match', 400, true, NOW(), NOW()),
('ERR_025', 'First login not required', 'First login password reset is not required for this account', 400, true, NOW(), NOW());
