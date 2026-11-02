# 🖥️ EscapeCenter Server Side

## Overview
The Server Side contains two applications:

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **EscapeCenter_Server** | Spring Boot (Java 21) | REST API backend with database & security |
| **EscapeCenter_DemoApp** | JavaFX (Java 21) | Desktop admin management application |

---

## 📋 Prerequisites
- **Java 21** (JDK)
- **Maven** (or use the included `mvnw` wrapper)
- **Docker** (for the MySQL container, see the root `README.md`)

---

## 🏃 Running

### 1. Start the Server (required first)
```bash
cd Server_Side/EscapeCenter_Server
mvn spring-boot:run
```
Runs on **http://localhost:8080**

### 2. Start the Desktop App
```bash
cd Server_Side/EscapeCenter_DemoApp
mvn javafx:run
```

---

## ⚙️ Configuration

- **Server**: environment variables `DB_URL` (optional), `DB_USER`, `DB_PASS`, `APP_ADMIN_USER`, `APP_ADMIN_PASSWORD`. See `.env.example` in the repo root.
- **DemoApp**: optional `src/main/resources/config.properties` with `server.url` (defaults to `http://localhost:8080`).

---

## 🚢 Docker (Server)

```bash
cd Server_Side/EscapeCenter_Server
docker build -t escapecenter-server .
docker run -p 8080:8080 \
  -e DB_USER=... -e DB_PASS=... -e APP_ADMIN_USER=... -e APP_ADMIN_PASSWORD=... \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/escapecenter \
  escapecenter-server
```
