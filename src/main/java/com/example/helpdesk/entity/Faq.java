package com.example.helpdesk.entity;

// Temporarily commented out until hd_faqs table is created in database
// Run this SQL to create the table:
// CREATE TABLE hd_faqs (
//     id BIGINT AUTO_INCREMENT PRIMARY KEY,
//     created_at DATETIME NOT NULL,
//     updated_at DATETIME NOT NULL,
//     question TEXT NOT NULL,
//     answer TEXT NOT NULL,
//     category_id BIGINT,
//     is_active BOOLEAN DEFAULT TRUE,
//     FOREIGN KEY (category_id) REFERENCES hd_categories(id)
// );

// import jakarta.persistence.*;
// import lombok.*;
//
// import java.time.LocalDateTime;
//
// @Entity
// @Table(name = "hd_faqs")
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class Faq {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @Column(nullable = false, columnDefinition = "TEXT")
//     private String question;
//
//     @Column(nullable = false, columnDefinition = "TEXT")
//     private String answer;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "category_id")
//     private Category category;
//
//     @Builder.Default
//     @Column(name = "is_active", nullable = false)
//     private Boolean active = true;
//
//     @Column(name = "created_at", nullable = false)
//     private LocalDateTime createdAt;
//
//     @Column(name = "updated_at", nullable = false)
//     private LocalDateTime updatedAt;
//
//     @PrePersist
//     protected void onCreate() {
//         LocalDateTime now = LocalDateTime.now();
//         createdAt = now;
//         updatedAt = now;
//     }
//
//     @PreUpdate
//     protected void onUpdate() {
//         updatedAt = LocalDateTime.now();
//     }
// }
