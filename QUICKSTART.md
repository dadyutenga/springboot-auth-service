# Quick Start Guide - Spring Boot Authentication with Email OTP

This guide will help you get the authentication system up and running in 5 minutes.

## Prerequisites Checklist

- [ ] Java 17+ installed
- [ ] Maven 3.6+ installed
- [ ] MySQL 8.0+ running
- [ ] Gmail account with App Password ready

## Step-by-Step Setup

### Step 1: Database Setup (1 minute)

Open MySQL and create the database:

```sql
CREATE DATABASE auth_db;
```

### Step 2: Configure Email (2 minutes)

1. **Get Gmail App Password:**
   - Go to https://myaccount.google.com/security
   - Enable 2-Factor Authentication
   - Navigate to "App passwords"
   - Generate password for "Mail"
   - Copy the 16-character password

2. **Update application.properties:**

Open `src/main/resources/application.properties` and update these lines:

```properties
# Database credentials (if different from defaults)
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

# Email configuration
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
app.mail.from=your-email@gmail.com
```

### Step 3: Build and Run (2 minutes)

```bash
# Navigate to project directory
cd dada

# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

Wait for the application to start. You should see:
```
Started DadaApplication in X.XXX seconds
```

## Testing the Application

### Test 1: Register a New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

**Expected Response:**
```json
{
  "message": "Registration successful. Please check your email for OTP verification."
}
```

**Check your email** for the OTP code (6 digits).

### Test 2: Verify OTP

```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otp":"123456"}'
```

Replace `123456` with the actual OTP from your email.

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "test@example.com",
  "message": "Email verified successfully"
}
```

**Copy the JWT token** from the response.

### Test 3: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "test@example.com",
  "message": "Login successful"
}
```

### Test 4: Access Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

Replace `YOUR_JWT_TOKEN_HERE` with the token from previous step.

**Expected Response:**
```json
{
  "email": "test@example.com",
  "message": "This is a protected endpoint"
}
```

## Using Postman

### Import Collection

1. Open Postman
2. Create a new collection called "Authentication API"
3. Add these requests:

**1. Register**
- Method: POST
- URL: `http://localhost:8080/api/auth/register`
- Body (JSON):
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**2. Verify OTP**
- Method: POST
- URL: `http://localhost:8080/api/auth/verify-otp`
- Body (JSON):
```json
{
  "email": "test@example.com",
  "otp": "123456"
}
```

**3. Login**
- Method: POST
- URL: `http://localhost:8080/api/auth/login`
- Body (JSON):
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**4. Get Profile (Protected)**
- Method: GET
- URL: `http://localhost:8080/api/user/profile`
- Authorization: Bearer Token
- Token: `<paste-jwt-token>`

## Complete Authentication Flow

```
1. User registers → POST /api/auth/register
   ↓
2. System sends OTP email (6 digits, expires in 5 min)
   ↓
3. User checks email and copies OTP
   ↓
4. User verifies OTP → POST /api/auth/verify-otp
   ↓
5. System enables account and returns JWT token
   ↓
6. User can now login → POST /api/auth/login
   ↓
7. Use JWT token to access protected endpoints
```

## Common Issues & Solutions

### Issue: "Failed to send OTP email"

**Solution:**
- Verify Gmail App Password is correct (16 characters, no spaces)
- Check that 2FA is enabled on your Gmail account
- Ensure port 587 is not blocked by firewall

### Issue: "Access denied" / "Unauthorized"

**Solution:**
- Make sure you include `Bearer ` prefix before token
- Check token hasn't expired (24 hour validity)
- Verify account is enabled (OTP verified)

### Issue: "Database connection failed"

**Solution:**
- Verify MySQL is running: `mysql --version`
- Check credentials in application.properties
- Ensure database `auth_db` exists

### Issue: "OTP expired"

**Solution:**
- Use the resend OTP endpoint:
```bash
curl -X POST "http://localhost:8080/api/auth/resend-otp?email=test@example.com"
```
- Check your email for the new OTP

## Default Configuration

| Setting | Default Value |
|---------|---------------|
| Server Port | 8080 |
| Database Name | auth_db |
| OTP Length | 6 digits |
| OTP Validity | 5 minutes |
| JWT Expiration | 24 hours |
| Password Min Length | 6 characters |

## Next Steps

1. **Review the code structure** - See `README_AUTH.md` for detailed documentation
2. **Customize email template** - Edit `EmailService.java`
3. **Add more endpoints** - Create new controllers for your business logic
4. **Implement password reset** - Extend `AuthService`
5. **Add roles and permissions** - Implement RBAC
6. **Configure CORS** - For frontend integration
7. **Set up production environment** - Use environment variables

## API Endpoints Summary

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/auth/register` | POST | No | Register new user |
| `/api/auth/verify-otp` | POST | No | Verify email with OTP |
| `/api/auth/login` | POST | No | Login with credentials |
| `/api/auth/resend-otp` | POST | No | Resend OTP email |
| `/api/auth/test` | GET | No | Test endpoint |
| `/api/user/profile` | GET | Yes | Get user profile |

## Environment Variables (Production)

For production, use environment variables instead of hardcoding:

```bash
export DB_URL=jdbc:mysql://localhost:3306/auth_db
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export JWT_SECRET=your-secure-secret-key
```

Update `application.properties`:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
jwt.secret=${JWT_SECRET}
```

## Support

- Detailed Documentation: See `README_AUTH.md`
- Check application logs for errors
- Enable debug logging: `logging.level.com.example.dada=DEBUG`

**You're all set!** 🎉

Start building your application with secure authentication!