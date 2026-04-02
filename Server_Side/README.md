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
- **MySQL** database

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

- **Server**: `EscapeCenter_Server/src/main/resources/application.properties`
- **DemoApp**: `EscapeCenter_DemoApp/src/main/resources/config.properties`

---

## 🚢 Docker (Server)

```bash
cd Server_Side/EscapeCenter_Server
docker build -t escapecenter-server .
docker run -p 8080:8080 escapecenter-server
```

---

## 📄 License

Private project for EscapeCenter.
