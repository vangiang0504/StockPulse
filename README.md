# Training Starter

Full-stack starter codebase for fresher training program. Includes JWT authentication, configuration, exception handling, and a sample User CRUD as a reference pattern.

## Tech Stack

**Backend**: Java 17, Spring Boot 3.2, Spring Security + JWT, Spring Data JPA, PostgreSQL 16, Redis 7, RabbitMQ 3, Flyway, MapStruct, Lombok, SpringDoc OpenAPI

**Frontend**: Angular 17, Angular Material, RxJS, TypeScript

**Infrastructure**: Docker Compose (PostgreSQL, Redis, RabbitMQ, MailHog)

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+ and npm
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
cd backend
docker compose up -d
docker compose ps   # Verify 4 services running
```

### 2. Run Backend

```bash
cd backend
./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 3. Run Frontend

```bash
cd frontend
npm install
npm start
```

- Angular: http://localhost:4200

### 4. Run Tests

```bash
cd backend
./mvnw test
```

## Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Spring Boot API | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| Angular Dev Server | http://localhost:4200 | - |
| PostgreSQL | localhost:5432 | postgres / postgres |
| Redis | localhost:6379 | no auth |
| RabbitMQ Management | http://localhost:15672 | guest / guest |
| MailHog Web UI | http://localhost:8025 | no auth |

## What's Included

### Backend
- JWT authentication (login, register, refresh token)
- Security configuration (stateless, BCrypt, role-based)
- Global exception handler (400, 401, 403, 404, 409, 500)
- Redis configuration with Jackson serializer
- RabbitMQ configuration with base exchange/queue
- OpenAPI/Swagger with JWT bearer scheme
- CORS configuration for Angular dev server
- ApiResponse and PageResponse wrappers
- BaseEntity with auto-managed timestamps
- User entity with full CRUD (reference implementation)
- Flyway migration for users table
- Unit tests for UserService (7 tests)
- BaseIntegrationTest with Testcontainers

### Frontend
- Angular 17 standalone components
- JWT interceptor (auto-attach token)
- Auth guard (route protection)
- Auth service (login, register, logout)
- Notification service (snackbar)
- Main layout with sidebar navigation
- Auth layout for login/register
- Login and register pages
- User list page with pagination
- User form page (create/edit)
- Confirm dialog component
- Loading spinner component
- Pagination component

## How to Extend

Follow the User CRUD pattern to add domain entities:

1. **Entity** extending `BaseEntity`
2. **Flyway migration** (V2, V3, ...)
3. **Repository** extending `JpaRepository`
4. **DTOs** (request + response records)
5. **Mapper** (MapStruct)
6. **Service** (interface + impl)
7. **Controller** (REST endpoints)
8. **Unit tests**
9. **Angular components** (list + form + service)

## Project Structure

```
training-starter/
├── backend/
│   ├── pom.xml
│   ├── docker-compose.yml
│   └── src/
│       ├── main/java/com/training/starter/
│       │   ├── config/          # Redis, RabbitMQ, OpenAPI, CORS
│       │   ├── security/        # JWT auth flow
│       │   ├── controller/      # Auth + User CRUD
│       │   ├── dto/             # Request/Response DTOs
│       │   ├── entity/          # BaseEntity + User
│       │   ├── exception/       # Global handler + custom exceptions
│       │   ├── mapper/          # MapStruct
│       │   ├── repository/      # Spring Data JPA
│       │   ├── service/         # Business logic
│       │   └── common/          # ApiResponse + PageResponse
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       └── test/
├── frontend/
│   └── src/app/
│       ├── core/                # Interceptors, guards, services, models
│       ├── layout/              # Main + Auth layouts
│       ├── features/            # Auth, Users, Dashboard
│       └── shared/              # Reusable components
└── README.md
```
