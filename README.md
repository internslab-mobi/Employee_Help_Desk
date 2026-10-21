# Employee Help Desk Backend

Enterprise Spring Boot backend service for Employee Help Desk management with JWT authentication, role-based access control, ticket management, and SLA tracking.

---

## Configuration & Environment Variables

All configuration is centralized in `src/main/resources/application.yml`. Sensitive credentials (passwords, JWT signing secrets) have been removed from source-controlled configuration files and must be provided via environment variables.

### Environment Variable Reference

| Environment Variable | Description | Default / Fallback | Classification |
|---|---|---|---|
| `DB_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/project_employee_helpdesk` | Non-sensitive |
| `DB_USERNAME` | MySQL database username | `root` | Non-sensitive |
| `DB_PASSWORD` | MySQL database password | *(None - Required)* | **SECRET** |
| `JWT_SECRET` | Secret key for signing JWT tokens (min. 256 bits for HS256) | *(None - Required)* | **SECRET** |
| `JWT_EXPIRATION` | Access token lifespan in milliseconds | `900000` (15 mins) | Non-sensitive |
| `JWT_REFRESH_EXPIRATION` | Refresh token lifespan in milliseconds | `604800000` (7 days) | Non-sensitive |
| `SERVER_PORT` | HTTP port for the embedded application server | `8098` | Non-sensitive |
| `EMAIL_ENABLED` | Toggle email delivery (`true`/`false`) | `true` | Non-sensitive |
| `MAIL_HOST` / `SMTP_HOST` | SMTP server hostname | `smtp.gmail.com` | Non-sensitive |
| `MAIL_PORT` / `SMTP_PORT` | SMTP server port | `587` | Non-sensitive |
| `MAIL_USERNAME` / `SMTP_USERNAME` | SMTP sender username / email address | *(None - Required)* | **SENSITIVE** |
| `MAIL_PASSWORD` / `SMTP_PASSWORD` | SMTP password or Google App Password | *(None - Required)* | **SECRET** |

> **IMPORTANT**:
> - Never commit `.env` or files containing actual secrets to version control.
> - A template file `.env.example` is provided in the project root for reference.

---

## Local Development Setup

### 1. Configure Environment Variables

#### Option A: PowerShell (Windows)
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/project_employee_helpdesk"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_secure_256_bit_jwt_secret_key_here"
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-gmail-app-password"
```

#### Option B: Bash / Linux / macOS
```bash
export DB_URL="jdbc:mysql://localhost:3306/project_employee_helpdesk"
export DB_USERNAME="root"
export DB_PASSWORD="your_database_password"
export JWT_SECRET="your_secure_256_bit_jwt_secret_key_here"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-gmail-app-password"
```

#### Option C: IntelliJ IDEA / Antigravity IDE
1. Open **Run/Debug Configurations**.
2. Select `HelpdeskApplication`.
3. Under **Environment variables**, enter:
   ```
   DB_PASSWORD=your_database_password;JWT_SECRET=your_jwt_secret;MAIL_USERNAME=your_email@gmail.com;MAIL_PASSWORD=your_app_password
   ```
4. Save and run.

---

## Running the Application

### Using Maven Wrapper
```powershell
.\mvnw.cmd spring-boot:run
```

Or on Linux/macOS:
```bash
./mvnw spring-boot:run
```

Once started, the backend is accessible at:
- **Base URL**: `http://localhost:8098`
- **Swagger UI**: `http://localhost:8098/swagger-ui/index.html`
- **OpenAPI v3 Docs**: `http://localhost:8098/v3/api-docs`

---

## Running Tests and Building

### Run Tests
```powershell
.\mvnw.cmd clean test
```

### Build Executable JAR
```powershell
.\mvnw.cmd clean package
```
The packaged JAR will be created in `target/helpdesk-0.0.1-SNAPSHOT.jar`.
