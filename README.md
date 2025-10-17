# Boda Boda Delivery System Backend

A production-ready Spring Boot 3.x backend for managing a motorcycle delivery marketplace with role-based access, trip lifecycle orchestration, live tracking hooks, reporting, and bi-directional ratings.

![Java](https://img.shields.io/badge/Java-17-orange.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)

---

## 🚀 Features at a Glance

| Area | Highlights |
|------|------------|
| Authentication | JWT auth, email OTP verification, BCrypt hashing, role-based access (`ADMIN`, `RIDER`, `CUSTOMER`) |
| Trip Lifecycle | UUID entities, REQUESTED → ACCEPTED → PICKED_UP → IN_TRANSIT → DELIVERED, cancellation rules, rider assignment |
| Live Tracking | WebSocket (STOMP) endpoint + Redis cache publisher for future real-time dashboards |
| Reporting | Customer incident reports, admin triage workflow (pending → under review → resolved) |
| Ratings | One rating per participant per trip, automatic average calculation on `User` and `RiderProfile` |
| Admin Suite | User activation toggles, rider oversight, global view of trips, reports, and ratings |
| Tooling | Global CORS, OpenAPI/Swagger docs, PostgreSQL-ready config, Redis integration, mail placeholders |

---

## 🧱 Domain Model (all IDs are UUIDs)

- `User` – full name, email, password, phone, role, enabled/verified flags, OTP metadata, aggregated rating
- `RiderProfile` – one-to-one with `User`, licensing data, vehicle type, status, total earnings, rating, last known location
- `Trip` – references customer & rider, pickup/dropoff, distance, fare, status progression timestamps
- `Payment` – one-to-one with trip, amount, method (M-PESA, AIRTEL, TIGO), transaction state, timestamp
- `Report` – incident reports linked to trip & reporter with triage status
- `Rating` – reviewer/target references with unique constraint per trip to prevent duplicates

---

## 📡 Delivery Tracking Hooks

- `WebSocketConfig` exposes `/ws/tracking` (SockJS fallback) with `/topic/trips/{tripId}` subscriptions
- `DeliveryTrackingPublisher` pushes `TripStatusNotification` messages and caches latest status in Redis (`trip-status` hash)
- Designed for dashboards/mobile apps to consume in near real-time

---

## 🛡️ Security & Access Rules

- `@EnableMethodSecurity` with fine-grained `@PreAuthorize` annotations
- Customers: create trips, cancel pending trips, submit reports, rate riders
- Riders: view/accept available trips, update status through delivery stages, rate customers
- Admins: manage accounts, inspect reports, view system-wide metrics, resolve incidents

---

## 📁 Project Layout

```
src/main/java/com/example/dada
├── config/              # Security, OpenAPI, WebSocket, Redis, CORS
├── controller/          # Auth, User, Trip, Rating, Report, Admin endpoints
├── dto/                 # DTOs for requests/responses & websocket payloads
├── enums/               # Roles, payment, trip & report status enums
├── exception/           # Custom exceptions + global handler
├── model/               # JPA entities using UUID identifiers
├── repository/          # Spring Data repositories
├── security/            # JWT utilities & filter
├── service/             # Core business services (Auth, Trip, Rating, Report, Admin, etc.)
└── util/                # Helpers (e.g. fare calculations)
```

---

## ⚙️ Configuration

Key settings live in `src/main/resources/application.yml` and are environment-variable friendly.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/dada}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}
  mail:
    username: ${SPRING_MAIL_USERNAME:your-email@example.com}
    password: ${SPRING_MAIL_PASSWORD:change-me}

jwt:
  secret: ${JWT_SECRET:change-me}
  expiration: ${JWT_EXPIRATION:86400000}
```

> **Note:** Update mail and JWT secrets before deploying. Redis is optional but recommended for live tracking.

---

## 🧪 Example Payloads

### Create Trip (Customer)
```http
POST /api/trips
Authorization: Bearer <token>
Content-Type: application/json

{
  "pickupLocation": "Kenyatta Avenue, Nairobi",
  "dropoffLocation": "Westlands Mall, Nairobi",
  "distanceKm": 7.5,
  "fare": 350.00,
  "pickupLatitude": -1.2833,
  "pickupLongitude": 36.8167,
  "dropoffLatitude": -1.2681,
  "dropoffLongitude": 36.8110
}
```

### Submit Report (Customer)
```http
POST /api/report
Authorization: Bearer <token>
Content-Type: application/json

{
  "tripId": "9b6a7df8-838d-4d57-8c40-3f8f26ae1a6e",
  "reason": "Damaged parcel",
  "description": "The package arrived soaked despite clear weather."
}
```

### Rate Counterparty (Customer or Rider)
```http
POST /api/ratings
Authorization: Bearer <token>
Content-Type: application/json

{
  "tripId": "9b6a7df8-838d-4d57-8c40-3f8f26ae1a6e",
  "targetUserId": "2f9c3cc5-47fd-49f7-9a7d-109c7796f8e1",
  "ratingValue": 5,
  "comment": "Excellent communication and timely delivery."
}
```

---

## 🛠️ Getting Started

1. **Clone & Build**
   ```bash
   git clone <repository-url>
   cd springboot-auth-service
   ./mvnw clean install
   ```

2. **Run the Service**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Explore the API**
   - Swagger UI: `http://localhost:8080/api/swagger-ui.html`
   - OpenAPI Spec: `http://localhost:8080/api/docs`

---

## ✅ Test Matrix (suggested)

| Command | Purpose |
|---------|---------|
| `./mvnw test` | Run unit/integration tests |
| `./mvnw verify -Pprod` | Production profile build |
| `docker-compose up` | Optional Postgres/Redis stack |

---

## 📬 Support & Contributions

Issues and contributions are welcome! Please open a GitHub issue with detailed reproduction steps and expected behaviour.

---

© 2024 Boda Boda Delivery System. All rights reserved.
