# Boda Boda Delivery System - Complete Setup Guide

## 🚀 Quick Start Guide

This guide will help you set up and run the Boda Boda Delivery System in under 15 minutes.

---

## 📋 Prerequisites

Before you begin, ensure you have:

- ✅ Java 17 or higher
- ✅ Maven 3.6 or higher
- ✅ MySQL 8.0 or higher (or PostgreSQL)
- ✅ Git
- ✅ Your favorite IDE (IntelliJ IDEA, Eclipse, or VS Code)
- ✅ Postman or similar API testing tool (optional)

Check your versions:
```bash
java -version
mvn -version
mysql --version
```

---

## 📥 Step 1: Clone and Navigate

```bash
cd dada/dada
```

---

## 🗄️ Step 2: Database Setup

### Option A: MySQL

1. **Start MySQL Server**
   ```bash
   # Linux/Mac
   sudo service mysql start
   
   # Windows
   # MySQL should start automatically or use MySQL Workbench
   ```

2. **Create Database**
   ```sql
   mysql -u root -p
   
   CREATE DATABASE boda_boda_db;
   USE boda_boda_db;
   ```

3. **Verify Database**
   ```sql
   SHOW DATABASES;
   ```

### Option B: PostgreSQL

1. **Create Database**
   ```bash
   psql -U postgres
   
   CREATE DATABASE boda_boda_db;
   \q
   ```

2. **Update application.properties** (see Step 3)

---

## ⚙️ Step 3: Configuration

### 3.1 Database Configuration

Edit `src/main/resources/application.properties`:

**For MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/boda_boda_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

**For PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/boda_boda_db
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### 3.2 Email Configuration (Gmail)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

app.mail.from=your-email@gmail.com
app.mail.from-name=Boda Boda Delivery
```

**⚠️ Important: Gmail App Password**
- Don't use your regular Gmail password
- Generate an App Password:
  1. Go to Google Account Settings
  2. Security → 2-Step Verification (enable it)
  3. App Passwords → Generate new password
  4. Use this password in application.properties

### 3.3 JWT Configuration

Generate a secure secret key (256-bit):
```bash
# Linux/Mac
openssl rand -base64 32

# Or use online generator: https://generate-secret.vercel.app/32
```

Update in application.properties:
```properties
jwt.secret=YOUR_GENERATED_SECRET_KEY_HERE
jwt.expiration=86400000
```

### 3.4 Complete Configuration File

Here's the complete `application.properties`:

```properties
# Application Name
spring.application.name=boda-boda-delivery

# Server Configuration
server.port=8080

# Database Configuration (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/boda_boda_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Mail Configuration (Gmail SMTP)
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
app.mail.from-name=Boda Boda Delivery