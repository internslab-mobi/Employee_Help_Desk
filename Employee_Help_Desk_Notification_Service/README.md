# Standalone Notification Microservice

A standalone Spring Boot microservice for notification management (in-app notifications and email notifications) within the Employee Help Desk ecosystem.

---

## 1. Overview & Isolation

This microservice is completely standalone and contains only the equivalent notification and email logic from the main project:
- **Zero Coupling**: It does not depend on, call, or import `Employee_Help_Desk_Backend` or `Employee_Help_Desk_Report_Service`.
- **In-App & Email Channels**: Replicates the dual notification channel logic from the main project:
  - Notifications are persisted in the service's own MySQL database (`employee_helpdesk_notifications`).
  - If a recipient email address is provided, a notification email is dispatched asynchronously via Spring Boot Mail (`JavaMailSender`).
- **Clean REST Controller**: Exposes the exact equivalent notification endpoints (`GET /notifications`, `GET /notifications/unread`, `GET /notifications/unread/count`, `PATCH /notifications/{notificationId}/read`) and `POST /notifications` for standalone notification dispatch.

---

## 2. Technology Stack

- **Java**: 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: MySQL 8.x
- **Email Delivery**: Spring Boot Mail (`JavaMailSender`, Jakarta Mail)
- **Validation**: Jakarta Validation (`spring-boot-starter-validation`)
- **JSON Serialization**: Jackson with `@JsonInclude(JsonInclude.Include.NON_NULL)`
- **Build Tool**: Apache Maven with Maven Wrapper (`./mvnw`)

---

## 3. Project & Package Structure

```
Employee_Help_Desk_Notification_Service/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── xyz/
        │       └── mobi/
        │           └── notificationservice/
        │               ├── NotificationServiceApplication.java
        │               ├── controller/
        │               │   └── NotificationController.java
        │               ├── dto/
        │               │   ├── CreateNotificationRequestDTO.java
        │               │   ├── NotificationResponseDTO.java
        │               │   └── UnreadNotificationCountResponseDTO.java
        │               ├── entity/
        │               │   ├── BaseEntity.java
        │               │   ├── Notification.java
        │               │   └── enums/
        │               │       └── NotificationType.java
        │               ├── exception/
        │               │   ├── BadRequestException.java
        │               │   ├── ErrorResponse.java
        │               │   ├── GlobalExceptionHandler.java
        │               │   ├── NotificationServiceException.java
        │               │   └── ResourceNotFoundException.java
        │               ├── mapper/
        │               │   └── NotificationMapper.java
        │               ├── repository/
        │               │   └── NotificationRepository.java
        │               └── service/
        │                   ├── EmailService.java
        │                   ├── NotificationService.java
        │                   └── impl/
        │                       └── NotificationServiceImpl.java
        └── resources/
            ├── application.yaml
            └── schema.sql
```

---

## 4. Database Setup & DDL

### Database Schema
```sql
CREATE DATABASE IF NOT EXISTS employee_helpdesk_notifications;
USE employee_helpdesk_notifications;
```

### Table DDL
```sql
CREATE TABLE IF NOT EXISTS hd_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    ticket_id BIGINT NULL,
    ticket_number VARCHAR(50) NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_notification_recipient (recipient_id),
    INDEX idx_notification_recipient_read_created (recipient_id, is_read, created_at),
    INDEX idx_notification_recipient_created (recipient_id, created_at),
    INDEX idx_notification_ticket (ticket_id),
    INDEX idx_notification_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 5. Configuration & Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `NOTIFICATION_SERVICE_PORT` | `8085` | HTTP Server Port |
| `DB_URL` | `jdbc:mysql://localhost:3306/employee_helpdesk_notifications?...` | JDBC database URL |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | `""` (empty) | Database password |
| `HIBERNATE_DDL_AUTO` | `update` | Hibernate DDL mode |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | `""` (empty) | SMTP username / address |
| `MAIL_PASSWORD` | `""` (empty) | SMTP password / app password |
| `MAIL_FROM` | `helpdesk@example.com` | From header for notification emails |

---

## 6. REST API Reference

| HTTP Method | Endpoint | Description |
|---|---|---|
| `POST` | `/notifications` | Create notification (in-app persistence + async email dispatch if `recipientEmail` provided) |
| `GET` | `/notifications` | List notifications (paginated, optional `recipientId` filter) |
| `GET` | `/notifications/unread` | List unread notifications (paginated, optional `recipientId` filter) |
| `GET` | `/notifications/unread/count` | Get count of unread notifications (optional `recipientId` filter) |
| `PATCH` | `/notifications/{notificationId}/read` | Mark a notification as read |
