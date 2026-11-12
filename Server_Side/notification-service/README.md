# Notification service

A small Spring Boot app with no web server. It reads booking events from RabbitMQ and sends the confirmation or cancellation email. The API never waits for it: a booking is saved and answered first, and the email follows.

## How they connect

```
Customer books  ->  API saves booking + outbox row (one transaction)
                    -> publisher sends it to RabbitMQ (exchange bookings)
                       -> queue bookings.notifications
                          -> notification service reads it
                             -> sends the email via SMTP to Mailpit
```

The API does not know this service or Mailpit exist, and this service does not know the API. Only the event format is shared (`BookingEvent` here, `BookingCreatedEvent` in the API's `BookingService`).

## What it does with each message

1. Reads the JSON event and skips it when the booking has no client email.
2. Picks the email by routing key: `booking.created` or `booking.cancelled` (`EmailTemplate`, texts in `src/main/resources/email/`).
3. Saves the event id in `processed_events`. If it is already there, the message is a duplicate and nothing is sent. The save and the email share one transaction, so a failed email can be retried.
4. Sends the email. When this fails, the message is tried 3 times (1 s, then 2 s apart), then it goes to the dead-letter queue `bookings.dlq` with the error in its headers.

## Run and test

```bash
docker compose up -d --build notification-service   # from the project root; starts what it needs
./mvnw verify                                        # needs Docker: Testcontainers starts RabbitMQ, MySQL and Mailpit
```

`./mvnw verify` needs `MYSQL_USER` and `MYSQL_PASSWORD` set to any value; the tests use their own containers.

| Setting | Meaning | Default |
|---------|---------|---------|
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | the broker | `localhost`, `guest`, `guest` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM` | the SMTP server and sender | `localhost`, `1025`, `noreply@escapecenter.local` |
| `DB_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` | MySQL, only for the `processed_events` table | `localhost:3306/escapecenter` |

## Mailpit (dev mail server)

Mailpit is a fake mail server: it accepts every email like a real one but delivers nothing, so you can read what would have been sent. Open http://localhost:8025 (no login). Each booking you create shows up there as an email within a few seconds. For production, point `MAIL_HOST` and `MAIL_PORT` at a real mail provider; the code does not change.

## RabbitMQ (the message broker)

How to use it: open http://localhost:15672 and log in with `RABBITMQ_USER` and `RABBITMQ_PASSWORD` from your `.env` file. Useful places:
- **Queues tab:** click `bookings.notifications`. "Ready" is the number of messages waiting. It should be 0 when things are healthy.
- **Click `bookings.dlq`, then "Get messages":** you can read failed messages, including the error in the headers. To retry one, publish it back to the `bookings` exchange. That's what the main README describes.
- **Exchanges tab:** `bookings` and its binding to the queue.

The three pieces: the exchange `bookings` sorts messages by their label (`booking.created`), the queue `bookings.notifications` holds them until this service takes them (even if it is switched off), and `bookings.dlq` keeps the ones that failed 3 times.
