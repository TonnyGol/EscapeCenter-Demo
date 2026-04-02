import React, { useState, useEffect } from "react";
import axios from "axios";
import { useSearchParams } from "react-router-dom";
import "./Booking.css";

const Booking = () => {
    const [searchParams] = useSearchParams();
    const preSelectedRoom = searchParams.get('room');

    const [isConnected, setIsConnected] = useState(false);
    const [bookings, setBookings] = useState({});
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [selectedRoom, setSelectedRoom] = useState(preSelectedRoom || "אחוזת השכן");
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
    const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

    const rooms = [
        { id: "achuzat", name: "אחוזת השכן", icon: "🏚️" },
        { id: "kami", name: "מקדש הקאמי", icon: "⛩️" },
        { id: "intervention", name: "ההתערבות", icon: "🕵️" },
        { id: "infinity", name: "אינפיניטי", icon: "♾️" },
        { id: "narcos", name: "נרקוס", icon: "💊" }
    ];

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
                // Use the full selectedSlot as key (includes room) to match calendar lookup
                setBookings(prev => ({ ...prev, [selectedSlot]: booking }));
                setSelectedSlot(null);
                setFormData({
                    firstName: "",
                    lastName: "",
                    phoneNumber: "",
                    email: "",
                    experience: "מתחילים",
                    notes: "",
                    participants: 2
                });
            })
            .catch(err => {
                alert("שגיאה בשמירת ההזמנה.");
                console.error(err);
            });
    };

    const renderDayColumn = (dayOffset) => {
        const today = new Date();
        const sunday = new Date(today.setDate(today.getDate() - today.getDay()));
        const day = new Date(sunday);
        day.setDate(sunday.getDate() + dayOffset);

        const dateKey = day.toLocaleDateString("he-IL", { day: '2-digit', month: '2-digit' }).replace("/", ".");

        return (
            <div className="day-column" key={dayOffset}>
                <div className="day-label">
                    <div className="day-name">
                        {day.toLocaleDateString("he-IL", { weekday: "short" })}
                    </div>
                    <div className="day-date">
                        {day.toLocaleDateString("he-IL", { day: '2-digit', month: '2-digit' })}
                    </div>
                </div>

                <div className="time-slots">
                    {Array.from({ length: appointmentsPerDay }).map((_, i) => {
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
                        const bookingKey = `${dateKey}/${timeRange}/${selectedRoom}`;
                        const existing = bookings[bookingKey];

                        return (
                            <button
                                key={bookingKey}
                                className={`slot-button ${existing ? "booked" : "available"}`}
                                onClick={() => setSelectedSlot(bookingKey)}
                                disabled={!!existing}
                            >
                                <span className="slot-time">{timeRange}</span>
                                <span className={`slot-status ${existing ? "status-booked" : "status-available"}`}>
                                    {existing ? "תפוס" : "פנוי"}
                                </span>
                            </button>
                        );
                    })}
                </div>
            </div>
        );
    };

    return (
        <div className="booking-page">
            <div className="container">
                <div className="page-header">
                    <h1 className="heading-1">הזמנת חדר בריחה</h1>
                    <p className="page-subtitle text-muted">
                        בחרו חדר, תאריך ושעה - ואנחנו נדאג לשאר
                    </p>
                </div>

                {isConnected ? (
                    <>
                        {/* Room Selector */}
                        <div className="room-selector">
                            {rooms.map(room => (
                                <button
                                    key={room.id}
                                    className={`room-tab ${selectedRoom === room.name ? "active" : ""}`}
                                    onClick={() => setSelectedRoom(room.name)}
                                >
                                    <span className="room-icon">{room.icon}</span>
                                    <span className="room-label">{room.name}</span>
                                </button>
                            ))}
                        </div>

                        {/* Calendar */}
                        <div className="calendar-wrapper">
                            <div className="calendar-header">
                                <h3 className="heading-4">זמינות עבור {selectedRoom}</h3>
                            </div>
                            <div className="week-view">
                                {[...Array(7)].map((_, i) => renderDayColumn(i))}
                            </div>
                        </div>
                    </>
                ) : (
                    <div className="error-message">לא ניתן להתחבר לשרת. אנא נסה שוב מאוחר יותר.</div>
                )}

                {selectedSlot && (
                    <div className="booking-modal-overlay" onClick={() => setSelectedSlot(null)}>
                        <div className="booking-modal" onClick={(e) => e.stopPropagation()}>
                            <h3 className="heading-3">הזמנת משחק</h3>
                            <div className="modal-info">
                                <p><strong>חדר:</strong> {selectedSlot.split('/')[2]}</p>
                                <p><strong>תאריך:</strong> {selectedSlot.split('/')[0]}</p>
                                <p><strong>שעה:</strong> {selectedSlot.split('/')[1]}</p>
                            </div>

                            <div className="booking-form">
                                <div className="form-row">
                                    <input type="text" name="firstName" placeholder="שם פרטי" value={formData.firstName} onChange={handleInputChange} />
                                    <input type="text" name="lastName" placeholder="שם משפחה" value={formData.lastName} onChange={handleInputChange} />
                                </div>
                                <input type="text" name="phoneNumber" placeholder="טלפון" value={formData.phoneNumber} onChange={handleInputChange} />
                                <input type="email" name="email" placeholder="אימייל" value={formData.email} onChange={handleInputChange} />
                                <div className="form-row">
                                    <select name="participants" value={formData.participants} onChange={handleInputChange}>
                                        {[...Array(11)].map((_, i) => <option key={i + 2} value={i + 2}>{i + 2} משתתפים</option>)}
                                    </select>
                                    <select name="experience" value={formData.experience} onChange={handleInputChange}>
                                        <option value="מתחילים">מתחילים</option>
                                        <option value="מנוסים">מנוסים</option>
                                    </select>
                                </div>
                                <textarea name="notes" placeholder="הערות" value={formData.notes} onChange={handleInputChange}></textarea>

                                <div className="modal-actions">
                                    <button className="btn btn-primary" onClick={handleSubmit}>אשר הזמנה</button>
                                    <button className="btn btn-secondary" onClick={() => setSelectedSlot(null)}>ביטול</button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Booking;
