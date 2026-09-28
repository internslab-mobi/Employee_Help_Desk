CREATE TABLE hd_faqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category_id BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (category_id) REFERENCES hd_categories(id),
    INDEX idx_category_id (category_id)
);
