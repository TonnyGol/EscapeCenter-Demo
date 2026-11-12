# EscapeCenter

[![CI](https://github.com/TonnyGol/EscapeCenter-Demo/actions/workflows/ci.yml/badge.svg)](https://github.com/TonnyGol/EscapeCenter-Demo/actions/workflows/ci.yml)

Booking and management system for an escape-room venue: Spring Boot API, React booking site, JavaFX staff app.

<!-- TODO: demo GIF (React booking flow + JavaFX admin calendar), recorded with ScreenToGif -->

## Architecture

```mermaid
flowchart LR
  Web[React + Vite SPA] -->|REST/JSON| API[Spring Boot API]
  Admin[JavaFX staff app] -->|REST/JSON| API
  API --> DB[(MySQL)]
  API -->|outbox publisher| MQ{{RabbitMQ}}
  MQ -->|booking.created| Notify[Notification service]
  Notify -->|SMTP| Mail[Mailpit in dev]
  Notify --> DB
```

### Booking emails

Creating a booking never waits for an email:

1. The API saves the booking **and** a `BookingCreated` row in `outbox_events` in one database transaction.
2. A scheduled job in the API sends unpublished rows to the RabbitMQ exchange `bookings` and marks them published.
3. The notification service reads the queue `bookings.notifications` and sends the email (Mailpit in dev, any SMTP server through `MAIL_HOST` / `MAIL_PORT` in production).

**Why an outbox?** Saving the booking and sending a message are two systems, and no single transaction covers both. Without the outbox, a crash between them gives a booking with no email, or an email for a booking that was rolled back. With it, the event exists if and only if the booking does.

**What happens when something is down**

| Down | Result |
|------|--------|
| RabbitMQ | Bookings still work. Events stay unpublished in `outbox_events` and go out when the broker is back. |
| Notification service | Messages wait in the durable queue and are delivered when it restarts. |
| Email server | The message is retried 3 times (1 s, then 2 s apart), then moved to the dead-letter queue `bookings.dlq` with the error in its headers. Look at it in the RabbitMQ UI (http://localhost:15672) and move it back to the exchange to try again. |

Delivery is at-least-once, so the same event can arrive twice. The notification service writes each event's id to `processed_events` in the same transaction as the email and skips ids it has seen. Published outbox rows are deleted after 7 days.

**Known limit:** the email address in a public booking is not verified, so someone could enter a stranger's address and that person would get the confirmation. The rate limit slows this down but does not stop it. A confirmation link in the email (roadmap phase 7b, customer self-service) is the real fix; add it before real customers use the site.

In dev, every email lands in Mailpit at http://localhost:8025 and nothing leaves your machine.

## Features

- Customers pick a room and a time slot on a weekly calendar and book it (Hebrew, RTL interface).
- Staff manage bookings and clients from a desktop app.
- REST API over MySQL, with HTTP Basic login for staff endpoints.
- Each room has its own first game time, number of games per day and break length (columns on the `rooms` table), so rooms can run different hours.

## Tech stack

| Part | Technology |
|------|-----------|
| API | Java 21, Spring Boot 3.4, JDBC |
| Database | MySQL 8.4 (Docker Compose) |
| Messaging | RabbitMQ, transactional outbox, notification service (Spring Boot) |
| Booking site | React 19, Vite |
| Staff app | JavaFX 21 |
| Security | Spring Security (HTTP Basic) |

## Quick start

Needs Docker, Java 21, Maven and Node.js.

```bash
cp .env.example .env          # then fill in the passwords
docker compose up -d          # MySQL; the API creates the schema (Flyway) when it starts
```

Export `MYSQL_USER`, `MYSQL_PASSWORD`, `APP_ADMIN_USER` and `APP_ADMIN_PASSWORD` in your shell (values are explained in `.env.example`), then:

```bash
cd Server_Side/EscapeCenter_Server && mvn spring-boot:run    # API on :8080
cd Client_Side/EscapeCenter_Web && npm install && npm run dev # site on http://localhost:5173
cd Server_Side/EscapeCenter_DemoApp && mvn javafx:run         # staff app (optional)
```

`mvn verify` starts its own throwaway MySQL with Testcontainers, so it only needs Docker running (no database or environment variables).

One-command start (`docker compose up` for the whole stack) arrives with the CI/CD phase.

## API

| Method | Path | Access | Purpose |
|--------|------|--------|---------|
| GET | `/api/rooms` | public | List rooms |
| GET | `/api/rooms/{id}/availability?date=YYYY-MM-DD` | public | Slots of a room on a day, free or taken |
| POST | `/api/bookings` | public | Create a booking (400 invalid, 409 slot taken) |
| GET | `/api/bookings?from&to&roomId` | staff | List bookings in a date range |
| PUT | `/api/bookings/{id}` | staff | Update client and booking details |
| DELETE | `/api/bookings/{id}` | staff | Delete a booking |
| GET | `/api/clients?query=` | staff | Search clients by name or phone |
| GET | `/api/staff/check` | staff | Check the staff login |

Errors come back as [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `ProblemDetail` JSON.

Swagger UI is planned (see roadmap).

## Roadmap

Next: input validation, a cleaner data layer and tests, then CI/CD, an event-driven email service and a live demo.
