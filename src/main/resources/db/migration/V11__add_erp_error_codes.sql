-- Add ERP_XXX error codes for standardized exception handling
-- These codes follow the ERP_XXX format as required by the centralized exception framework

INSERT INTO hd_error_codes (code, message, description, http_status, is_active, created_at, updated_at) VALUES
('ERP_001', 'Resource not found', 'Requested resource was not found', 404, TRUE, NOW(), NOW()),
('ERP_002', 'Validation failed', 'Request validation failed', 400, TRUE, NOW(), NOW()),
('ERP_003', 'Authentication failed', 'Authentication failed', 401, TRUE, NOW(), NOW()),
('ERP_004', 'Access denied', 'Access denied', 403, TRUE, NOW(), NOW()),
('ERP_005', 'Resource conflict', 'Resource already exists or conflicts with existing resource', 409, TRUE, NOW(), NOW()),
('ERP_006', 'Invalid business operation', 'Business rule violation', 400, TRUE, NOW(), NOW()),
('ERP_007', 'Invalid state transition', 'Invalid state transition', 409, TRUE, NOW(), NOW()),
('ERP_008', 'Invalid request', 'Invalid request', 400, TRUE, NOW(), NOW()),
('ERP_009', 'External service error', 'External service error', 502, TRUE, NOW(), NOW()),
('ERP_010', 'Internal server error', 'An unexpected error occurred', 500, TRUE, NOW(), NOW()),
('ERP_999', 'No error', 'No error - for successful responses when errorCode is required', 200, TRUE, NOW(), NOW());
