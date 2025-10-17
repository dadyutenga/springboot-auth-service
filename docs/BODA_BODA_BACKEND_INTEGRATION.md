# Boda Boda Delivery System – Backend Integration & API Guide

This document is the canonical reference for integrating with the Boda Boda Delivery System backend. It covers the overall architecture, domain model, API surface, WhatsApp automation layer, and environment configuration needed to operate the Spring Boot 3.x service in production.

---

## 1. System Overview

### 1.1 High-level architecture
```
                   +--------------------+
                   | React Admin (Web)  |
                   +---------+----------+
                             |
                  HTTPS / REST & WebSocket
                             |
 +--------------+    +-------v-------+        +------------------+
 | Flutter App  |----> Spring Boot   |------->| PostgreSQL 15    |
 | (Rider)      |    | API Gateway   |        | (Transactions)   |
 +--------------+    | & Service     |        +------------------+
                     | Layer         |
                     |               |----+
                     +-------+-------+    |   +------------------+
                             |            +-->| Redis (tracking   |
         WhatsApp Cloud API  |                | cache, pub/sub)   |
             / Twilio        |                +------------------+
                             |
                     +-------v-------+
                     | WhatsApp      |
                     | Integration   |
                     | (Webhook +    |
                     | outbound API) |
                     +-------+-------+
                             |
                       Email (SMTP)
```

### 1.2 Service composition
* **Spring Boot 3.2** REST service with layered architecture (`controller` → `service` → `repository`).
* **PostgreSQL** primary datastore for users, trips, reports, ratings, rider profiles, and payment metadata.
* **Redis (optional)** cache and pub/sub broker for trip status broadcasting (`DeliveryTrackingPublisher`).
* **Spring Security 6** with JWT bearer authentication (`JwtAuthenticationFilter`, `SecurityConfig`).
* **Async / scheduling** for email dispatch (`AsyncConfig`, `EmailService`).
* **WebSocket/STOMP** for live trip tracking (`WebSocketConfig`, `/ws/tracking`).
* **WhatsApp Cloud API/Twilio** bridge for conversational flows (`whatsapp` package).
* **OpenAPI 3** documentation via springdoc (`OpenApiConfig`).

### 1.3 External dependencies
* **SMTP provider** (e.g., SendGrid, SES) for OTP emails.
* **WhatsApp Business Platform** or Twilio WhatsApp API for bidirectional messaging.
* Optional **payment gateways** (future roadmap) and **geolocation APIs** if distance is calculated server-side.

---

## 2. Project Structure

The Java sources live under `src/main/java/com/example/dada/`. Key packages:

```
com/example/dada/
├── DadaApplication.java
├── config/
│   ├── AsyncConfig.java           # @EnableAsync + executor tuning
│   ├── CorsConfig.java            # Centralised CORS policy
│   ├── OpenApiConfig.java         # springdoc configuration
│   ├── RedisConfig.java           # Lettuce connection + template beans
│   ├── SecurityConfig.java        # HTTP security, JWT filter, method security
│   └── WebSocketConfig.java       # STOMP endpoints & broker
├── controller/
│   ├── AdminController.java       # /api/admin/* administration APIs
│   ├── AuthController.java        # /api/auth/* authentication & OTP endpoints
│   ├── RatingController.java      # /api/ratings/*
│   ├── ReportController.java      # /api/report + admin report actions
│   ├── TripController.java        # /api/trips/* lifecycle endpoints
│   └── UserController.java        # /api/users/me profile APIs
├── dto/                           # Request/response payloads & notifications
├── entity/ (model/)               # JPA entities (User, Trip, Rating, Report...)
├── repository/                    # Spring Data JPA repositories
├── security/                      # JWT utilities, filters, websocket interceptors
├── service/
│   ├── AuthService.java           # Registration, OTP, login, JWT issuance
│   ├── DeliveryTrackingPublisher  # Trip status broadcasting to Redis/WebSocket
│   ├── RatingService.java         # Rating flows and aggregate updates
│   ├── ReportService.java         # Reporting module
│   ├── RiderService.java          # Rider profile management
│   ├── TripService.java           # Trip lifecycle orchestration
│   └── UserService.java           # Profile and context helpers
├── util/
│   ├── FareCalculator.java        # Distance-based fare computation helper
│   └── SensitiveDataMasker.java   # Logging helper for PII masking
└── whatsapp/
    ├── WhatsAppController.java    # Webhook endpoints
    ├── WhatsAppService.java       # Conversation orchestrator
    ├── WhatsAppClient.java        # Outbound HTTP client
    ├── MessageParser.java         # Natural-language command parser
    ├── ConversationState.java     # Stateful chat context per sender
    └── WhatsAppWebhookRequest.java# Incoming webhook schema
```

DTOs under `dto/` include `RegisterRequest`, `LoginRequest`, `TripRequestDto`, `TripStatusUpdateDto`, `ReportRequestDto`, `RatingRequestDto`, `TripStatusNotification`, etc. JPA entities live in `model/` (e.g., `User`, `Trip`, `RiderProfile`, `Report`, `Rating`, `Payment`).

---

## 3. Authentication Module

### 3.1 REST endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Initiate registration, persist disabled account, email OTP. |
| POST | `/api/auth/verify-otp` | Validate OTP, enable account, respond with JWT. |
| POST | `/api/auth/login` | Authenticate with email/password, issue JWT. |
| POST | `/api/auth/resend-otp` | Regenerate OTP for pending account. |
| GET | `/api/auth/test` | Lightweight service health check. |

### 3.2 OTP flow
1. `AuthService.register` validates duplicates, hashes password, assigns default `CUSTOMER` role when missing, and stores a randomly generated numeric OTP + expiry. The OTP length and validity window are property-driven (`otp.length`, `otp.expiration-minutes`).
2. `EmailService.sendOtpEmail` runs asynchronously (via `@Async`) using `JavaMailSender`. It builds an HTML email template including OTP and expiry instructions.
3. `AuthService.verifyOtp` re-fetches the user, ensures the OTP matches and is still valid (`User.isOtpValid()`), flips `enabled` + `verified` flags, clears OTP fields, and returns an `AuthResponse` containing a JWT.
4. `AuthService.resendOtp` re-issues OTP for still-disabled users and sends another email.

### 3.3 JWT issuance & validation
* `JwtUtil.generateToken(email)` builds HS256 tokens with configurable signing secret + TTL.
* `JwtAuthenticationFilter` inspects the `Authorization: Bearer <token>` header, validates expiry & signature, and populates the `SecurityContext`. Disabled accounts are prevented from authenticating.
* `SecurityConfig` registers the JWT filter in the chain, enforces stateless sessions, and leaves `/api/auth/**`, swagger resources, actuator health, and websocket handshake endpoints open.

### 3.4 Role-based access control
* Method-level `@PreAuthorize` governs each endpoint. Example: `TripController.createTrip` is annotated with `@PreAuthorize("hasRole('CUSTOMER')")`.
* Users are granted `ROLE_<enum>` authorities via `User.getAuthorities()` and `UserDetails` implementation.

---

## 4. User Management & Roles

### 4.1 Entities
* **User** (`users` table) – core identity, implements `UserDetails`. Includes OTP fields, verification flags, aggregated rating, and audit timestamps.
* **RiderProfile** (`rider_profiles`) – one-to-one with `User`, stores regulatory metadata (license, national ID), vehicle type, operational status, earnings, rating, and last known location.

### 4.2 Role taxonomy
* `ADMIN` – platform operators; manage users, trips, reports, and ratings insight via `AdminController`.
* `RIDER` – logistics partners; access rider-specific trip endpoints and rating submissions.
* `CUSTOMER` – clients placing delivery orders.

### 4.3 Account administration
* Admins call `PATCH /api/admin/users/{id}/status?enabled=false` to disable a user (`AdminService.setUserEnabled`). Disabling toggles the `User.enabled` flag, blocking JWT authentication.
* Rider approvals occur within `RiderService` (e.g., `approveRider`, `rejectRider`) and can be exposed through admin UX.

### 4.4 Security configuration
* `CorsConfig` allows configuring allowed origins/methods/headers via properties while exposing `Authorization` headers for SPA consumption.
* `SecurityConfig` enables method security, sets `SessionCreationPolicy.STATELESS`, and installs the JWT authentication filter.
* WebSocket handshake security leverages `JwtHandshakeInterceptor` (if enabled) to validate JWT tokens on `/ws/tracking` sessions.

---

## 5. Trip Management

### 5.1 Domain relationships
* `Trip` references a `customer` (`User` with CUSTOMER role) and an optional `rider` (`User` with RIDER role) through many-to-one associations.
* `RiderProfile` updates (earnings, trip counts, location) are triggered by trip progression.

### 5.2 Lifecycle
```
REQUESTED -> ACCEPTED -> PICKED_UP -> IN_TRANSIT -> DELIVERED
        \                                    /
         +----------> CANCELLED <------------+
```
* **REQUESTED** – created by customer. Available for rider acceptance.
* **ACCEPTED** – assigned rider, timestamped (`acceptedAt`).
* **PICKED_UP**, **IN_TRANSIT** – sequential updates by rider.
* **DELIVERED** – completion; sets `completedAt`, increments rider earnings/trips.
* **CANCELLED** – only the owning customer can cancel while `REQUESTED` or `ACCEPTED`.

`TripService` enforces valid transitions (`NEXT_STATUS` map). Unauthorized transitions raise `BadRequestException`.

### 5.3 Endpoints
| Method | Path | Role | Behaviour |
|--------|------|------|-----------|
| POST | `/api/trips` | CUSTOMER | Create new trip request using `TripRequestDto` (pickup/dropoff, coordinates, distance, fare). |
| GET | `/api/trips/customer` | CUSTOMER | List customer’s trips. |
| GET | `/api/trips/rider` | RIDER | List trips assigned to the rider. |
| GET | `/api/trips/available` | RIDER | List `REQUESTED` trips. |
| GET | `/api/trips/{id}` | Customer/Rider/Admin | Fetch trip details if participant or admin. |
| PATCH | `/api/trips/{id}/accept` | RIDER | Assign self and mark as `ACCEPTED`. |
| PATCH | `/api/trips/{id}/status` | Customer/Rider/Admin | Update status via `TripStatusUpdateDto` (with optional `riderLocation`). |

### 5.4 Sample payloads
**Create trip**
```http
POST /api/trips
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "pickupLocation": "Kileleshwa, Nairobi",
  "dropoffLocation": "CBD, Nairobi",
  "distanceKm": 7.25,
  "fare": 540.00,
  "pickupLatitude": -1.2845,
  "pickupLongitude": 36.7892,
  "dropoffLatitude": -1.2833,
  "dropoffLongitude": 36.8219
}
```

**Update status**
```http
PATCH /api/trips/{tripId}/status
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "status": "IN_TRANSIT",
  "riderLocation": "Ring Road Kilimani"
}
```

### 5.5 Fare calculation
While clients usually supply a fare, `FareCalculator` offers a deterministic helper: `base fare (1.50) + distance * 0.75`, with minimum fare 2.00 and rounding to 2 decimals. Rider earnings default to 80% of fare (`calculateRiderEarnings`). The WhatsApp integration uses its own constants when estimating quotes in chat.

### 5.6 Real-time tracking
`DeliveryTrackingPublisher.publishStatusUpdate` emits a `TripStatusNotification` to `/topic/trips/{tripId}` (for WebSocket subscribers) and caches status in Redis (`trip-status` hash, TTL 24h). This enables SPA polling fallback and rider app reconnects.

---

## 6. Reporting Module

### 6.1 Use case
Customers can flag missing or damaged goods after a trip is delivered.

### 6.2 Flow
1. Customer submits `POST /api/report` with `tripId`, `reason`, `description`.
2. `ReportService.createReport` validates the caller is the trip’s customer and persists a `Report` with status `PENDING`.
3. Admin dashboard pulls `GET /api/admin/reports` via `AdminService.getReports` and can transition reports:
   * `PATCH /api/admin/reports/{id}/review` → `UNDER_REVIEW`.
   * `PATCH /api/admin/reports/{id}/resolve` → `RESOLVED`.
4. Optional: When a report is created or resolved, integrate with `WhatsAppClient` to notify admin hotline or customer.

`Report` links to both `User` (reporter) and `Trip`, enabling analytics on repeat incidents.

---

## 7. Ratings Module

### 7.1 Domain
* `Rating` has unique constraint `(trip_id, reviewer_id, target_user_id)` – one rating per participant pair per trip.
* Aggregates update both `User.rating` and `RiderProfile.rating` via `RatingService.updateAverages`.

### 7.2 Endpoints
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/ratings` | CUSTOMER/RIDER | Submit rating for delivered trip (`RatingRequestDto`). |
| GET | `/api/ratings/me` | Any authenticated | List ratings received by the current user. |
| GET | `/api/admin/ratings/{userId}` | ADMIN | Admin view of ratings for a user. |

### 7.3 Validation rules
* Trip must be `DELIVERED`.
* Reviewer must be trip participant (customer or assigned rider).
* Reviewer can only rate the counterpart (customer ↔ rider).
* Duplicate ratings rejected.

### 7.4 Sample request
```http
POST /api/ratings
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "tripId": "ab1e6bc5-5e22-4f41-89fe-4095989e1d4f",
  "targetUserId": "90a431bc-238c-4f72-a5d6-6d064a2c6df3",
  "ratingValue": 5,
  "comment": "Prompt delivery, very professional!"
}
```

Response includes rating metadata plus timestamps.

---

## 8. WhatsApp Integration Layer

### 8.1 Components
* **`WhatsAppController`** – exposes:
  * `GET /whatsapp/verify-webhook` – handshake verifying Facebook/Twilio webhook challenge using `whatsapp.verify-token`.
  * `POST /whatsapp/webhook` – receives inbound messages, delegates to `WhatsAppService`.
* **`WhatsAppService`** – state machine handling commands, bridging to `AuthService`, `TripService`, `ReportService`, `RiderService`, and `RatingService`.
* **`MessageParser`** – regex-based intent extraction (`REGISTER`, `RIDE_REQUEST`, `ACCEPT_TRIP`, etc.).
* **`ConversationState`** – per-phone finite state machine capturing pending form fields, OTP tokens, trip IDs, etc. Stored in-memory using `ConcurrentHashMap` keyed by normalized phone numbers.
* **`WhatsAppClient`** – reactive `WebClient` wrapper for outbound messages to the WhatsApp Business API. Supports masked logging when credentials absent.

### 8.2 Supported message flows
1. **Registration**
   * User sends `register` → service captures email, name, role (customer/rider).
   * Generates temporary password/token, calls `AuthService.register` (with phone mapped to account), and instructs user to check email for OTP.
   * User replies `otp 123456` → `AuthService.verifyOtp` executed and JWT emailed/linked via ephemeral `TemporaryCredential`.

2. **Ride request**
   * Customer: `ride from <pickup> to <dropoff>`.
   * Service estimates distance and fare (fallback to default rate), stores pending trip details, then asks for confirmation.
   * `confirm` triggers `TripService.createTripForUser`; response includes trip ID shared back to user.
   * `cancel` aborts.

3. **Trip updates**
   * Rider receives `accept <tripId>` (or uses interactive buttons) to claim a trip (`TripService.acceptTripForUser`).
   * Subsequent messages `picked up <tripId>`, `in transit <tripId>`, `delivered <tripId>` map to `TripStatusUpdateDto` updates.
   * Service pushes notifications to both customer and rider via WhatsApp + WebSocket broadcast.

4. **Tracking**
   * Customer sends `track <tripId>` – service fetches `TripService.getTripDetailsForUser`, summarises status, and optionally includes ETA.

5. **Reporting**
   * `report issue <tripId> <message>` – stores `ReportRequestDto`, acknowledges receipt, notifies admins via `adminNotifyNumber` if configured.

6. **Ratings**
   * After `DELIVERED`, service prompts for rating; user can reply `rate <tripId> 5 Great service`. Pending comment stage stored in `ConversationState` when multi-step.

7. **Rider earnings summary**
   * Rider sends `earnings` to receive aggregated totals from `RiderService`/profile.

### 8.3 Webhook security
* Verify endpoint compares `hub.verify_token` against `whatsapp.verify-token` property.
* Inbound messages are processed idempotently; unknown senders default to registration prompt.
* Outbound API credentials (`whatsapp.access-token`, `whatsapp.phone-number-id`) must be configured; otherwise, messages are logged but not sent.

### 8.4 Payload example
Incoming message webhook (simplified):
```json
{
  "entry": [
    {
      "changes": [
        {
          "value": {
            "messages": [
              {
                "from": "254712345678",
                "type": "text",
                "text": { "body": "ride from Lavington to Westlands" }
              }
            ]
          }
        }
      ]
    }
  ]
}
```
`WhatsAppWebhookRequest.Message.body()` abstracts over text vs interactive replies.

---

## 9. Database Design

All primary keys are UUID (`@GeneratedValue(strategy = GenerationType.UUID)`). Core tables:

| Table | Key Fields | Relationships |
|-------|------------|---------------|
| `users` | `id`, `full_name`, `email`, `password`, `role`, `enabled`, `verified`, `rating`, OTP columns | One-to-one `rider_profiles`; one-to-many `trips` (as customer), `trips` (as rider), `reports`, `ratings` (reviewer/target). |
| `rider_profiles` | `id`, `user_id`, `license_number`, `vehicle_type`, `status`, `total_earnings`, `rating`, `last_location` | One-to-one with `users`. |
| `trips` | `id`, `customer_id`, `rider_id`, `pickup_location`, `fare`, `status`, coordinates, timestamps | Many-to-one with `users` (customer/rider); referenced by `reports`, `ratings`. |
| `reports` | `id`, `reporter_id`, `trip_id`, `reason`, `description`, `status`, `created_at` | Many-to-one `users` and `trips`. |
| `ratings` | `id`, `trip_id`, `reviewer_id`, `target_user_id`, `rating_value`, `comment`, `created_at` | Many-to-one `trips` and `users`; unique constraint on `(trip_id, reviewer_id, target_user_id)`. |
| `payments` | (future) store transaction references, method, status, amount. |

ER summary:
```
User 1---1 RiderProfile
User 1---* Trip (as customer)
User 1---* Trip (as rider)
Trip 1---* Report
Trip 1---* Rating
User 1---* Rating (as reviewer)
User 1---* Rating (as target)
```

---

## 10. Configuration

### 10.1 `application.yml` template
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://db.example.com:5432/bodaboda
    username: boda_app
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  data:
    redis:
      host: redis.example.com
      port: 6379
      password: ${REDIS_PASSWORD:}

jwt:
  secret: ${JWT_SECRET_BASE64}
  expiration: 3600000 # 60 minutes

otp:
  length: 6
  expiration-minutes: 5

app:
  mail:
    from: support@bodaboda.africa
    from-name: "Boda Boda Delivery"
  whatsapp:
    admin-notify-number: 254711000000

mail:
  host: smtp.sendgrid.net
  port: 587
  username: apikey
  password: ${SENDGRID_API_KEY}
  properties:
    mail:
      smtp:
        auth: true
        starttls:
          enable: true

whatsapp:
  api-base-url: https://graph.facebook.com/v17.0
  access-token: ${WHATSAPP_ACCESS_TOKEN}
  phone-number-id: ${WHATSAPP_PHONE_ID}
  verify-token: ${WHATSAPP_VERIFY_TOKEN}
```

Additional profiles (e.g., `application-prod.yml`) should refine datasource pools, logging, and mail provider settings.

---

## 11. API Reference

| Endpoint | Method | Role(s) | Description | Request Body | Response |
|----------|--------|---------|-------------|--------------|----------|
| `/api/auth/register` | POST | Public | Create user in pending state, send OTP | `RegisterRequest` | `MessageResponse` (201) |
| `/api/auth/verify-otp` | POST | Public | Verify OTP and issue JWT | `VerifyOtpRequest` | `AuthResponse` |
| `/api/auth/login` | POST | Public | Authenticate by email/password | `LoginRequest` | `AuthResponse` |
| `/api/auth/resend-otp` | POST | Public | Re-send OTP | `email` query param | `MessageResponse` |
| `/api/users/me` | GET | Any authenticated | Fetch current profile | – | `UserDto` |
| `/api/users/me` | PUT | Any authenticated | Update profile info | `UpdateProfileRequest` | `UserDto` |
| `/api/trips` | POST | CUSTOMER | Create trip | `TripRequestDto` | `TripResponseDto` (201) |
| `/api/trips/customer` | GET | CUSTOMER | List customer trips | – | `[TripResponseDto]` |
| `/api/trips/rider` | GET | RIDER | List rider trips | – | `[TripResponseDto]` |
| `/api/trips/available` | GET | RIDER | Browse available trips | – | `[TripResponseDto]` |
| `/api/trips/{id}` | GET | CUSTOMER/RIDER/ADMIN | Trip details | – | `TripResponseDto` |
| `/api/trips/{id}/accept` | PATCH | RIDER | Claim trip | – | `TripResponseDto` |
| `/api/trips/{id}/status` | PATCH | CUSTOMER/RIDER/ADMIN | Advance/cancel trip | `TripStatusUpdateDto` | `TripResponseDto` |
| `/api/report` | POST | CUSTOMER | File incident report | `ReportRequestDto` | `ReportResponseDto` |
| `/api/admin/reports` | GET | ADMIN | List all reports | – | `[ReportResponseDto]` |
| `/api/admin/reports/{id}/review` | PATCH | ADMIN | Mark report under review | – | `ReportResponseDto` |
| `/api/admin/reports/{id}/resolve` | PATCH | ADMIN | Resolve report | – | `ReportResponseDto` |
| `/api/ratings` | POST | CUSTOMER/RIDER | Submit rating | `RatingRequestDto` | `RatingResponseDto` |
| `/api/ratings/me` | GET | Any authenticated | Ratings received by current user | – | `[RatingResponseDto]` |
| `/api/admin/ratings/{userId}` | GET | ADMIN | Ratings for specific user | – | `[RatingResponseDto]` |
| `/api/admin/users` | GET | ADMIN | List all users | – | `[UserDto]` |
| `/api/admin/users/{id}/status` | PATCH | ADMIN | Enable/disable account | `enabled` query param | `UserDto` |
| `/api/admin/trips` | GET | ADMIN | List all trips | – | `[TripResponseDto]` |
| `/api/admin/riders` | GET | ADMIN | List rider profiles | – | `[RiderDto]` |
| `/whatsapp/verify-webhook` | GET | WhatsApp platform | Webhook verification challenge | Query params | Challenge string (200/403) |
| `/whatsapp/webhook` | POST | WhatsApp platform | Inbound message hook | WhatsApp JSON payload | `200 OK` |

---

## 12. Example Workflows

### 12.1 Register & verify via WhatsApp
1. Customer sends `register` in WhatsApp chat.
2. Service asks for email, name, and role sequentially, persisting intermediate state in `ConversationState`.
3. Once registration details collected, a temporary password is generated, `AuthService.register` called, OTP emailed.
4. Customer replies `otp 845912`; service validates via `AuthService.verifyOtp` and shares confirmation + login instructions.

### 12.2 Rider accepts trip via WhatsApp
1. Rider sees `Trip #ABC` broadcast (via admin assignment or WhatsApp push).
2. Rider replies `accept abc123...`; `TripService.acceptTripForUser` assigns them.
3. Rider subsequently messages `picked up abc123`, `in transit abc123`, `delivered abc123`; each triggers `TripStatusUpdateDto` and WebSocket push. Rider earnings and trip count updated when `DELIVERED`.

### 12.3 Customer tracks order via message
1. Customer sends `track abc123`.
2. Service fetches trip details, returns status, last rider location (if available), and estimated completion.
3. SPA dashboard simultaneously receives `/topic/trips/abc123` notification for synchronised status.

### 12.4 Admin reviews report
1. Customer files report using mobile app or WhatsApp.
2. Admin React dashboard pulls `GET /api/admin/reports`, filters by `PENDING`.
3. Admin marks `UNDER_REVIEW` once investigation starts, optionally triggers WhatsApp acknowledgement to customer.
4. After resolution, admin hits `/api/admin/reports/{id}/resolve`; status updated and rider/customer notified.

---

## 13. Frontend Integration Notes

### 13.1 React Admin Dashboard
* Authenticate via `/api/auth/login`, store JWT in HTTP-only cookie or secure storage; attach `Authorization: Bearer` header.
* Use `/api/admin/users`, `/api/admin/trips`, `/api/admin/reports`, and `/api/admin/ratings/{userId}` for management views.
* Subscribe to `/ws/tracking` using STOMP over SockJS; apply JWT during handshake. Listen on `/topic/trips/{tripId}` to display live status.
* For report actions, call PATCH endpoints and optimistically update UI.

### 13.2 Flutter Rider App
* Use same JWT login flow as web.
* Poll `/api/trips/available` or subscribe via WebSocket to get new assignments (service broadcasts status changes).
* Update trip status using `/api/trips/{id}/status`; include `riderLocation` string to surface last known location.
* Submit post-delivery ratings through `/api/ratings` targeting the customer.

### 13.3 Mobile customer app / WhatsApp hybrid
* Primary interaction through WhatsApp; optionally complement with native Flutter app that consumes `/api/trips/customer` and listens to `/topic/trips/{tripId}`.
* Use cached Redis status (exposed via future REST endpoint) for quick lookups.

### 13.4 Real-time considerations
* Reconnect WebSocket clients using exponential backoff.
* Fallback to REST polling (`/api/trips/{id}`) every 30 seconds when WebSocket unavailable.

---

## 14. Future Enhancements

* **Payments** – integrate M-PESA, Airtel Money, Tigo using asynchronous callbacks; model payments in `Payment` entity with state machine.
* **Caching** – leverage Redis for rider availability, surge pricing, and WhatsApp conversation state persistence across nodes.
* **Analytics & AI** – apply ML models to detect fraud (e.g., repeated cancellations, abnormal routes) and prioritise incoming reports based on risk.
* **Geo services** – integrate Google Maps or OpenStreetMap for precise distance + ETA calculations, feeding into `FareCalculator`.
* **Observability** – add distributed tracing (OpenTelemetry) and structured logging with `SensitiveDataMasker`.

---

**Next steps**: combine this document with the generated OpenAPI specification to publish on an internal developer portal or import into Postman for interactive exploration.
