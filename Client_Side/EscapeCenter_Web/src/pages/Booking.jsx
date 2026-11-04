import React, { useState, useEffect, useMemo, useCallback } from "react";
import axios from "axios";
import { useSearchParams } from "react-router-dom";
import "./Booking.css";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const MISSING_DETAILS_MESSAGE = "יש למלא שם פרטי, שם משפחה וטלפון.";
const SAVE_FAILED_MESSAGE = "שגיאה בשמירת ההזמנה.";
const SLOT_TAKEN_MESSAGE = "המשבצת הזו כבר נתפסה, בחרו שעה אחרת.";
const BOOKING_SAVED_MESSAGE = "הזמנה נשמרה בהצלחה!";
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const BOOKING_COLOR = "#fecaca";
const DAYS_IN_WEEK = 7;
const ROOM_ICONS = {
    "אחוזת השכן": "🏚️",
    "מקדש הקאמי": "⛩️",
    "ההתערבות": "🕵️",
    "אינפיניטי": "♾️",
    "נרקוס": "💊"
};
const EMPTY_FORM = {
    firstName: "",
    lastName: "",
    phoneNumber: "",
    email: "",
    experience: "מתחילים",
    notes: "",
    participants: 2
};

const getWeekDays = () => {
    const today = new Date();
    const sunday = new Date(today.setDate(today.getDate() - today.getDay()));
    return Array.from({ length: DAYS_IN_WEEK }, (_, offset) => {
        const day = new Date(sunday);
        day.setDate(sunday.getDate() + offset);
        return day;
    });
};

// "en-CA" prints a date as YYYY-MM-DD in local time, which is what the API expects.
const toDateParam = day => day.toLocaleDateString("en-CA");
const timeOf = isoDateTime => isoDateTime.substring(11, 16);

const Booking = () => {
    const [searchParams] = useSearchParams();
    const preSelectedRoom = searchParams.get('room');

    const [isConnected, setIsConnected] = useState(false);
    const [rooms, setRooms] = useState([]);
    const [selectedRoom, setSelectedRoom] = useState(null);
    const [weekSlots, setWeekSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [formData, setFormData] = useState(EMPTY_FORM);
    const [isSaving, setIsSaving] = useState(false);

    const weekDays = useMemo(getWeekDays, []);

    const loadWeek = useCallback(room =>
        Promise.all(weekDays.map(day =>
            axios.get(`${API_URL}/api/rooms/${room.id}/availability`, { params: { date: toDateParam(day) } })
        ))
            .then(responses => setWeekSlots(responses.map(response => response.data)))
            .catch(err => console.error("Error loading availability", err)), [weekDays]);

    useEffect(() => {
        axios.get(`${API_URL}/api/rooms`)
            .then(res => {
                setRooms(res.data);
                setSelectedRoom(res.data.find(room => room.name === preSelectedRoom) || res.data[0] || null);
                setIsConnected(true);
            })
            .catch(err => {
                console.error("Error loading rooms", err);
                setIsConnected(false);
            });
    }, [preSelectedRoom]);

    useEffect(() => {
        if (selectedRoom) loadWeek(selectedRoom);
    }, [selectedRoom, loadWeek]);

    const handleInputChange = e => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = () => {
        if (!selectedSlot) return;

        const { firstName, lastName, phoneNumber, email, experience, notes, participants } = formData;
        if (![firstName, lastName, phoneNumber].every(value => value.trim())) {
            alert(MISSING_DETAILS_MESSAGE);
            return;
        }

        setIsSaving(true);
        axios.post(`${API_URL}/api/bookings`, {
            roomId: selectedRoom.id,
            startAt: selectedSlot.startAt,
            client: { firstName, lastName, phoneNumber, email },
            booking: { participants: Number(participants), experience, notes, color: BOOKING_COLOR }
        })
            .then(() => {
                alert(BOOKING_SAVED_MESSAGE);
                setSelectedSlot(null);
                setFormData(EMPTY_FORM);
                loadWeek(selectedRoom);
            })
            .catch(err => {
                const status = err.response?.status;
                if (status === HTTP_CONFLICT) {
                    alert(SLOT_TAKEN_MESSAGE);
                    setSelectedSlot(null);
                    loadWeek(selectedRoom);
                    return;
                }
                const serverReason = status === HTTP_BAD_REQUEST ? `\n${err.response.data.detail}` : "";
                alert(SAVE_FAILED_MESSAGE + serverReason);
                console.error(err);
            })
            .finally(() => setIsSaving(false));
    };

    const renderDayColumn = (day, dayIndex) => {
        const slots = weekSlots[dayIndex] || [];

        return (
            <div className="day-column" key={dayIndex}>
                <div className="day-label">
                    <div className="day-name">
                        {day.toLocaleDateString("he-IL", { weekday: "short" })}
                    </div>
                    <div className="day-date">
                        {day.toLocaleDateString("he-IL", { day: '2-digit', month: '2-digit' })}
                    </div>
                </div>

                <div className="time-slots">
                    {slots.map(slot => {
                        const timeRange = `${timeOf(slot.startAt)}-${timeOf(slot.endAt)}`;

                        return (
                            <button
                                key={slot.startAt}
                                className={`slot-button ${slot.available ? "available" : "booked"}`}
                                onClick={() => setSelectedSlot(slot)}
                                disabled={!slot.available}
                            >
                                <span className="slot-time">{timeRange}</span>
                                <span className={`slot-status ${slot.available ? "status-available" : "status-booked"}`}>
                                    {slot.available ? "פנוי" : "תפוס"}
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

                {isConnected && selectedRoom ? (
                    <>
                        {/* Room Selector */}
                        <div className="room-selector">
                            {rooms.map(room => (
                                <button
                                    key={room.id}
                                    className={`room-tab ${selectedRoom.id === room.id ? "active" : ""}`}
                                    onClick={() => setSelectedRoom(room)}
                                >
                                    <span className="room-icon">{ROOM_ICONS[room.name]}</span>
                                    <span className="room-label">{room.name}</span>
                                </button>
                            ))}
                        </div>

                        {/* Calendar */}
                        <div className="calendar-wrapper">
                            <div className="calendar-header">
                                <h3 className="heading-4">זמינות עבור {selectedRoom.name}</h3>
                            </div>
                            <div className="week-view">
                                {weekDays.map(renderDayColumn)}
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
                                <p><strong>חדר:</strong> {selectedRoom.name}</p>
                                <p><strong>תאריך:</strong> {new Date(selectedSlot.startAt).toLocaleDateString("he-IL")}</p>
                                <p><strong>שעה:</strong> {timeOf(selectedSlot.startAt)}-{timeOf(selectedSlot.endAt)}</p>
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
                                        {[...Array(selectedRoom.capacity - 1)].map((_, i) => <option key={i + 2} value={i + 2}>{i + 2} משתתפים</option>)}
                                    </select>
                                    <select name="experience" value={formData.experience} onChange={handleInputChange}>
                                        <option value="מתחילים">מתחילים</option>
                                        <option value="מנוסים">מנוסים</option>
                                    </select>
                                </div>
                                <textarea name="notes" placeholder="הערות" value={formData.notes} onChange={handleInputChange}></textarea>

                                <div className="modal-actions">
                                    <button className="btn btn-primary" onClick={handleSubmit} disabled={isSaving}>אשר הזמנה</button>
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
