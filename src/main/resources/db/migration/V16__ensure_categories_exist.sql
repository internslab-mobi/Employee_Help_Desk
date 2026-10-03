-- Ensure category reference data exists
-- This migration is idempotent and safe to run even if V12 was partially applied
-- It uses INSERT IGNORE to avoid duplicate key errors

-- Ensure Departments exist first (idempotent)
INSERT IGNORE INTO hd_departments (code, name, description, is_active, timezone, created_at, updated_at) VALUES
('IT', 'Information Technology', 'IT support and infrastructure', TRUE, 'UTC', NOW(), NOW()),
('HR', 'Human Resources', 'HR and personnel management', TRUE, 'UTC', NOW(), NOW()),
('FIN', 'Finance', 'Finance and accounting', TRUE, 'UTC', NOW(), NOW()),
('OPS', 'Operations', 'Operations and logistics', TRUE, 'UTC', NOW(), NOW()),
('MKT', 'Marketing', 'Marketing and communications', TRUE, 'UTC', NOW(), NOW()),
('LEG', 'Legal', 'Legal and compliance', TRUE, 'UTC', NOW(), NOW()),
('FAC', 'Facilities', 'Facilities management', TRUE, 'UTC', NOW(), NOW());

-- Ensure Categories exist for IT Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'IT' LIMIT 1),
    'Hardware',
    'Hardware-related issues',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'IT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'IT' LIMIT 1),
    'Software',
    'Software and application issues',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'IT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'IT' LIMIT 1),
    'Network',
    'Network and connectivity issues',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'IT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'IT' LIMIT 1),
    'Access',
    'Access and authentication issues',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'IT');

-- Ensure Categories exist for HR Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'HR' LIMIT 1),
    'Leave',
    'Leave and time-off requests',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'HR');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'HR' LIMIT 1),
    'Payroll',
    'Payroll and compensation',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'HR');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'HR' LIMIT 1),
    'Benefits',
    'Employee benefits and insurance',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'HR');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'HR' LIMIT 1),
    'Policy',
    'HR policies and procedures',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'HR');

-- Ensure Categories exist for Finance Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FIN' LIMIT 1),
    'Expense',
    'Expense reports and reimbursements',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FIN');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FIN' LIMIT 1),
    'Invoice',
    'Invoice and billing',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FIN');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FIN' LIMIT 1),
    'Budget',
    'Budget and financial planning',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FIN');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FIN' LIMIT 1),
    'Tax',
    'Tax-related issues',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FIN');

-- Ensure Categories exist for Operations Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'OPS' LIMIT 1),
    'Logistics',
    'Logistics and supply chain',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'OPS');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'OPS' LIMIT 1),
    'Inventory',
    'Inventory management',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'OPS');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'OPS' LIMIT 1),
    'Procurement',
    'Procurement and purchasing',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'OPS');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'OPS' LIMIT 1),
    'Quality',
    'Quality assurance',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'OPS');

-- Ensure Categories exist for Marketing Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'MKT' LIMIT 1),
    'Campaign',
    'Marketing campaigns',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'MKT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'MKT' LIMIT 1),
    'Content',
    'Content creation and management',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'MKT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'MKT' LIMIT 1),
    'Social Media',
    'Social media management',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'MKT');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'MKT' LIMIT 1),
    'Events',
    'Event planning and coordination',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'MKT');

-- Ensure Categories exist for Legal Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'LEG' LIMIT 1),
    'Contracts',
    'Contract review and management',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'LEG');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'LEG' LIMIT 1),
    'Compliance',
    'Regulatory compliance',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'LEG');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'LEG' LIMIT 1),
    'IP',
    'Intellectual property',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'LEG');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'LEG' LIMIT 1),
    'Dispute',
    'Legal disputes and litigation',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'LEG');

-- Ensure Categories exist for Facilities Department
INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FAC' LIMIT 1),
    'Maintenance',
    'Facility maintenance',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FAC');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FAC' LIMIT 1),
    'Security',
    'Building security',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FAC');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FAC' LIMIT 1),
    'Space',
    'Space allocation and management',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FAC');

INSERT IGNORE INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT 
    (SELECT id FROM hd_departments WHERE code = 'FAC' LIMIT 1),
    'Utilities',
    'Utilities and services',
    TRUE,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM hd_departments WHERE code = 'FAC');
