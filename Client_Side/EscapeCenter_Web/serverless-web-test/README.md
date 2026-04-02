# 🧪 EscapeCenter Serverless Test Version

This is a **standalone demo version** of the EscapeCenter booking web client that runs **without a backend server**.

## Features
- ✅ Displays booking calendar with mock data
- ✅ Shows pre-filled sample bookings
- ✅ Allows creating new bookings (stored in browser memory only)
- ✅ No backend/API required

## Quick Start

### 1. Install Dependencies
```bash
cd serverless-web-test
npm install
```

### 2. Run the Demo
```bash
npm run dev
```

The app will open at `http://localhost:5173` (or another port if 5173 is busy)

## How to Use

1. Open your browser to the dev server URL
2. Browse the weekly calendar view
3. Click on available (green) slots to make a booking
4. Fill out the booking form
5. Submit - the booking will be saved in browser memory
6. Refresh the page to reset all demo data

## Differences from Production Version

| Feature | Production | Demo |
|---------|-----------|------|
| Data Source | Backend API | Mock data in code |
| Data Persistence | Database | Browser memory (resets on refresh) |
| Connection Status | Real API check | Always connected |
| Pre-filled Bookings | From database | Hardcoded samples |

## File Structure
```
serverless-web-test/
├── BookingCalendar.jsx    # Mock component with sample data
├── BookingCalendar.css    # Styling (includes demo banner)
└── README.md              # This file
```

## Note
This version is for **testing and demonstration purposes only**. For production use, see the main `EscapeCenter-Web` directory.
