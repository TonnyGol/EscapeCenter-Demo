# 🚀 EscapeCenter Web Client

## Overview
React + Vite booking web client for EscapeCenter. Allows customers to view available time slots and create bookings for escape rooms.

---

## 📋 Prerequisites
- **Node.js** (v16 or higher)
- **Backend API** running at `http://localhost:8080` (configurable via `.env`)

---

## ⚙️ Configuration

Copy `.env.example` to `.env` and edit the API URL if needed:
```env
VITE_API_URL=http://localhost:8080
```

---

## 🏃 Running

### Install Dependencies (first time only)
```bash
npm install
```

### Start Development Server
```bash
npm run dev
```
The app will be available at **http://localhost:5173**

### Build for Production (optional)
```bash
npm run build
```

---

## 🎯 Features

- ✅ **Weekly Calendar View** with 7-day layout
- ✅ **5 Escape Rooms** with time slot availability
- ✅ **Customer Booking Form** with details and confirmation
- ✅ **RTL Support** — Hebrew language interface
- ✅ **Responsive Design** — mobile and desktop

---

## 🚢 Docker Deployment

```bash
docker build -t escapecenter-web .
docker run -p 80:80 escapecenter-web
```

---

## 🔧 Troubleshooting

| Issue | Fix |
|-------|-----|
| "לא ניתן להתחבר לשרת" | Start the backend server at `http://localhost:8080` |
| Port 5173 in use | Vite auto-selects the next available port |
| Module not found | Run `npm install` |

---

## 📄 License

Private project for EscapeCenter.
