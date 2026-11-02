# EscapeCenter

Booking and management system for an escape-room venue: Spring Boot API, React booking site, JavaFX staff app.

<!-- TODO: demo GIF (React booking flow + JavaFX admin calendar), recorded with ScreenToGif -->

## Architecture

```mermaid
flowchart LR
  Web[React + Vite SPA] -->|REST/JSON| API[Spring Boot API]
  Admin[JavaFX staff app] -->|REST/JSON| API
  API --> DB[(MySQL)]
```

## Features

- Customers pick a room and a time slot on a weekly calendar and book it (Hebrew, RTL interface).
- Staff manage bookings and clients from a desktop app.
- REST API over MySQL, with HTTP Basic login for staff endpoints.

## Tech stack

| Part | Technology |
|------|-----------|
| API | Java 21, Spring Boot 3.4, JDBC |
| Database | MySQL 8.4 (Docker Compose) |
| Booking site | React 19, Vite |
| Staff app | JavaFX 21 |
| Security | Spring Security (HTTP Basic) |

## Quick start

Needs Docker, Java 21, Maven and Node.js.

```bash
cp .env.example .env          # then fill in the passwords
docker compose up -d          # MySQL, schema is created on first start
```

Export `DB_USER`, `DB_PASS`, `APP_ADMIN_USER` and `APP_ADMIN_PASSWORD` in your shell (values are explained in `.env.example`), then:

```bash
cd Server_Side/EscapeCenter_Server && mvn spring-boot:run    # API on :8080
cd Client_Side/EscapeCenter_Web && npm install && npm run dev # site on http://localhost:5173
cd Server_Side/EscapeCenter_DemoApp && mvn javafx:run         # staff app (optional)
```

Until Phase 5 (Testcontainers), `mvn test` needs the Docker MySQL running and the four variables above set.

One-command start (`docker compose up` for the whole stack) arrives with the CI/CD phase.

## API

| Method | Path | Access | Purpose |
|--------|------|--------|---------|
| GET | `/bookings` | public | List all bookings |
| POST | `/addBooking` | public | Create a booking |
| PUT | `/server/updateBooking` | staff | Update a booking |
| POST | `/server/deleteBooking` | staff | Delete a booking |
| GET | `/server/clients` | staff | List all clients |
| GET | `/server/test` | staff | Check the staff login |

Swagger UI is planned (see roadmap).

## Roadmap

Next: input validation, a cleaner data layer and tests, then CI/CD, an event-driven email service and a live demo.
