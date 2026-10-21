-- Seed reference data for departments, categories, sub-categories, and skills
-- This migration populates the required reference tables for an empty database

-- Insert Departments
INSERT INTO hd_departments (code, name, description, is_active, timezone, created_at, updated_at) VALUES
('IT', 'Information Technology', 'IT support and infrastructure', TRUE, 'UTC', NOW(), NOW()),
('HR', 'Human Resources', 'HR and personnel management', TRUE, 'UTC', NOW(), NOW()),
('FIN', 'Finance', 'Finance and accounting', TRUE, 'UTC', NOW(), NOW()),
('OPS', 'Operations', 'Operations and logistics', TRUE, 'UTC', NOW(), NOW()),
('MKT', 'Marketing', 'Marketing and communications', TRUE, 'UTC', NOW(), NOW()),
('LEG', 'Legal', 'Legal and compliance', TRUE, 'UTC', NOW(), NOW()),
('FAC', 'Facilities', 'Facilities management', TRUE, 'UTC', NOW(), NOW());

-- Insert Categories (IT Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'IT'), 'Hardware', 'Hardware-related issues', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'IT'), 'Software', 'Software and application issues', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'IT'), 'Network', 'Network and connectivity issues', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'IT'), 'Access', 'Access and authentication issues', TRUE, NOW(), NOW());

-- Insert Categories (HR Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'HR'), 'Leave', 'Leave and time-off requests', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'HR'), 'Payroll', 'Payroll and compensation', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'HR'), 'Benefits', 'Employee benefits and insurance', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'HR'), 'Policy', 'HR policies and procedures', TRUE, NOW(), NOW());

-- Insert Categories (Finance Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'FIN'), 'Expense', 'Expense reports and reimbursements', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FIN'), 'Invoice', 'Invoice and billing', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FIN'), 'Budget', 'Budget and financial planning', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FIN'), 'Tax', 'Tax-related issues', TRUE, NOW(), NOW());

-- Insert Categories (Operations Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'OPS'), 'Logistics', 'Logistics and supply chain', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'OPS'), 'Inventory', 'Inventory management', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'OPS'), 'Procurement', 'Procurement and purchasing', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'OPS'), 'Quality', 'Quality assurance', TRUE, NOW(), NOW());

-- Insert Categories (Marketing Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'MKT'), 'Campaign', 'Marketing campaigns', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'MKT'), 'Content', 'Content creation and management', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'MKT'), 'Social Media', 'Social media management', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'MKT'), 'Events', 'Event planning and coordination', TRUE, NOW(), NOW());

-- Insert Categories (Legal Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'LEG'), 'Contracts', 'Contract review and management', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'LEG'), 'Compliance', 'Regulatory compliance', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'LEG'), 'IP', 'Intellectual property', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'LEG'), 'Dispute', 'Legal disputes and litigation', TRUE, NOW(), NOW());

-- Insert Categories (Facilities Department)
INSERT INTO hd_categories (department_id, name, description, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_departments WHERE code = 'FAC'), 'Maintenance', 'Facility maintenance', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FAC'), 'Security', 'Building security', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FAC'), 'Space', 'Space allocation and management', TRUE, NOW(), NOW()),
((SELECT id FROM hd_departments WHERE code = 'FAC'), 'Utilities', 'Utilities and services', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Hardware - IT)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Hardware' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Laptop', 'Laptop hardware issues', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Hardware' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Desktop', 'Desktop computer issues', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Hardware' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Printer', 'Printer and scanner issues', 'LOW', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Hardware' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Monitor', 'Monitor and display issues', 'LOW', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Hardware' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Peripheral', 'Keyboards, mice, and other peripherals', 'LOW', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Software - IT)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Software' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'OS', 'Operating system issues', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Software' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Office', 'Microsoft Office and productivity tools', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Software' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Custom App', 'Custom application issues', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Software' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Installation', 'Software installation and upgrades', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Software' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'License', 'Software licensing issues', 'MEDIUM', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Network - IT)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Network' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'WiFi', 'Wireless connectivity issues', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Network' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'VPN', 'VPN access issues', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Network' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Internet', 'Internet connectivity', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Network' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Intranet', 'Internal network issues', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Network' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Firewall', 'Firewall and security rules', 'HIGH', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Access - IT)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Access' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Password Reset', 'Password reset assistance', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Access' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Account Lock', 'Account unlock requests', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Access' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'New User', 'New user account creation', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Access' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'Permissions', 'Access permission changes', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Access' AND department_id = (SELECT id FROM hd_departments WHERE code = 'IT')), 'MFA', 'Multi-factor authentication issues', 'HIGH', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Leave - HR)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Leave' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Sick Leave', 'Sick leave requests', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Leave' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Vacation', 'Vacation and annual leave', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Leave' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Personal', 'Personal leave requests', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Leave' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Emergency', 'Emergency leave', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Leave' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Comp Time', 'Compensatory time off', 'LOW', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Payroll - HR)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Payroll' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Salary', 'Salary and payment issues', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Payroll' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Deduction', 'Payroll deduction inquiries', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Payroll' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Bonus', 'Bonus and commission payments', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Payroll' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Timesheet', 'Timesheet and attendance issues', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Payroll' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Tax', 'Payroll tax questions', 'LOW', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Benefits - HR)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Benefits' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Health Insurance', 'Health insurance enrollment and claims', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Benefits' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Retirement', '401k and retirement plans', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Benefits' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Life Insurance', 'Life insurance benefits', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Benefits' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Dental', 'Dental and vision benefits', 'LOW', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Benefits' AND department_id = (SELECT id FROM hd_departments WHERE code = 'HR')), 'Dependents', 'Dependent coverage and changes', 'MEDIUM', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Expense - Finance)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Expense' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Travel', 'Travel expense reports', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Expense' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Meals', 'Meal and entertainment expenses', 'LOW', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Expense' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Equipment', 'Equipment and supply expenses', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Expense' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Training', 'Training and education expenses', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Expense' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Reimbursement', 'General reimbursement requests', 'MEDIUM', TRUE, NOW(), NOW());

-- Insert Sub-Categories (Invoice - Finance)
INSERT INTO hd_sub_categories (category_id, name, description, priority, is_active, created_at, updated_at) VALUES
((SELECT id FROM hd_categories WHERE name = 'Invoice' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Vendor Invoice', 'Vendor invoice processing', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Invoice' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Client Invoice', 'Client invoice inquiries', 'MEDIUM', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Invoice' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Payment', 'Payment status and tracking', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Invoice' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Dispute', 'Invoice disputes', 'HIGH', TRUE, NOW(), NOW()),
((SELECT id FROM hd_categories WHERE name = 'Invoice' AND department_id = (SELECT id FROM hd_departments WHERE code = 'FIN')), 'Credit', 'Credit notes and adjustments', 'LOW', TRUE, NOW(), NOW());

-- Insert Skills
INSERT INTO hd_skills (name, description, is_active, created_at, updated_at) VALUES
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
