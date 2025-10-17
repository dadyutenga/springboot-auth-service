# Configuration Guide - Spring Boot Authentication

This guide provides detailed instructions for configuring the authentication system.

## Table of Contents

1. [Database Configuration](#database-configuration)
2. [Email Configuration](#email-configuration)
3. [JWT Configuration](#jwt-configuration)
4. [Application Properties Reference](#application-properties-reference)
5. [Environment Variables](#environment-variables)
6. [Troubleshooting](#troubleshooting)

---

## Database Configuration

### MySQL Setup

#### Step 1: Install MySQL

Download and install MySQL 8.0+ from [https://dev.mysql.com/downloads/](https://dev.mysql.com/downloads/)

#### Step 2: Create Database

Open MySQL command line or MySQL Workbench and run:

```sql
CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Step 3: Create User (Optional but Recommended)

For production, create a dedicated database user:

```sql
CREATE USER 'auth_user'@'localhost' IDENTIFIED BY 'strong_password_here';
GRANT ALL PRIVILEGES ON auth_db.* TO 'auth_user'@'localhost';
FLUSH PRIVILEGES;
```

#### Step 4: Configure application.properties

Update these properties in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=auth_user
spring.datasource.password=strong_password_here
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### PostgreSQL Alternative

If you prefer PostgreSQL:

#### Step 1: Update pom.xml

Replace MySQL dependency with:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### Step 2: Create Database

```sql
CREATE DATABASE auth_db;
```

#### Step 3: Update application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/auth_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Database Connection Pool (Production)

For production, configure HikariCP:

```properties
# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1200000
```

---

## Email Configuration

### Gmail Setup (Recommended for Development)

#### Step 1: Enable 2-Factor Authentication

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable 2-Step Verification
3. Wait 24 hours for full activation (sometimes required)

#### Step 2: Generate App Password

1. Go to [App Passwords](https://myaccount.google.com/apppasswords)
2. Select "Mail" and "Other (Custom name)"
3. Enter "Spring Boot Auth" as the name
4. Click "Generate"
5. Copy the 16-character password (shown as: `xxxx xxxx xxxx xxxx`)

#### Step 3: Configure application.properties

```properties
# Gmail SMTP Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email Sender Details
app.mail.from=your-email@gmail.com
app.mail.from-name=Authentication Service
```

**Important:** Remove spaces from App Password when pasting!

### Outlook/Hotmail Setup

```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### AWS SES (Production)

For production environments, AWS SES is recommended:

#### Step 1: Add AWS SES dependency

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-ses</artifactId>
    <version>1.12.529</version>
</dependency>
```

#### Step 2: Configure

```properties
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=YOUR_AWS_SES_SMTP_USERNAME
spring.mail.password=YOUR_AWS_SES_SMTP_PASSWORD
spring.mail.properties.mail.transport.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### SendGrid Setup (Production Alternative)

#### Step 1: Add SendGrid dependency

```xml
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
    <version>4.9.3</version>
</dependency>
```

#### Step 2: Configure

```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=YOUR_SENDGRID_API_KEY
```

### Testing Email Configuration

Create a test controller:

```java
@RestController
@RequestMapping("/api/test")
public class EmailTestController {
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String email) {
        emailService.sendOtpEmail(email, "123456");
        return ResponseEntity.ok("Test email sent to " + email);
    }
}
```

Test with: `curl http://localhost:8080/api/test/send-test-email?email=test@example.com`

---

## JWT Configuration

### Generate Secure JWT Secret

#### Option 1: Using OpenSSL
```bash
openssl rand -base64 64
```

#### Option 2: Using Python
```python
import secrets
import base64
secret = secrets.token_bytes(64)
print(base64.b64encode(secret).decode())
```

#### Option 3: Using Java
```java
import java.security.SecureRandom;
import java.util.Base64;

public class SecretGenerator {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        System.out.println(secret);
    }
}
```

### Configure JWT Settings

```properties
# JWT Configuration
jwt.secret=YOUR_GENERATED_SECRET_KEY_HERE
jwt.expiration=86400000

# JWT expiration times (in milliseconds)
# 1 hour   = 3600000
# 24 hours = 86400000
# 7 days   = 604800000
# 30 days  = 2592000000
```

### Security Recommendations

1. **Secret Key Length**: Use at least 256 bits (32 bytes)
2. **Rotation**: Rotate secrets periodically in production
3. **Storage**: Never commit secrets to version control
4. **Environment**: Use environment variables in production

---

## Application Properties Reference

### Complete Configuration Template

```properties
# ============================================
# Application Configuration
# ============================================
spring.application.name=dada
server.port=8080

# ============================================
# Database Configuration
# ============================================
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ============================================
# JPA/Hibernate Configuration
# ============================================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# ============================================
# Email Configuration
# ============================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Application Email Settings
app.mail.from=your-email@gmail.com
app.mail.from-name=Authentication Service

# ============================================
# JWT Configuration
# ============================================
jwt.secret=404E635266556