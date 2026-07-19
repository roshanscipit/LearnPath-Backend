# Doliuw App – Spring Boot Backend

**Stack:** Java 17 · Spring Boot 3.2 · MySQL 8 · Caffeine Cache · JWT

---

## Quick Start

### 1. Prerequisites
- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`)
- MySQL 8.0 running locally

### 2. Create the database
```sql
mysql -u root -p
CREATE DATABASE doliuw_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit
```

### 3. Configure credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/doliuw_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 4. Run
```bash
cd doliuw-backend
mvn spring-boot:run
```

Server starts at **http://localhost:8080**

---

## API Reference

### Auth (Public)

| Method | URL | Body | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/signup/email` | `{name, email, password}` | Register with email |
| POST | `/api/auth/login/email` | `{email, password}` | Login with email |
| POST | `/api/auth/send-otp` | `{mobile, name?}` | Send OTP to mobile |
| POST | `/api/auth/verify-otp` | `{mobile, otp, name?}` | Verify OTP & get JWT |
| POST | `/api/auth/google/callback` | `{sessionId}` | Exchange Google session |
| GET  | `/api/auth/me` | – *(JWT required)* | Get current user |
| POST | `/api/auth/logout` | – *(JWT required)* | Logout |

### Companies & Roles (Public)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/companies?category=all` | All companies (filter: product/service/startup) |
| GET | `/api/companies/{id}` | Single company detail |
| GET | `/api/roles` | All roles |
| GET | `/api/roles/{id}` | Single role |
| GET | `/api/mock-tests/list?type=free` | Mock tests (filter: free/paid) |

### Progress (JWT Required)

| Method | URL | Body | Description |
|--------|-----|------|-------------|
| GET | `/api/progress` | – | Get user's learning progress |
| PUT | `/api/progress` | `{selectedRole, selectedVariant, completedModules[], currentModule, overallProgress, testsTaken, averageScore}` | Update progress |

### Bookings (JWT Required)

| Method | URL | Body | Description |
|--------|-----|------|-------------|
| GET | `/api/bookings` | – | List my bookings |
| POST | `/api/bookings` | `{serviceId, serviceName, price, bookingDate, timeSlot}` | Create booking |
| DELETE | `/api/bookings/{id}` | – | Cancel booking |

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```

The JWT token is returned in the `token` field of every auth response.

---

## Caching (Caffeine)

| Cache Name | TTL | Max Size | Eviction |
|------------|-----|----------|---------|
| `companies` | 30 min | 500 | On write |
| `roles` | 60 min | 200 | On write |
| `mockTests` | 60 min | 200 | On write |
| `users` | 10 min | 1000 | On logout / profile update |
| `userProgress` | 5 min | 1000 | On progress update |
| `otpStore` | 10 min | 500 | On OTP use / expiry |
| `bookings` | 15 min | 500 | On create / cancel |

---

## OTP – Demo vs Production

**Demo mode** (`app.otp.demo-mode=true`):  
OTP is always `123456`. No SMS is sent.

**Production mode** (`app.otp.demo-mode=false`):  
Integrate [Twilio](https://www.twilio.com/) or MSG91 in `AuthService.sendOtp()`:
```java
// Uncomment and configure in AuthService.sendOtp():
twilioService.sendSms("+91" + mobile, "Your Doliuw OTP: " + otp);
```

---

## Project Structure

```
doliuw-backend/
├── src/main/java/com/doliuw/
│   ├── DoliuwApplication.java       # Entry point
│   ├── config/
│   │   ├── CacheConfig.java         # Caffeine cache setup
│   │   └── SecurityConfig.java      # JWT + CORS security
│   ├── controller/
│   │   ├── AuthController.java      # /api/auth/**
│   │   ├── CompanyController.java   # /api/companies, /api/roles, /api/mock-tests
│   │   ├── UserProgressController.java
│   │   └── BookingController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CompanyService.java      # All static data + Caffeine caching
│   │   ├── UserProgressService.java
│   │   ├── BookingService.java
│   │   └── OtpCleanupService.java   # Scheduled OTP purge
│   ├── security/
│   │   ├── JwtTokenProvider.java    # JWT sign/verify
│   │   └── JwtAuthenticationFilter.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── UserProgress.java
│   │   ├── Booking.java
│   │   └── OtpStore.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── UserProgressRepository.java
│   │   ├── OtpRepository.java
│   │   └── BookingRepository.java
│   ├── dto/
│   │   ├── AuthDtos.java
│   │   └── AppDtos.java
│   └── exception/
│       ├── AppException.java
│       └── GlobalExceptionHandler.java
└── src/main/resources/
    ├── application.properties
    └── db-init.sql
```

---

## Frontend Integration

In your React app (`learnpath/.env` or `.env.local`):
```
REACT_APP_BACKEND_URL=http://localhost:8080
```

The frontend already calls all these endpoints — once the backend is running, auth, progress, and bookings will work with real data instead of demo mode.

---

## Connecting the JWT in the Frontend

After login, store the token and send it with every request:
```javascript
// After login response:
localStorage.setItem('token', data.token);

// In each fetch call:
const response = await fetch(`${BACKEND_URL}/api/progress`, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`,
    'Content-Type': 'application/json'
  }
});
```
