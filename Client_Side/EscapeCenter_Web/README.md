# 🚀 EscapeCenter Web Client - Quick Start Guide

## Overview
This is a React + Vite booking web client for EscapeCenter customers. It allows customers to view available time slots and create bookings for escape rooms.

---

## 📋 Prerequisites
- **Node.js** (v16 or higher recommended)
- **Backend API** running at `http://localhost:8080` (configurable via `.env`)

---

## ⚙️ Configuration

### Environment Variables
The backend API URL is configurable via environment variables:

1. Copy `.env.example` to `.env` (already done)
2. Edit `.env` to change the API URL if needed:
   ```env
   VITE_API_URL=http://localhost:8080
   ```

---

## 🏃 Running the Application

### Option 1: Using NPM Commands

#### 1. Install Dependencies (first time only)
```bash
npm install
```

#### 2. Start Development Server
```bash
npm run dev
```

The app will be available at **http://localhost:5173** (or another port if 5173 is busy)

#### 3. Build for Production (optional)
```bash
npm run build
```

#### 4. Preview Production Build (optional)
```bash
npm run preview
```

### Option 2: Using PowerShell Script
A convenient startup script is provided:

```powershell
.\start.bat
```

Or run in PowerShell:
```powershell
.\start-dev.ps1
```

---

## 🧪 Testing Without Backend (Serverless Demo)

We've created a standalone demo version that doesn't require a backend server:

### Location
```
serverless-web-test/
```

### To Run the Demo
1. Follow the instructions in `serverless-web-test/README.md`
2. This version uses mock data and runs completely client-side

---

## 🏗️ Project Structure

```
EscapeCenter-Web/
├── src/
│   ├── components/
│   │   └── BookingCalendar.jsx    # Main booking calendar component
│   ├── style/
│   │   └── BookingCalendar.css    # Calendar styling
│   ├── App.jsx                     # Main app component
│   ├── main.jsx                    # Entry point
│   └── index.css                   # Global styles
├── .env                            # Environment variables (API URL)
├── .env.example                    # Example environment file
├── package.json                    # Dependencies and scripts
├── vite.config.js                  # Vite configuration
└── index.html                      # HTML template
```

---

## 🎯 Features

- ✅ **Weekly Calendar View**: Shows 7 days starting from Sunday
- ✅ **5 Escape Rooms**: אחוזת השכן, מקדש הקאמי, ההתערבות, אינפיניטי, נרקוס
- ✅ **Time Slots**: 7 slots per day (90-min sessions with 30-min breaks)
- ✅ **Availability Colors**: 
  - 🟢 Green = Available
  - 🔴 Red = Booked
- ✅ **Customer Booking Form**: Create new bookings with customer details
- ✅ **RTL Support**: Hebrew language interface
- ✅ **Responsive Design**: Works on mobile and desktop

---

## 🔧 Troubleshooting

### Issue: "לא ניתן להתחבר לשרת" (Cannot connect to server)

**This is expected** if your backend is not running. To fix:

1. **Start your backend server** at `http://localhost:8080`
2. Verify the backend is running by visiting `http://localhost:8080/booking` in your browser
3. Refresh the web client

### Issue: Port 5173 is already in use

Vite will automatically use the next available port. Check the terminal output for the actual URL.

### Issue: Cannot find module errors

Run:
```bash
npm install
```

---

## 📝 API Requirements

The backend should provide these endpoints:

### GET `/booking`
Returns all bookings as an object:
```json
{
  "07.02/10:00-11:30/אחוזת השכן": {
    "firstName": "דני",
    "lastName": "כהן",
    "phoneNumber": "0501234567",
    "email": "example@example.com",
    "experience": "מתחילים",
    "participants": 4,
    "notes": "",
    "bookingID": "07.02/10:00-11:30",
    "room": "אחוזת השכן",
    "color": "#fecaca"
  }
}
```

### POST `/booking`
Creates a new booking. Request body:
```json
{
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string",
  "email": "string",
  "experience": "מתחילים" | "מנוסים",
  "participants": number,
  "notes": "string",
  "bookingID": "DD.MM/HH:MM-HH:MM",
  "room": "string",
  "color": "#fecaca"
}
```

---

## 🎨 Customization

### Change Available Time Slots
Edit `src/components/BookingCalendar.jsx`:
```javascript
const appointmentsPerDay = 7;  // Number of slots per day
const startTime = 10;           // Start hour (24h format)
const appointmentDuration = 90; // Duration in minutes
const breakDuration = 30;       // Break between slots in minutes
```

### Change Room Names
Edit `src/components/BookingCalendar.jsx`:
```javascript
const rooms = ["אחוזת השכן", "מקדש הקאמי", "ההתערבות", "אינפיניטי", "נרקוס"];
```

### Change API URL
Edit `.env`:
```env
VITE_API_URL=http://your-api-url:port
```

---

## 📦 Dependencies

- **React 19.0.0**: UI framework
- **React DOM 19.0.0**: React rendering
- **Axios 1.9.0**: HTTP client
- **date-fns 4.1.0**: Date utilities
- **Vite 6.3.1**: Build tool

---

## 🚢 Docker Deployment

Dockerfile and nginx.conf are included for containerized deployment:

```bash
docker build -t escapecenter-web .
docker run -p 80:80 escapecenter-web
```

---

## 📄 License

Private project for EscapeCenter.

---

## 🆘 Need Help?

If you run into issues:
1. Check that Node.js is installed: `node --version`
2. Ensure backend is running at the configured URL
3. Check browser console for detailed error messages
4. Try the serverless demo to test the UI without backend dependencies
