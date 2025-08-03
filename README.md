# EscapeCenter-Demo

**EscapeCenter-Demo** is a Java-based BackEnd application for managing escape room bookings. It provides a booking interface for users and a robust server-side system for managing schedules, room availability, and user interactions. This demo project showcases a working example of a room booking platform, a clean GUI, and basic booking validation logic.

## 🔧 Technologies Used

### Backend
- **Java 22**
- **Spring Boot**
- **Jakarta Mail API (Email Notifications)**

### (Java GUI)
- **JavaFX** (for the admin interface)

## 📸 Screenshots

> - Login Screen 
<img width="357" height="229" alt="מסך התחברות עובד" src="https://github.com/user-attachments/assets/f5d37e7b-2772-4361-9dd8-abf78cb02716" />

> - Admin panel
<img width="404" height="332" alt="מסך ראשי" src="https://github.com/user-attachments/assets/2d78e410-6ef1-4b1a-9f40-d60fbb65d778" />

> - Clients information screen
<img width="601" height="625" alt="מסך מידע לקוחות" src="https://github.com/user-attachments/assets/6097b17b-2380-4c08-9e09-3252b7eef441" />

> - Booking calendar interface
<img width="1195" height="721" alt="לוח הזמנות ממוזער" src="https://github.com/user-attachments/assets/740e6c96-01c8-4b48-b80c-52b9846dbe99" />
<img width="1176" height="539" alt="המשך לוח הזמנות ממוזער" src="https://github.com/user-attachments/assets/7cd07a9b-accc-437a-b27d-4e1bd1d30a54" />
<img width="2547" height="1380" alt="לוח הזמנות מסך מלא" src="https://github.com/user-attachments/assets/5a91b4fc-4ab4-470b-9b1e-7fac494d0da0" />

## 🎯 Features

- 🔐 Admin GUI for viewing and managing all room bookings.
- 📆 Weekly view of all room availability.
- ✅ Real-time slot availability feedback (Green = Available, Red = Booked).
- ✉️ Email confirmation after successful booking (via Jakarta Mail).
- 🗃️ In-memory database for quick testing and development.
- 🕐 Local date/time handling with week-based scheduling.
