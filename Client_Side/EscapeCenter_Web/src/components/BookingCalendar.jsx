import React, { useState, useEffect } from "react";
import axios from "axios";
import "../style/BookingCalendar.css";

const BookingCalendar = () => {
    const [isConnected, setIsConnected] = useState(false);
    const [bookings, setBookings] = useState({});
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [formData, setFormData] = useState({
        firstName: "",
        lastName: "",
        phoneNumber: "",
        email: "",
        experience: "מתחילים",
        notes: "",
        participants: 2
    });

    const appointmentDuration = 90;
    const breakDuration = 30;
    const appointmentsPerDay = 7;
    const startTime = 10;
    const room = "room1"; // You can later allow selecting room
    const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

    useEffect(() => {
        axios.get(`${API_URL}/bookings`)
            .then(res => {
                setBookings(res.data || {});
                setIsConnected(true);
            })
            .catch(err => {
                console.error("Error loading bookings", err);
                setIsConnected(false);
            });
    }, []);

    const handleInputChange = e => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = () => {
        if (!selectedSlot) return;

        const bookingID = `${selectedSlot.split('/')[0]}/${selectedSlot.split('/')[1]}`;

        const booking = {
            ...formData,
            bookingID: bookingID,
            room: selectedSlot.split('/')[2],
            color: "#fecaca"
        };

        axios.post(`${API_URL}/addBooking`, booking)
            .then(() => {
                alert("הזמנה נשמרה בהצלחה!");
                setBookings(prev => ({ ...prev, [bookingID]: booking }));
                setSelectedSlot(null);
            })
            .catch(err => {
                alert("שגיאה בשמירת ההזמנה.");
                console.error(err);
            });
    };

    const rooms = ["אחוזת השכן", "מקדש הקאמי", "ההתערבות", "אינפיניטי", "נרקוס"];

    const renderDayColumn = (dayOffset) => {
        const today = new Date();
        const sunday = new Date(today.setDate(today.getDate() - today.getDay()));
        const day = new Date(sunday);
        day.setDate(sunday.getDate() + dayOffset);

        const dateKey = day.toLocaleDateString("he-IL", { day: '2-digit', month: '2-digit' }).replace("/", ".");

        return (
            <div className="day-column" key={dayOffset}>
                <div className="day-label">
                    {day.toLocaleDateString("he-IL", {
                        weekday: "long",
                        day: "2-digit",
                        month: "2-digit",
                    })}
                </div>

                <div className="day-grid">
                    {/* Room headers */}
                    {rooms.map(room => (
                        <div className="room-header" key={`${room}-header`}>
                            {room}
                        </div>
                    ))}

                    {/* Time slots column under each room */}
                    {Array.from({ length: appointmentsPerDay }).map((_, i) =>
                        rooms.map(room => {
                            const start = new Date(day);
                            start.setHours(
                                startTime + Math.floor((i * (appointmentDuration + breakDuration)) / 60)
                            );
                            start.setMinutes(
                                (i * (appointmentDuration + breakDuration)) % 60
                            );
                            const end = new Date(
                                start.getTime() + appointmentDuration * 60000
                            );

                            const timeRange = `${start.toTimeString().substring(0, 5)}-${end
                                .toTimeString()
                                .substring(0, 5)}`;
                            const bookingKey = `${dateKey}/${timeRange}/${room}`;
                            const existing = bookings[bookingKey];

                            return (
                                <button
                                    key={bookingKey}
                                    className={`slot-button ${existing ? "booked" : "available"}`}
                                    onClick={() => setSelectedSlot(bookingKey)}
                                    disabled={!!existing}
                                >
                                    {timeRange}
                                    <br />
                                    {existing ? "תפוס" : "פנוי"}
                                </button>
                            );
                        })
                    )}
                </div>
            </div>

        );
    };


    return (
        <div className="calendar-container">
            {isConnected ? (
                <div className="week-view">
                    {[...Array(7)].map((_, i) => renderDayColumn(i))}
                </div>
            ) : (
                <div className="error-message">לא ניתן להתחבר לשרת. אנא נסה שוב מאוחר יותר.</div>
            )}

            {selectedSlot && (
                <div className="booking-modal">
                    <h3>הזמנת משחק</h3>
                    <p>תאריך: {selectedSlot.split('/')[0]}</p>
                    <p>חדר: {selectedSlot.split('/')[2]}</p>
                    <p>שעה: {selectedSlot.split('/')[1]}</p>
                    <input type="text" name="firstName" placeholder="שם פרטי" value={formData.firstName} onChange={handleInputChange} />
                    <input type="text" name="lastName" placeholder="שם משפחה" value={formData.lastName} onChange={handleInputChange} />
                    <input type="text" name="phoneNumber" placeholder="טלפון" value={formData.phoneNumber} onChange={handleInputChange} />
                    <input type="email" name="email" placeholder="אימייל" value={formData.email} onChange={handleInputChange} />
                    <select name="participants" value={formData.participants} onChange={handleInputChange}>
                        {[...Array(11)].map((_, i) => <option key={i + 2} value={i + 2}>{i + 2}</option>)}
                    </select>
                    <select name="experience" value={formData.experience} onChange={handleInputChange}>
                        <option value="מתחילים">מתחילים</option>
                        <option value="מנוסים">מנוסים</option>
                    </select>
                    <textarea name="notes" placeholder="הערות" value={formData.notes} onChange={handleInputChange}></textarea>
                    <button onClick={handleSubmit}>שלח</button>
                    <button onClick={() => setSelectedSlot(null)}>ביטול</button>
                </div>
            )}
        </div>
    );
};

export default BookingCalendar;
