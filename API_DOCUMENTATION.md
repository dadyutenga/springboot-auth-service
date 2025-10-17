# Boda Boda Delivery System - Complete API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Authentication](#authentication)
4. [API Endpoints](#api-endpoints)
5. [Request/Response Examples](#request-response-examples)
6. [Database Schema](#database-schema)
7. [Setup Instructions](#setup-instructions)

---

## Overview

This is a complete Boda Boda (motorcycle taxi) delivery system with role-based access control supporting three user roles:
- **CUSTOMER**: Request trips, view trip history, make payments
- **RIDER**: View and accept trips, complete deliveries, track earnings
- **ADMIN**: Approve riders, manage users, view system statistics

### Technology Stack
- Java 17
- Spring Boot 3.3.0
- MySQL Database
- JWT Authentication
- Spring Security with Method-level Authorization
- BCrypt Password Encoding

---

## Architecture

### Clean Architecture Layers

```
Presentation Layer (Controllers)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (MySQL)
```

### Role-Based Access Control

| Role     | Permissions                                           |
|----------|-------------------------------------------------------|
| CUSTOMER | Create trips, view own trips, make payments          |
| RIDER    | View available trips, accept/complete trips, earnings|
| ADMIN    | All permissions + user/rider management + statistics |

---

## Authentication

### Registration Flow
1. User registers with email, password, fullName, phoneNumber, and role
2. System sends OTP to email
3. User verifies OTP
4. Account is activated (enabled=true, verified=true)
5. User can login and receive JWT token

### JWT Token Usage
All protected endpoints require JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## API Endpoints

### 1. Authentication Endpoints

#### POST /api/auth/register
Register a new user
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "phoneNumber": "+255712345678",
  "role": "CUSTOMER"
}
```

#### POST /api/auth/verify-otp
Verify OTP and activate account
```json
{
  "email": "john@example.com",
  "otp": "123456"
}
```

#### POST /api/auth/login
Login and receive JWT token
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### 2. User Profile Endpoints

#### GET /api/users/profile
Get current user profile (All authenticated users)

Response:
```json
{
  "id": 1,
  "fullName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "+255712345678",
  "role": "CUSTOMER",
  "verified": true,
  "createdAt": "2024-01-15T10:30:00"
}
```

#### PUT /api/users/profile
Update user profile
```json
{
  "fullName": "John Updated Doe",
  "phoneNumber": "+255712345999"
}
```

---

### 3. Trip Endpoints (Customer)

#### POST /api/trips
Create a new trip request (CUSTOMER only)
```json
{
  "pickupLocation": "Kariakoo Market",
  "dropoffLocation": "Mlimani City Mall",
  "distanceKm": 5.2,
  "pickupLatitude": -6.8162,
  "pickupLongitude": 39.2803,
  "dropoffLatitude": -6.7735,
  "dropoffLongitude": 39.2475
}
```

Response:
```json
{
  "id": 1,
  "customerId": 1,
  "customerName": "John Doe",
  "riderId": null,
  "riderName": null,
  "pickupLocation": "Kariakoo Market",
  "dropoffLocation": "Mlimani City Mall",
  "distanceKm": 5.2,
  "fare": 5.40,
  "status": "REQUESTED",
  "createdAt": "2024-01-15T11:00:00",
  "acceptedAt": null,
  "completedAt": null
}
```

**Fare Calculation:**
- Base Fare: $1.50
- Per KM Rate: $0.75
- Minimum Fare: $2.00
- Example: (1.50 + (5.2 × 0.75)) = $5.40

#### GET /api/trips/my-trips
Get all trips for current customer

Response:
```json
[
  {
    "id": 1,
    "customerId": 1,
    "customerName": "John Doe",
    "riderId": 5,
    "riderName": "Jane Rider",
    "pickupLocation": "Kariakoo Market",
    "dropoffLocation": "Mlimani City Mall",
    "distanceKm": 5.2,
    "fare": 5.40,
    "status": "COMPLETED",
    "createdAt": "2024-01-15T11:00:00",
    "acceptedAt": "2024-01-15T11:05:00",
    "completedAt": "2024-01-15T11:30:00"
  }
]
```

#### POST /api/trips/{tripId}/cancel
Cancel a trip (CUSTOMER or RIDER)

---

### 4. Rider Endpoints

#### POST /api/riders/register
Register as a rider (RIDER role required)
```json
{
  "licenseNumber": "DL-2024-12345",
  "nationalId": "NID-1234567890",
  "vehicleType": "MOTORCYCLE"
}
```

Vehicle Types: MOTORCYCLE, BICYCLE, SCOOTER, TUKTUK

Response:
```json
{
  "id": 1,
  "licenseNumber": "DL-2024-12345",
  "nationalId": "NID-1234567890",
  "vehicleType": "MOTORCYCLE",
  "status": "PENDING",
  "earnings": 0.00,
  "rating": 0.00,
  "totalTrips": 0
}
```

#### GET /api/riders/profile
Get rider profile and earnings

#### GET /api/trips/available
Get available trips for riders (RIDER only)

Response:
```json
[
  {
    "id": 2,
    "customerId": 3,
    "customerName": "Alice Customer",
    "riderId": null,
    "riderName": null,
    "pickupLocation": "University of Dar es Salaam",
    "dropoffLocation": "Sea Cliff Hotel",
    "distanceKm": 8.5,
    "fare": 7.88,
    "status": "REQUESTED",
    "createdAt": "2024-01-15T12:00:00",
    "acceptedAt": null,
    "completedAt": null
  }
]
```

#### POST /api/trips/{tripId}/accept
Accept a trip request (RIDER only)

#### POST /api/trips/{tripId}/start
Start the trip (RIDER only)

#### POST /api/trips/{tripId}/complete
Complete the trip (RIDER only)
- Automatically updates rider earnings
- Trip fare: 80% goes to rider, 20% platform fee

#### GET /api/riders/my-trips
Get all trips assigned to current rider

#### GET /api/riders/earnings
Get rider earnings summary

---

### 5. Payment Endpoints

#### POST /api/payments
Initiate payment for a trip
```json
{
  "tripId": 1,
  "method": "M_PESA",
  "phoneNumber": "+255712345678"
}
```

Payment Methods: M_PESA, AIRTEL_MONEY, TIGO_PESA, CASH

Response:
```json
{
  "id": 1,
  "tripId": 1,
  "amount": 5.40,
  "method": "M_PESA",
  "status": "PENDING",
  "transactionId": "TXN4A7B9C2D1E3F5",
  "timestamp": "2024-01-15T11:35:00"
}
```

Payment Status Flow: PENDING → SUCCESS or FAILED

#### GET /api/payments/trip/{tripId}
Get payment details for a trip

#### GET /api/payments/transaction/{transactionId}
Get payment by transaction ID

---

### 6. Admin Endpoints

#### GET /api/admin/stats
Get system statistics (ADMIN only)

Response:
```json
{
  "totalUsers": 150,
  "totalCustomers": 100,
  "totalRiders": 45,
  "totalTrips": 500,
  "completedTrips": 450,
  "pendingTrips": 10,
  "totalRevenue": 2500.00,
  "pendingRiderApprovals": 5
}
```

#### GET /api/admin/users
Get all users

#### GET /api/admin/users/customers
Get all customers

#### GET /api/admin/users/riders
Get all riders

#### GET /api/admin/riders/pending
Get pending rider approvals

Response:
```json
[
  {
    "id": 3,
    "licenseNumber": "DL-2024-54321",
    "nationalId": "NID-9876543210",
    "vehicleType": "MOTORCYCLE",
    "status": "PENDING",
    "earnings": 0.00,
    "rating": 0.00,
    "totalTrips": 0
  }
]
```

#### POST /api/admin/riders/{riderId}/approve
Approve a rider

#### POST /api/admin/riders/{riderId}/reject
Reject a rider

#### GET /api/admin/trips
Get all trips in the system

---

## Request/Response Examples

### Complete Trip Workflow

#### 1. Customer Creates Trip
```bash
POST /api/trips
Authorization: Bearer <customer_token>

{
  "pickupLocation": "Kariakoo Market",
  "dropoffLocation": "Mlimani City Mall",
  "distanceKm": 5.2
}

Response: 201 Created
{
  "id": 1,
  "status": "REQUESTED",
  "fare": 5.40,
  ...
}
```

#### 2. Rider Views Available Trips
```bash
GET /api/trips/available
Authorization: Bearer <rider_token>

Response: 200 OK
[
  {
    "id": 1,
    "status": "REQUESTED",
    "fare": 5.40,
    ...
  }
]
```

#### 3. Rider Accepts Trip
```bash
POST /api/trips/1/accept
Authorization: Bearer <rider_token>

Response: 200 OK
{
  "id": 1,
  "status": "ACCEPTED",
  "riderId": 5,
  "riderName": "Jane Rider",
  "acceptedAt": "2024-01-15T11:05:00",
  ...
}
```

#### 4. Rider Starts Trip
```bash
POST /api/trips/1/start
Authorization: Bearer <rider_token>

Response: 200 OK
{
  "id": 1,
  "status": "IN_PROGRESS",
  ...
}
```

#### 5. Rider Completes Trip
```bash
POST /api/trips/1/complete
Authorization: Bearer <rider_token>

Response: 200 OK
{
  "id": 1,
  "status": "COMPLETED",
  "completedAt": "2024-01-15T11:30:00",
  ...
}
```

Rider earnings updated: 5.40 × 0.80 = $4.32

#### 6. Customer Makes Payment
```bash
POST /api/payments
Authorization: Bearer <customer_token>

{
  "tripId": 1,
  "method": "M_PESA",
  "phoneNumber": "+255712345678"
}

Response: 201 Created
{
  "transactionId": "TXN4A7B9C2D1E3F5",
  "status": "PENDING",
  ...
}
```

---

## Database Schema

### users
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) UNIQUE,
    role ENUM('CUSTOMER', 'RIDER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    otp VARCHAR(6),
    otp_expiry DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
```

### rider_profiles
```sql
CREATE TABLE rider_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    national_id VARCHAR(50) UNIQUE NOT NULL,
    vehicle_type ENUM('MOTORCYCLE', 'BICYCLE', 'SCOOTER', 'TUKTUK') NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    earnings DECIMAL(10,2) DEFAULT 0.00,
    rating DECIMAL(3,2) DEFAULT 0.00,
    total_trips INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### trips
```sql
CREATE TABLE trips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    rider_id BIGINT,
    pickup_location VARCHAR(255) NOT NULL,
    dropoff_location VARCHAR(255) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    status ENUM('REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL,
    pickup_latitude DOUBLE,
    pickup_longitude DOUBLE,
    dropoff_latitude DOUBLE,
    dropoff_longitude DOUBLE,
    accepted_at DATETIME,
    completed_at DATETIME,
    cancelled_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (rider_id) REFERENCES users(id)
);
```

### payments
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_id BIGINT UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method ENUM('M_PESA', 'AIRTEL_MONEY', 'TIGO_PESA', 'CASH') NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL,
    transaction_id VARCHAR(50) UNIQUE,
    phone_number VARCHAR(20),
    timestamp DATETIME NOT NULL,
    completed_at DATETIME,
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);
```

---

## Setup Instructions

### 1. Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Git

### 2. Database Setup
```sql
CREATE DATABASE boda_boda_db;
USE boda_boda_db;
```

### 3. Configuration

Update `application.properties`:
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/boda_boda_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT
jwt.secret=your-256-bit-secret-key-here
jwt.expiration=86400000

# Email (Gmail)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 4. Build and Run
```bash
# Clone the repository
git clone <repository-url>
cd dada

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Create Admin User

First, register a user via API, then update the database:
```sql
UPDATE users SET role = 'ADMIN', verified = TRUE, enabled = TRUE 
WHERE email = 'admin@example.com';
```

---

## Testing the API

### Using Postman

1. **Import the Postman Collection** (if available)
2. **Set up environment variables:**
   - `base_url`: http://localhost:8080
   - `token`: (will be set after login)

### Sample Test Flow

1. **Register Customer:**
```bash
POST {{base_url}}/api/auth/register
{
  "email": "customer@test.com",
  "password": "Test123!",
  "fullName": "Test Customer",
  "phoneNumber": "+255712345678",
  "role": "CUSTOMER"
}
```

2. **Verify OTP** (check email or database)
```bash
POST {{base_url}}/api/auth/verify-otp
{
  "email": "customer@test.com",
  "otp": "123456"
}
```

3. **Login:**
```bash
POST {{base_url}}/api/auth/login
{
  "email": "customer@test.com",
  "password": "Test123!"
}
```

4. **Save the token and use in subsequent requests**

---

## Error Responses

### Common HTTP Status Codes

| Code | Description |
|------|-------------|
| 200  | Success |
| 201  | Created |
| 400  | Bad Request (validation error) |
| 401  | Unauthorized (missing/invalid token) |
| 403  | Forbidden (insufficient permissions) |
| 404  | Not Found |
| 500  | Internal Server Error |

### Error Response Format
```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/trips"
}
```

---

## Security Best Practices

1. **Password Requirements:**
   - Minimum 8 characters
   - Must contain uppercase, lowercase, number, and special character

2. **JWT Token:**
   - Expires in 24 hours
   - Store securely on client side
   - Send in Authorization header

3. **Role-Based Access:**
   - Enforced at method level with `@PreAuthorize`
   - Cannot access endpoints without proper role

4. **OTP Verification:**
   - OTP expires in 5 minutes
   - Must verify before login

---

## Additional Features

### Swagger/OpenAPI Documentation
Access API documentation at: `http://localhost:8080/swagger-ui.html`

### CORS Configuration
Configured to allow all origins in development. Update for production:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // Configure allowed origins
    }
}
```

---

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check MySQL is running
   - Verify credentials in application.properties
   - Ensure database exists

2. **Email OTP Not Sending**
   - Check email configuration
   - Use Gmail App Password (not regular password)
   - Check SMTP settings

3. **401 Unauthorized**
   - Check JWT token is valid
   - Ensure token is in Authorization header
   - Token may have expired

4. **403 Forbidden**
   - User doesn't have required role
   - Check role assignment in database

---

## Future Enhancements

1. **Real-time Features:**
   - WebSocket for live trip tracking
   - Push notifications for riders

2. **Payment Integration:**
   - Integrate actual M-Pesa API
   - Add more payment gateways

3. **Advanced Features:**
   - Rider ratings and reviews
   - Trip history analytics
   - Surge pricing
   - Promo codes and discounts

4. **Mobile App:**
   - Android/iOS applications
   - GPS tracking integration

---

## Support

For questions or issues:
- Email: support@bodaboda.com
- Documentation: [GitHub Wiki](https://github.com/your-repo/wiki)
- Issue Tracker: [GitHub Issues](https://github.com/your-repo/issues)

---

## License

This project is licensed under the MIT License.

---

**Version:** 1.0.0  
**Last Updated:** January 2024