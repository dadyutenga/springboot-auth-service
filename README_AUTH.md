# Spring Boot Authentication with Email OTP Verification

A production-ready Spring Boot 3.3.0 application implementing user authentication with email verification via One-Time Password (OTP).

## Features

- User registration with email validation
- Email verification using 6-digit OTP (expires in 5 minutes)
- JWT-based authentication
- Secure password storage with BCrypt
- MySQL database integration
- Email sending via SMTP (Gmail)
- Global exception handling
- Input validation
- Protected endpoints
- Async email sending

## Technology Stack

- **Spring Boot 3.3.0**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database operations
- **Spring Mail** - Email sending
- **MySQL** - Database
- **JWT (jjwt 0.12.3)** - Token generation
- **Lombok** - Reduce boilerplate code
- **Bean Validation** - Request validation

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Gmail account with App Password (for email sending)

## Project Structure

```
src/main/java/com/example/dada/
├── config/
│   ├── AsyncConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   └── UserController.java
├── dto/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── MessageResponse.java
│   ├── RegisterRequest.java
│   └── VerifyOtpRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InvalidOtpException.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotEnabledException.java
├── model/
│   └── User.java
├── repository/
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── JwtUtil.java
├── service/
│   ├── AuthService.java
│   ├── CustomUserDetailsService.java
│   └── EmailService.java
└── DadaApplication.java
```

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd dada
```

### 2. Configure MySQL Database

Create a MySQL database:

```sql
CREATE DATABASE auth_db;
```

### 3. Configure Application Properties

Edit `src/main/resources/application.properties`:

**Database Configuration:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

**Email Configuration:**

For Gmail, you need to create an App Password:
1. Go to Google Account settings
2. Enable 2-Factor Authentication
3. Go to Security > App Passwords
4. Generate a new app password for "Mail"

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
app.mail.from=your-email@gmail.com
```

**JWT Secret:**
Generate a secure secret key (Base64 encoded, at least 256 bits):
```properties
jwt.secret=your-secure-secret-key-here
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "message": "Registration successful. Please check your email for OTP verification."
}
```

#### 2. Verify OTP
```http
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "message": "Email verified successfully"
}
```

#### 3. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "message": "Login successful"
}
```

#### 4. Resend OTP
```http
POST /api/auth/resend-otp?email=user@example.com
```

**Response:**
```json
{
  "message": "OTP has been resent to your email"
}
```

#### 5. Test Endpoint
```http
GET /api/auth/test
```

### Protected Endpoints (Requires JWT Token)

#### Get User Profile
```http
GET /api/user/profile
Authorization: Bearer <your-jwt-token>
```

**Response:**
```json
{
  "email": "user@example.com",
  "message": "This is a protected endpoint"
}
```

## Testing with cURL

### 1. Register a new user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 2. Verify OTP (check your email for the code)
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otp":"123456"}'
```

### 3. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 4. Access protected endpoint
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Testing with Postman

1. Import the API endpoints into Postman
2. Create a collection for the authentication flow
3. For protected endpoints, add the JWT token in the Authorization header:
   - Type: Bearer Token
   - Token: <your-jwt-token>

## Database Schema

The application automatically creates the following table:

**users**
- `id` (BIGINT, PK, Auto Increment)
- `email` (VARCHAR, UNIQUE, NOT NULL)
- `password` (VARCHAR, NOT NULL)
- `otp` (VARCHAR(6), NULLABLE)
- `otp_expiry` (DATETIME, NULLABLE)
- `enabled` (BOOLEAN, NOT NULL, DEFAULT FALSE)
- `created_at` (DATETIME, NOT NULL)
- `updated_at` (DATETIME)

## Security Features

1. **Password Encryption**: BCrypt with default strength
2. **JWT Tokens**: Signed with HS256, expires in 24 hours
3. **OTP Expiration**: 5 minutes validity
4. **Stateless Authentication**: No server-side session storage
5. **Input Validation**: Bean Validation on all DTOs
6. **Global Exception Handling**: Consistent error responses

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | (required) | JWT signing key (Base64) |
| `jwt.expiration` | 86400000 | Token expiration (24 hours in ms) |
| `otp.expiration-minutes` | 5 | OTP validity period |
| `otp.length` | 6 | OTP digit length |

## Error Handling

The application returns consistent error responses:

```json
{
  "error": "Error message here"
}
```

**Common HTTP Status Codes:**
- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (invalid credentials)
- `403` - Forbidden (account not verified)
- `409` - Conflict (email already exists)
- `500` - Internal Server Error

## Email Template

The OTP email includes:
- Branded HTML template
- 6-digit OTP code prominently displayed
- Expiration warning (5 minutes)
- Security notice

## Production Considerations

1. **Environment Variables**: Store sensitive data in environment variables
2. **HTTPS**: Always use HTTPS in production
3. **Rate Limiting**: Implement rate limiting for registration/login
4. **Logging**: Configure proper logging levels
5. **Database**: Use connection pooling and optimize queries
6. **Email**: Consider using a dedicated email service (SendGrid, AWS SES)
7. **JWT Secret**: Use a strong, randomly generated secret
8. **CORS**: Configure CORS properly for frontend integration

## Troubleshooting

### Email not sending
- Check Gmail App Password is correct
- Verify Gmail security settings allow "Less secure app access" or use App Password
- Check firewall settings for port 587

### Database connection failed
- Verify MySQL is running
- Check credentials in application.properties
- Ensure database exists

### JWT token invalid
- Verify the secret key matches
- Check token expiration
- Ensure Bearer prefix is included in Authorization header

## Authentication Flow

1. **Registration**: User registers with email and password
2. **OTP Generation**: System generates 6-digit OTP, stores it with expiry time
3. **Email Sent**: OTP sent to user's email (async)
4. **Verification**: User submits OTP within 5 minutes
5. **Account Enabled**: If OTP valid, account is enabled and JWT token returned
6. **Login**: User can login with credentials
7. **Access**: Use JWT token to access protected endpoints

## Future Enhancements

- [ ] Password reset functionality
- [ ] Refresh tokens
- [ ] Role-based access control (RBAC)
- [ ] OAuth2 integration (Google, Facebook)
- [ ] Account lockout after failed attempts
- [ ] Email change verification
- [ ] Two-factor authentication (2FA)
- [ ] Audit logging

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the repository.