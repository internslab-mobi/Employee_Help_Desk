# Standalone Email Notification Microservice

A standalone Spring Boot microservice for email notification delivery within the Employee Help Desk ecosystem.

---

## 1. Overview & Isolation

This microservice is completely standalone and dedicated exclusively to email notifications:
- **Zero Coupling**: It does not depend on, call, or import `Employee_Help_Desk_Backend` or `Employee_Help_Desk_Report_Service`.
- **Email Only**: In-app notifications are managed directly by the core application; this microservice focuses strictly on email dispatch.
- **Stateless & Database-Free**: No database or persistence layer required. All email deliveries are handled through Spring Boot Mail (`JavaMailSender`) asynchronously.
- **Flexible REST Endpoints**: Exposes endpoints for email dispatch supporting both clean email request payloads (`to`, `subject`, `body`) and notification format payloads (`recipientEmail`, `title`, `message`).

---

## 2. Technology Stack

- **Java**: 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Email Delivery**: Spring Boot Mail (`JavaMailSender`, Jakarta Mail)
- **Validation**: Jakarta Validation (`spring-boot-starter-validation`)
- **JSON Serialization**: Jackson with `@JsonInclude(JsonInclude.Include.NON_NULL)` and `@JsonAlias`
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
        │               │   └── EmailController.java
        │               ├── dto/
        │               │   ├── EmailRequestDTO.java
        │               │   └── EmailResponseDTO.java
        │               ├── exception/
        │               │   ├── BadRequestException.java
        │               │   ├── ErrorResponse.java
        │               │   ├── GlobalExceptionHandler.java
        │               │   ├── NotificationServiceException.java
        │               │   └── ResourceNotFoundException.java
        │               └── service/
        │                   ├── EmailService.java
        │                   └── impl/
        │                       └── EmailServiceImpl.java
        └── resources/
            └── application.yaml
```

---

## 4. Configuration & Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `NOTIFICATION_SERVICE_PORT` | `8085` | HTTP Server Port |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | `""` (empty) | SMTP username / address |
| `MAIL_PASSWORD` | `""` (empty) | SMTP password / app password |
| `MAIL_FROM` | `helpdesk@example.com` | From header for notification emails |
| `MAIL_SMTP_AUTH` | `true` | SMTP Authentication flag |
| `MAIL_SMTP_STARTTLS_ENABLE` | `true` | SMTP STARTTLS flag |
| `MAIL_TIMEOUT_MS` | `5000` | SMTP Connection, Read, and Write Timeout (ms) |

---

## 5. REST API Reference

### Endpoints

| HTTP Method | Endpoint | Description |
|---|---|---|
| `POST` | `/emails` | Dispatch notification email |
| `POST` | `/emails/send` | Dispatch notification email (alias) |
| `POST` | `/notifications` | Dispatch notification email (compatible endpoint) |
| `POST` | `/notifications/email` | Dispatch notification email (alias) |

### Request Payload Format

#### Standard Email Format
```json
{
  "to": "employee@example.com",
  "subject": "Ticket Resolved: HD-1001",
  "body": "Your ticket HD-1001 has been marked as resolved."
}
```

#### Notification Format (Supported via `@JsonAlias`)
```json
{
  "recipientEmail": "employee@example.com",
  "title": "Ticket Resolved: HD-1001",
  "message": "Your ticket HD-1001 has been marked as resolved."
}
```

### Response Payload Format (`200 OK`)
```json
{
  "status": "SUCCESS",
  "message": "Email notification accepted for delivery",
  "success": true,
  "recipient": "employee@example.com",
  "timestamp": "2026-10-13T10:00:00Z"
}
```
