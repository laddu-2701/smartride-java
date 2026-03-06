# Smart Ride Sharing System - Milestone 1

This is a Spring Boot application implementing the first milestone of a ride-hailing platform.  
It includes:

- User management (registration, login, role-based access: driver/passenger)
- JWT authentication with Spring Security
- Driver ride posting and passenger search/booking
- Booking confirmation with seat update
- Basic prototype frontend with minimal CSS

## Getting Started

### Prerequisites

- Java 17 or higher installed
- Maven installed (needed to build and run the project)

> **Note:** The Maven executable (`mvn`) must be on your PATH. If it's not installed, download it from https://maven.apache.org/.

### Build and Run

```powershell
cd d:\Projects\carpooling
mvn clean package
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.
H2 console is available at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:carpoolingdb`).

You can also use the provided VS Code tasks (`mvn package` and `mvn spring-boot:run`) from the **Terminal → Run Task…** menu.

### API Endpoints

- `POST /auth/register` – register a new user (driver or passenger)
- `POST /auth/login` – obtain JWT token
- `GET /auth/me` – view your profile (requires Bearer token)
- `POST /rides/post` – drivers post rides (authenticated)
- `GET /rides/search` – search rides by source/destination/date
- `POST /bookings/book` – book seats on a ride (authenticated)

Use the static pages at `/` as a minimal prototype interface.

## Notes

- Passwords are encoded with BCrypt.
- JWT secret and expiration can be configured in `application.properties`.
- Data models use explicit getters/setters (Lombok removed) to guarantee compilation without additional plugins.
- Ride data is stored in memory using H2; switching to a persistent database only requires updating configuration and adding a dependency.

---

This repository contains the baseline functionality required for Milestone 1. Future milestones will add fare calculation, filtering, ratings, user reviews, and payment integration.