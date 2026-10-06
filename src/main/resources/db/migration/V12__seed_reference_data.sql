-- Seed reference data for departments, categories, sub-categories, and skills
-- This migration populates the required reference tables for an empty database
-- Made idempotent to safely handle existing reference data without duplicates

-- Insert Departments (idempotent: skips if department code already exists)
INSERT IGNORE INTO hd_departments (code, name, description, is_active, timezone, created_at, updated_at) VALUES
('IT', 'Information Technology', 'IT support and infrastructure', TRUE, 'UTC', NOW(), NOW()),
('HR', 'Human Resources', 'HR and personnel management', TRUE, 'UTC', NOW(), NOW()),
('FIN', 'Finance', 'Finance and accounting', TRUE, 'UTC', NOW(), NOW()),
('OPS', 'Operations', 'Operations and logistics', TRUE, 'UTC', NOW(), NOW()),
('MKT', 'Marketing', 'Marketing and communications', TRUE, 'UTC', NOW(), NOW()),
('LEG', 'Legal', 'Legal and compliance', TRUE, 'UTC', NOW(), NOW()),
('FAC', 'Facilities', 'Facilities management', TRUE, 'UTC', NOW(), NOW());

-- Insert Categories (idempotent: skips if category already exists in department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at)
SELECT dept.id, c.name, c.description, TRUE, NOW(), NOW()
FROM (
    SELECT 'IT' as dept_code, 'Hardware' as name, 'Hardware-related issues' as description UNION ALL
    SELECT 'IT', 'Software', 'Software and application issues' UNION ALL
    SELECT 'IT', 'Network', 'Network and connectivity issues' UNION ALL
    SELECT 'IT', 'Access', 'Access and authentication issues' UNION ALL
    SELECT 'HR', 'Leave', 'Leave and time-off requests' UNION ALL
    SELECT 'HR', 'Payroll', 'Payroll and compensation' UNION ALL
    SELECT 'HR', 'Benefits', 'Employee benefits and insurance' UNION ALL
    SELECT 'HR', 'Policy', 'HR policies and procedures' UNION ALL
    SELECT 'FIN', 'Expense', 'Expense reports and reimbursements' UNION ALL
    SELECT 'FIN', 'Invoice', 'Invoice and billing' UNION ALL
    SELECT 'FIN', 'Budget', 'Budget and financial planning' UNION ALL
    SELECT 'FIN', 'Tax', 'Tax-related issues' UNION ALL
    SELECT 'OPS', 'Logistics', 'Logistics and supply chain' UNION ALL
    SELECT 'OPS', 'Inventory', 'Inventory management' UNION ALL
    SELECT 'OPS', 'Procurement', 'Procurement and purchasing' UNION ALL
    SELECT 'OPS', 'Quality', 'Quality assurance' UNION ALL
    SELECT 'MKT', 'Campaign', 'Marketing campaigns' UNION ALL
    SELECT 'MKT', 'Content', 'Content creation and management' UNION ALL
    SELECT 'MKT', 'Social Media', 'Social media management' UNION ALL
    SELECT 'MKT', 'Events', 'Event planning and coordination' UNION ALL
    SELECT 'LEG', 'Contracts', 'Contract review and management' UNION ALL
    SELECT 'LEG', 'Compliance', 'Regulatory compliance' UNION ALL
    SELECT 'LEG', 'IP', 'Intellectual property' UNION ALL
    SELECT 'LEG', 'Dispute', 'Legal disputes and litigation' UNION ALL
    SELECT 'FAC', 'Maintenance', 'Facility maintenance' UNION ALL
    SELECT 'FAC', 'Security', 'Building security' UNION ALL
    SELECT 'FAC', 'Space', 'Space allocation and management' UNION ALL
    SELECT 'FAC', 'Utilities', 'Utilities and services'
) c
JOIN hd_departments dept ON dept.code = c.dept_code
WHERE NOT EXISTS (
    SELECT 1 FROM hd_categories existing 
    WHERE existing.department_id = dept.id AND existing.name = c.name
);

-- Insert Sub-Categories (idempotent: skips if subcategory already exists in category)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at)
SELECT cat.id, s.name, s.description, s.priority, TRUE, NOW(), NOW()
FROM (
    -- Hardware (IT)
    SELECT 'IT' as dept_code, 'Hardware' as cat_name, 'Laptop' as name, 'Laptop hardware issues' as description, 'MEDIUM' as priority UNION ALL
    SELECT 'IT', 'Hardware', 'Desktop', 'Desktop computer issues', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Hardware', 'Printer', 'Printer and scanner issues', 'LOW' UNION ALL
    SELECT 'IT', 'Hardware', 'Monitor', 'Monitor and display issues', 'LOW' UNION ALL
    SELECT 'IT', 'Hardware', 'Peripheral', 'Keyboards, mice, and other peripherals', 'LOW' UNION ALL
    -- Software (IT)
    SELECT 'IT', 'Software', 'OS', 'Operating system issues', 'HIGH' UNION ALL
    SELECT 'IT', 'Software', 'Office', 'Microsoft Office and productivity tools', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Software', 'Custom App', 'Custom application issues', 'HIGH' UNION ALL
    SELECT 'IT', 'Software', 'Installation', 'Software installation and upgrades', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Software', 'License', 'Software licensing issues', 'MEDIUM' UNION ALL
    -- Network (IT)
    SELECT 'IT', 'Network', 'WiFi', 'Wireless connectivity issues', 'HIGH' UNION ALL
    SELECT 'IT', 'Network', 'VPN', 'VPN access issues', 'HIGH' UNION ALL
    SELECT 'IT', 'Network', 'Internet', 'Internet connectivity', 'HIGH' UNION ALL
    SELECT 'IT', 'Network', 'Intranet', 'Internal network issues', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Network', 'Firewall', 'Firewall and security rules', 'HIGH' UNION ALL
    -- Access (IT)
    SELECT 'IT', 'Access', 'Password Reset', 'Password reset assistance', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Access', 'Account Lock', 'Account unlock requests', 'HIGH' UNION ALL
    SELECT 'IT', 'Access', 'New User', 'New user account creation', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Access', 'Permissions', 'Access permission changes', 'MEDIUM' UNION ALL
    SELECT 'IT', 'Access', 'MFA', 'Multi-factor authentication issues', 'HIGH' UNION ALL
    -- Leave (HR)
    SELECT 'HR', 'Leave', 'Sick Leave', 'Sick leave requests', 'HIGH' UNION ALL
    SELECT 'HR', 'Leave', 'Vacation', 'Vacation and annual leave', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Leave', 'Personal', 'Personal leave requests', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Leave', 'Emergency', 'Emergency leave', 'HIGH' UNION ALL
    SELECT 'HR', 'Leave', 'Comp Time', 'Compensatory time off', 'LOW' UNION ALL
    -- Payroll (HR)
    SELECT 'HR', 'Payroll', 'Salary', 'Salary and payment issues', 'HIGH' UNION ALL
    SELECT 'HR', 'Payroll', 'Deduction', 'Payroll deduction inquiries', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Payroll', 'Bonus', 'Bonus and commission payments', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Payroll', 'Timesheet', 'Timesheet and attendance issues', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Payroll', 'Tax', 'Payroll tax questions', 'LOW' UNION ALL
    -- Benefits (HR)
    SELECT 'HR', 'Benefits', 'Health Insurance', 'Health insurance enrollment and claims', 'HIGH' UNION ALL
    SELECT 'HR', 'Benefits', 'Retirement', '401k and retirement plans', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Benefits', 'Life Insurance', 'Life insurance benefits', 'MEDIUM' UNION ALL
    SELECT 'HR', 'Benefits', 'Dental', 'Dental and vision benefits', 'LOW' UNION ALL
    SELECT 'HR', 'Benefits', 'Dependents', 'Dependent coverage and changes', 'MEDIUM' UNION ALL
    -- Expense (Finance)
    SELECT 'FIN', 'Expense', 'Travel', 'Travel expense reports', 'MEDIUM' UNION ALL
    SELECT 'FIN', 'Expense', 'Meals', 'Meal and entertainment expenses', 'LOW' UNION ALL
    SELECT 'FIN', 'Expense', 'Equipment', 'Equipment and supply expenses', 'MEDIUM' UNION ALL
    SELECT 'FIN', 'Expense', 'Training', 'Training and education expenses', 'MEDIUM' UNION ALL
    SELECT 'FIN', 'Expense', 'Reimbursement', 'General reimbursement requests', 'MEDIUM' UNION ALL
    -- Invoice (Finance)
    SELECT 'FIN', 'Invoice', 'Vendor Invoice', 'Vendor invoice processing', 'MEDIUM' UNION ALL
    SELECT 'FIN', 'Invoice', 'Client Invoice', 'Client invoice inquiries', 'MEDIUM' UNION ALL
    SELECT 'FIN', 'Invoice', 'Payment', 'Payment status and tracking', 'HIGH' UNION ALL
    SELECT 'FIN', 'Invoice', 'Dispute', 'Invoice disputes', 'HIGH' UNION ALL
    SELECT 'FIN', 'Invoice', 'Credit', 'Credit notes and adjustments', 'LOW'
) s
JOIN hd_departments dept ON dept.code = s.dept_code
JOIN hd_categories cat ON cat.department_id = dept.id AND cat.name = s.cat_name
WHERE NOT EXISTS (
    SELECT 1 FROM hd_sub_categories existing
    WHERE existing.category_id = cat.id AND existing.name = s.name
);

-- Insert Skills (idempotent: skips if skill name already exists)
INSERT IGNORE INTO hd_skills (name, description, is_active, created_at, updated_at) VALUES
('Hardware Troubleshooting', 'Diagnose and resolve hardware issues', TRUE, NOW(), NOW()),
('Software Installation', 'Install and configure software applications', TRUE, NOW(), NOW()),
('Network Configuration', 'Configure and troubleshoot network settings', TRUE, NOW(), NOW()),
('Windows OS', 'Windows operating system expertise', TRUE, NOW(), NOW()),
('Linux OS', 'Linux operating system expertise', TRUE, NOW(), NOW()),
('Mac OS', 'Mac operating system expertise', TRUE, NOW(), NOW()),
('Database Management', 'Database administration and SQL', TRUE, NOW(), NOW()),
('Cloud Services', 'AWS, Azure, Google Cloud services', TRUE, NOW(), NOW()),
('Security', 'Cybersecurity and access control', TRUE, NOW(), NOW()),
('Mobile Devices', 'iOS and Android device support', TRUE, NOW(), NOW()),
('VPN', 'VPN configuration and troubleshooting', TRUE, NOW(), NOW()),
('Active Directory', 'Active Directory and user management', TRUE, NOW(), NOW()),
('Office 365', 'Microsoft Office 365 administration', TRUE, NOW(), NOW()),
('Backup', 'Data backup and recovery', TRUE, NOW(), NOW()),
('Firewall', 'Firewall configuration and management', TRUE, NOW(), NOW()),
('HR Policy', 'Human resources policies and procedures', TRUE, NOW(), NOW()),
('Payroll System', 'Payroll software and processes', TRUE, NOW(), NOW()),
('Benefits Administration', 'Employee benefits management', TRUE, NOW(), NOW()),
('Recruitment', 'Hiring and onboarding processes', TRUE, NOW(), NOW()),
('Accounting', 'Financial accounting and reporting', TRUE, NOW(), NOW()),
('Budgeting', 'Budget planning and management', TRUE, NOW(), NOW()),
('Tax Compliance', 'Tax regulations and compliance', TRUE, NOW(), NOW()),
('Audit', 'Internal and external audit support', TRUE, NOW(), NOW()),
('Procurement', 'Purchasing and vendor management', TRUE, NOW(), NOW()),
('Supply Chain', 'Supply chain and logistics management', TRUE, NOW(), NOW()),
('Inventory Control', 'Inventory tracking and management', TRUE, NOW(), NOW()),
('Digital Marketing', 'Online marketing and SEO', TRUE, NOW(), NOW()),
('Content Creation', 'Writing and content development', TRUE, NOW(), NOW()),
('Social Media', 'Social media platform management', TRUE, NOW(), NOW()),
('Event Planning', 'Event coordination and management', TRUE, NOW(), NOW()),
('Contract Law', 'Legal contract review and drafting', TRUE, NOW(), NOW()),
('Compliance', 'Regulatory compliance management', TRUE, NOW(), NOW()),
('Intellectual Property', 'IP rights and protection', TRUE, NOW(), NOW()),
('Facility Management', 'Building and facilities maintenance', TRUE, NOW(), NOW()),
('Security Systems', 'Physical security systems', TRUE, NOW(), NOW()),
('Space Planning', 'Office space allocation and design', TRUE, NOW(), NOW()),
('Project Management', 'Project planning and execution', TRUE, NOW(), NOW()),
('Communication', 'Verbal and written communication', TRUE, NOW(), NOW()),
('Customer Service', 'Customer support and relations', TRUE, NOW(), NOW()),
('Problem Solving', 'Analytical problem-solving skills', TRUE, NOW(), NOW()),
('Time Management', 'Prioritization and time management', TRUE, NOW(), NOW()),
('Team Leadership', 'Team management and leadership', TRUE, NOW(), NOW());
