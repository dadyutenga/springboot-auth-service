# Boda Boda Delivery System

A comprehensive Spring Boot 3.x backend system for a motorcycle taxi (Boda Boda) delivery service with role-based access control, JWT authentication, and complete trip management.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🎯 Overview

This system provides a complete backend solution for managing a Boda Boda delivery service with three distinct user roles:

- **CUSTOMER** - Request trips, view history, make payments
- **RIDER** - Accept trips, complete deliveries, track earnings
- **ADMIN** - Approve riders, manage users, view system statistics

## ✨ Key Features

### Authentication & Security
- ✅ JWT-based authentication
- ✅ Email OTP verification
- ✅ BCrypt password encryption
- ✅ Role-based access control with `@PreAuthorize`
- ✅ Method-level security

### Trip Management
- ✅ Complete trip lifecycle (REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED)
- ✅ Automatic fare calculation
- ✅ Real-time status tracking
- ✅ Trip cancellation by customer or rider
- ✅ GPS coordinates support

### Rider Management
- ✅ Rider registration with license verification
- ✅ Admin approval workflow
- ✅ Earnings tracking (80% fare commission)
- ✅ Trip history and statistics
- ✅ Vehicle type management

### Payment System
- ✅ Mock mobile money integration (M-Pesa, Airtel Money, Tigo Pesa)
- ✅ Transaction tracking
- ✅ Async payment processing
- ✅ Payment status monitoring

### Admin Dashboard
- ✅ System-wide statistics
- ✅ User management
- ✅ Rider approval system
- ✅ Trip monitoring
- ✅ Revenue tracking

---

## 🏗️ Architecture

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.0 |
| Security | Spring Security + JWT |
| Database | MySQL 8.0 / PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| API Docs | Swagger/OpenAPI 3.0 |
| Build Tool | Maven |

### Project Structure

```
dada/
├── src/main/java/com/example/dada/
│   ├── config/              # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/          # REST Controllers
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── TripController.java
│   │   ├── RiderController.java
│   │   ├── AdminController.java
│   │   └── PaymentController.java
│   ├── dto/                 # Data Transfer Objects
│   │   ├── request/
│   │   └── response/
│   ├── enums/               # Enumerations
│   │   ├── UserRole.java
│   │   ├── TripStatus.java
│   │   ├── RiderStatus.java
│   │   ├── VehicleType.java
│   │   ├── PaymentMethod.java
│   │   └── PaymentStatus.java
│   ├── exception/           # Exception Handling
│   ├── model/               # JPA Entities
│   │   ├── User.java
│   │   ├── RiderProfile.java
│   │   ├── Trip.java
│   │   └── Payment.java
│   ├── repository/          # Data Access Layer
│   ├── security/            # JWT & Security
│   ├── service/             # Business Logic
│   │   ├── UserService.java
│   │   ├── RiderService.java
│   │   ├── TripService.java
│   │   ├── PaymentService.java
│   │   └── AdminService.java
│   └── util/                # Utilities
│       └── FareCalculator.java
└── src/main/resources/
    ├── application.properties
    └── ...
```

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+ or PostgreSQL 12+
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd dada/dada
   ```

2. **Create database**
   ```sql
   CREATE DATABASE boda_boda_db;
   ```

3. **Configure application**
   
   Edit `src/main/resources/application.properties`:
   ```properties
   # Database
   spring.datasource.url=jdbc:mysql://localhost:3306/boda_boda_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
   # JWT
   jwt.secret=your-secret-key-here
   jwt.expiration=86400000
   
   # Email
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-app-password
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**
   - API Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - OpenAPI Spec: `http://localhost:8080/v3/api-docs`

---

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "phoneNumber": "+255712345678",
  "role": "CUSTOMER"
}
```

#### Verify OTP
```http
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

### Trip Endpoints

#### Create Trip (Customer)
```http
POST /api/trips
Authorization: Bearer <token>
Content-Type: application/json

{
  "pickupLocation": "Kariakoo Market",
  "dropoffLocation": "Mlimani City Mall",
  "distanceKm": 5.2,
  "pickupLatitude": -6.8162,
  "pickupLongitude": 39.2803,
  "dropoffLatitude": -6.7735,
  "dropoffLongitude": 39.2475
}

Response:
{
  "id": 1,
  "fare": 5.40,
  "status": "REQUESTED",
  ...
}
```

#### Get Available Trips (Rider)
```http
GET /api/trips/available
Authorization: Bearer <rider_token>
```

#### Accept Trip (Rider)
```http
POST /api/trips/1/accept
Authorization: Bearer <rider_token>
```

#### Complete Trip (Rider)
```http
POST /api/trips/1/complete
Authorization: Bearer <rider_token>
```

### Rider Endpoints

#### Register as Rider
```http
POST /api/riders/register
Authorization: Bearer <token>
Content-Type: application/json

{
  "licenseNumber": "DL-2024-12345",
  "nationalId": "NID-1234567890",
  "vehicleType": "MOTORCYCLE"
}
```

#### View Earnings
```http
GET /api/riders/earnings
Authorization: Bearer <rider_token>
```

### Admin Endpoints

#### Get System Statistics
```http
GET /api/admin/stats
Authorization: Bearer <admin_token>

Response:
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

#### Approve Rider
```http
POST /api/admin/riders/1/approve
Authorization: Bearer <admin_token>
```

For complete API documentation, visit the Swagger UI at `http://localhost:8080/swagger-ui.html`

---

## 💰 Fare Calculation

The system uses a transparent fare calculation algorithm:

```
Base Fare: $1.50
Rate per KM: $0.75
Minimum Fare: $2.00

Total Fare = Base Fare + (Distance × Rate per KM)
```

**Commission Structure:**
- Rider receives: 80% of fare
- Platform fee: 20% of fare

**Example:**
- Distance: 5.2 km
- Calculated Fare: $1.50 + (5.2 × $0.75) = **$5.40**
- Rider Earnings: $5.40 × 0.80 = **$4.32**
- Platform Fee: $5.40 × 0.20 = **$1.08**

---

## 🔐 Security

### Authentication Flow
1. User registers with email, password, and role
2. System sends OTP to email
3. User verifies OTP
4. Account is activated (verified = true)
5. User logs in and receives JWT token
6. Token must be included in Authorization header for protected endpoints

### Role-Based Permissions

| Feature | CUSTOMER | RIDER | ADMIN |
|---------|----------|-------|-------|
| Register/Login | ✅ | ✅ | ✅ |
| Create Trip | ✅ | ❌ | ❌ |
| Accept Trip | ❌ | ✅ | ❌ |
| Complete Trip | ❌ | ✅ | ❌ |
| View Earnings | ❌ | ✅ | ❌ |
| Make Payment | ✅ | ❌ | ❌ |
| Approve Riders | ❌ | ❌ | ✅ |
| View Statistics | ❌ | ❌ | ✅ |
| Manage Users | ❌ | ❌ | ✅ |

---

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) UNIQUE,
    role ENUM('CUSTOMER', 'RIDER', 'ADMIN') NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    otp VARCHAR(6),
    otp_expiry DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
```

### Rider Profiles Table
```sql
CREATE TABLE rider_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    national_id VARCHAR(50) UNIQUE NOT NULL,
    vehicle_type ENUM('MOTORCYCLE', 'BICYCLE', 'SCOOTER', 'TUKTUK'),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL,
    earnings DECIMAL(10,2) DEFAULT 0.00,
    rating DECIMAL(3,2) DEFAULT 0.00,
    total_trips INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Trips Table
```sql
CREATE TABLE trips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    rider_id BIGINT,
    pickup_location VARCHAR(255) NOT NULL,
    dropoff_location VARCHAR(255) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    status ENUM('REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'),
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

### Payments Table
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_id BIGINT UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method ENUM('M_PESA', 'AIRTEL_MONEY', 'TIGO_PESA', 'CASH'),
    status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'),
    transaction_id VARCHAR(50) UNIQUE,
    phone_number VARCHAR(20),
    timestamp DATETIME NOT NULL,
    completed_at DATETIME,
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);
```

---

## 🧪 Testing

### Manual Testing Workflow

1. **Register Customer**
   ```bash
   POST /api/auth/register (role: CUSTOMER)
   POST /api/auth/verify-otp
   POST /api/auth/login
   ```

2. **Register Rider**
   ```bash
   POST /api/auth/register (role: RIDER)
   POST /api/auth/verify-otp
   POST /api/auth/login
   POST /api/riders/register
   ```

3. **Admin Approves Rider**
   ```bash
   POST /api/auth/login (admin)
   GET /api/admin/riders/pending
   POST /api/admin/riders/{id}/approve
   ```

4. **Customer Creates Trip**
   ```bash
   POST /api/trips
   ```

5. **Rider Accepts and Completes Trip**
   ```bash
   GET /api/trips/available
   POST /api/trips/{id}/accept
   POST /api/trips/{id}/start
   POST /api/trips/{id}/complete
   ```

6. **Customer Makes Payment**
   ```bash
   POST /api/payments
   ```

---

## 📖 Additional Documentation

- **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** - Detailed implementation steps
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Complete API reference
- **[CODE_REFERENCE.md](CODE_REFERENCE.md)** - All code implementations
- **[PROJECT_STATUS.md](PROJECT_STATUS.md)** - Current project status
- **[SETUP_GUIDE.md](SETUP_GUIDE.md)** - Step-by-step setup instructions

---

## 🛠️ Configuration

### Gmail SMTP Setup

1. Enable 2-Step Verification in your Google Account
2. Generate App Password:
   - Go to Google Account → Security → App Passwords
   - Select "Mail" and "Other"
   - Copy the generated password
3. Use the app password in `application.properties`

### JWT Secret Key

Generate a secure secret key:
```bash
# Linux/Mac
openssl rand -base64 32

# Or use online generator
```

---

## 🚢 Deployment

### Production Checklist

- [ ] Update CORS to allow specific origins only
- [ ] Use environment variables for sensitive data
- [ ] Set up production database
- [ ] Configure production email service
- [ ] Enable HTTPS
- [ ] Set up monitoring and logging
- [ ] Configure rate limiting
- [ ] Perform security audit
- [ ] Set up database backups
- [ ] Configure CI/CD pipeline

### Environment Variables

```bash
export DB_URL=jdbc:mysql://prod-db:3306/boda_boda_db
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export JWT_SECRET=your_production_secret
export MAIL_USERNAME=noreply@yourdomain.com
export MAIL_PASSWORD=app_password
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Your Name** - *Initial work*

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- JWT.io for authentication standards
- All contributors and testers

---

## 📞 Support

For issues, questions, or contributions:

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Email**: support@bodaboda.com
- **Documentation**: See additional markdown files in this directory

---

## 🔄 Version History

- **v1.0.0** (2024-01) - Initial release
  - Complete authentication system
  - Role-based access control
  - Trip management
  - Payment processing
  - Admin dashboard

---

## 🎯 Future Enhancements

- [ ] WebSocket for real-time tracking
- [ ] Google Maps integration
- [ ] Push notifications
- [ ] Real M-Pesa/Airtel Money API integration
- [ ] Rating and review system
- [ ] Advanced analytics dashboard
- [ ] Mobile app (Android/iOS)
- [ ] Promo codes and discounts
- [ ] Multi-language support
- [ ] Driver verification system

---

**Built with ❤️ using Spring Boot 3.x**

---

## 📊 Quick Stats

- **Lines of Code**: ~5,000+
- **API Endpoints**: 25+
- **Database Tables**: 4
- **Supported Roles**: 3
- **Payment Methods**: 4
- **Trip Statuses**: 5

---

For more information, see the comprehensive documentation files included in this project.