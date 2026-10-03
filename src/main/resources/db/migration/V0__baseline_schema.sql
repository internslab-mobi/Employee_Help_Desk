-- V0: Baseline schema creation for Employee Help Desk
-- Creates all core tables that are assumed to exist by subsequent migrations.
-- Uses CREATE TABLE IF NOT EXISTS for idempotent execution.

-- Departments
CREATE TABLE IF NOT EXISTS hd_departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    timezone VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_departments_code UNIQUE (code),
    CONSTRAINT uk_hd_departments_name UNIQUE (name)
);

-- Employees
CREATE TABLE IF NOT EXISTS hd_employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_code VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    designation VARCHAR(100),
    department_id BIGINT,
    employment_status VARCHAR(30) NOT NULL,
    date_of_joining DATE,
    date_of_exit DATE,
    profile_image LONGBLOB,
    profile_image_type VARCHAR(100),
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    timezone VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_employees_code UNIQUE (employee_code),
    CONSTRAINT uk_hd_employees_email UNIQUE (email),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES hd_departments(id)
);

-- Categories
CREATE TABLE IF NOT EXISTS hd_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_department FOREIGN KEY (department_id) REFERENCES hd_departments(id),
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES hd_employees(id)
);

-- Sub-Categories
CREATE TABLE IF NOT EXISTS hd_sub_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    priority VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sub_categories_category FOREIGN KEY (category_id) REFERENCES hd_categories(id),
    CONSTRAINT fk_sub_categories_created_by FOREIGN KEY (created_by) REFERENCES hd_employees(id)
);

-- Skills
CREATE TABLE IF NOT EXISTS hd_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_skills_name UNIQUE (name)
);

-- Department Agents
CREATE TABLE IF NOT EXISTS hd_department_agents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_assigned_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_dept_agents_department FOREIGN KEY (department_id) REFERENCES hd_departments(id),
    CONSTRAINT fk_dept_agents_employee FOREIGN KEY (employee_id) REFERENCES hd_employees(id)
);

-- Department Managers
CREATE TABLE IF NOT EXISTS hd_department_managers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_dept_managers_department FOREIGN KEY (department_id) REFERENCES hd_departments(id),
    CONSTRAINT fk_dept_managers_employee FOREIGN KEY (employee_id) REFERENCES hd_employees(id)
);

-- Agent Skills
CREATE TABLE IF NOT EXISTS hd_agent_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_skills_agent FOREIGN KEY (agent_id) REFERENCES hd_department_agents(id),
    CONSTRAINT fk_agent_skills_skill FOREIGN KEY (skill_id) REFERENCES hd_skills(id)
);

-- Sub-Category Skills
CREATE TABLE IF NOT EXISTS hd_sub_category_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sub_category_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sub_cat_skills_sub_cat FOREIGN KEY (sub_category_id) REFERENCES hd_sub_categories(id),
    CONSTRAINT fk_sub_cat_skills_skill FOREIGN KEY (skill_id) REFERENCES hd_skills(id)
);

-- SLA Policies (mapped by SlaRule entity)
CREATE TABLE IF NOT EXISTS hd_sla_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    sub_category_id BIGINT NOT NULL,
    duration_minutes INT NOT NULL,
    warning_minutes INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sla_policies_department FOREIGN KEY (department_id) REFERENCES hd_departments(id),
    CONSTRAINT fk_sla_policies_sub_category FOREIGN KEY (sub_category_id) REFERENCES hd_sub_categories(id)
);

-- Tickets
CREATE TABLE IF NOT EXISTS hd_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_number VARCHAR(50),
    requester_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    category_id BIGINT,
    sub_category_id BIGINT,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_agent_id BIGINT,
    assigned_manager_id BIGINT,
    sla_policy_id BIGINT,
    reopen_count INT NOT NULL DEFAULT 0,
    priority VARCHAR(30) NOT NULL,
    resolution_summary TEXT,
    hold_reason TEXT,
    hold_started_at DATETIME,
    withdrawal_reason TEXT,
    resolved_at DATETIME,
    reopened_at DATETIME,
    closed_at DATETIME,
    withdrawn_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_tickets_number UNIQUE (ticket_number),
    CONSTRAINT fk_tickets_requester FOREIGN KEY (requester_id) REFERENCES hd_employees(id),
    CONSTRAINT fk_tickets_department FOREIGN KEY (department_id) REFERENCES hd_departments(id),
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id) REFERENCES hd_categories(id),
    CONSTRAINT fk_tickets_sub_category FOREIGN KEY (sub_category_id) REFERENCES hd_sub_categories(id),
    CONSTRAINT fk_tickets_agent FOREIGN KEY (assigned_agent_id) REFERENCES hd_department_agents(id),
    CONSTRAINT fk_tickets_manager FOREIGN KEY (assigned_manager_id) REFERENCES hd_department_managers(id),
    CONSTRAINT fk_tickets_sla_policy FOREIGN KEY (sla_policy_id) REFERENCES hd_sla_policies(id)
);

-- Ticket History
CREATE TABLE IF NOT EXISTS hd_ticket_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    actor_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    metadata JSON,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id),
    CONSTRAINT fk_ticket_history_actor FOREIGN KEY (actor_id) REFERENCES hd_employees(id)
);

-- Ticket Messages
CREATE TABLE IF NOT EXISTS hd_ticket_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    seen BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id),
    CONSTRAINT fk_ticket_messages_sender FOREIGN KEY (sender_id) REFERENCES hd_employees(id)
);

-- Ticket Attachments
CREATE TABLE IF NOT EXISTS hd_ticket_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    message_id BIGINT,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    file_data LONGBLOB NOT NULL,
    attachment_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id),
    CONSTRAINT fk_ticket_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES hd_employees(id),
    CONSTRAINT fk_ticket_attachments_message FOREIGN KEY (message_id) REFERENCES hd_ticket_messages(id)
);

-- Ticket Feedback
CREATE TABLE IF NOT EXISTS hd_ticket_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hd_ticket_feedback_ticket UNIQUE (ticket_id),
    CONSTRAINT fk_ticket_feedback_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id),
    CONSTRAINT fk_ticket_feedback_submitter FOREIGN KEY (submitted_by) REFERENCES hd_employees(id)
);

-- SLA Instances (mapped by TicketSla entity)
CREATE TABLE IF NOT EXISTS hd_sla_instances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sla_policy_id BIGINT NOT NULL,
    cycle_number INT NOT NULL DEFAULT 0,
    allocated_minutes INT NOT NULL,
    sla_start_at DATETIME NOT NULL,
    original_deadline_at DATETIME NOT NULL,
    current_deadline_at DATETIME NOT NULL,
    warning_at DATETIME,
    status VARCHAR(30) NOT NULL,
    paused_at DATETIME,
    breached_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sla_instances_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id),
    CONSTRAINT fk_sla_instances_policy FOREIGN KEY (sla_policy_id) REFERENCES hd_sla_policies(id)
);

-- Notifications
CREATE TABLE IF NOT EXISTS hd_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    ticket_id BIGINT,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES hd_employees(id),
    CONSTRAINT fk_notifications_ticket FOREIGN KEY (ticket_id) REFERENCES hd_tickets(id)
);

-- Login OTPs
CREATE TABLE IF NOT EXISTS hd_login_otps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME,
    attempt_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_login_otps_employee FOREIGN KEY (employee_id) REFERENCES hd_employees(id)
);
