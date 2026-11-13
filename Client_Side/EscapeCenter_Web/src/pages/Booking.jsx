import React, { useState, useEffect, useMemo } from "react";
import axios from "axios";
import { useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import "./Booking.css";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const MISSING_DETAILS_MESSAGE = "יש למלא שם פרטי, שם משפחה וטלפון.";
const SAVE_FAILED_MESSAGE = "שגיאה בשמירת ההזמנה.";
const SLOT_TAKEN_MESSAGE = "המשבצת הזו כבר נתפסה, בחרו שעה אחרת.";
const BOOKING_SAVED_MESSAGE = "הזמנה נשמרה בהצלחה!";
const TRY_AGAIN_MESSAGE = "ההזמנה לא נשמרה, נסו שוב.";
// Sent by the API in the "code" field of a 409 that is not a taken slot.
const TRY_AGAIN_CODE = "TRY_AGAIN";
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const BOOKING_COLOR = "#fecaca";
const MONTHS_BOOKABLE_AHEAD = 3;
const WEEKDAY_INITIALS = ["א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"];
const LOADING = "loading";
const READY = "ready";
const FAILED = "failed";
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

const startOfToday = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
};

const lastBookableDay = today => {
    const last = new Date(today);
    last.setMonth(last.getMonth() + MONTHS_BOOKABLE_AHEAD);
    return last;
};

const firstOfMonth = day => new Date(day.getFullYear(), day.getMonth(), 1);

const addMonths = (month, count) => new Date(month.getFullYear(), month.getMonth() + count, 1);

// The days of a month, after one empty cell for each weekday before the 1st, so every date sits under its weekday.
const monthCells = month => {
    const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    const emptyCells = Array(month.getDay()).fill(null);
    const days = Array.from({ length: daysInMonth }, (_, index) =>
        new Date(month.getFullYear(), month.getMonth(), index + 1));
    return [...emptyCells, ...days];
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
    const today = useMemo(startOfToday, []);
    const lastDay = useMemo(() => lastBookableDay(today), [today]);
    const [selectedDate, setSelectedDate] = useState(today);
    const [visibleMonth, setVisibleMonth] = useState(() => firstOfMonth(today));
    const [availability, setAvailability] = useState({ status: LOADING, slots: [] });
    // Changing this number loads the chosen day again, after a booking or when the customer asks to retry.
    const [reloadCount, setReloadCount] = useState(0);
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [formData, setFormData] = useState(EMPTY_FORM);
    const [isSaving, setIsSaving] = useState(false);

    const reloadDay = () => setReloadCount(count => count + 1);

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
        if (!selectedRoom) return;
        // A customer who clicks through dates quickly must not see the slots of an earlier click arrive late.
        let isStale = false;
        setAvailability({ status: LOADING, slots: [] });
        axios.get(`${API_URL}/api/rooms/${selectedRoom.id}/availability`, { params: { date: toDateParam(selectedDate) } })
            .then(response => {
                if (!isStale) setAvailability({ status: READY, slots: response.data });
            })
            .catch(err => {
                console.error("Error loading availability", err);
                if (!isStale) setAvailability({ status: FAILED, slots: [] });
            });
        return () => {
            isStale = true;
        };
    }, [selectedRoom, selectedDate, reloadCount]);

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
                reloadDay();
            })
            .catch(err => {
                const status = err.response?.status;
                if (status === HTTP_CONFLICT && err.response.data?.code === TRY_AGAIN_CODE) {
                    alert(TRY_AGAIN_MESSAGE);
                    return;
                }
                if (status === HTTP_CONFLICT) {
                    alert(SLOT_TAKEN_MESSAGE);
                    setSelectedSlot(null);
                    reloadDay();
                    return;
                }
                const serverReason = status === HTTP_BAD_REQUEST ? `\n${err.response.data.detail}` : "";
                alert(SAVE_FAILED_MESSAGE + serverReason);
                console.error(err);
            })
            .finally(() => setIsSaving(false));
    };

    const isBookable = day => day >= today && day <= lastDay;
    const hasEarlierMonth = visibleMonth > firstOfMonth(today);
    const hasLaterMonth = addMonths(visibleMonth, 1) <= lastDay;

    const renderDay = (day, cellIndex) => {
        if (!day) return <span key={`empty-${cellIndex}`} />;

        const isSelected = day.getTime() === selectedDate.getTime();
        const isToday = day.getTime() === today.getTime();
        return (
            <button
                key={day.getTime()}
                type="button"
                className={`day-cell ${isSelected ? "selected" : ""} ${isToday ? "today" : ""}`}
                onClick={() => setSelectedDate(day)}
                disabled={!isBookable(day)}
                aria-pressed={isSelected}
                aria-label={day.toLocaleDateString("he-IL", { weekday: "long", day: "numeric", month: "long" })}
            >
                {day.getDate()}
            </button>
        );
    };

    const renderSlots = () => {
        if (availability.status === LOADING) {
            return <p className="slots-message" role="status">טוענים את השעות…</p>;
        }
        if (availability.status === FAILED) {
            return (
                <div className="slots-message" role="alert">
                    <p>השעות של התאריך הזה לא נטענו.</p>
                    <button type="button" className="btn btn-secondary" onClick={reloadDay}>נסו שוב</button>
                </div>
            );
        }
        if (!availability.slots.some(slot => slot.available)) {
            return <p className="slots-message" role="status">אין שעות פנויות בתאריך הזה. בחרו תאריך אחר בלוח.</p>;
        }
        return (
            <div className="time-slots">
                {availability.slots.map(slot => (
                    <button
                        key={slot.startAt}
                        type="button"
                        className={`slot-button ${slot.available ? "available" : "booked"}`}
                        onClick={() => setSelectedSlot(slot)}
                        disabled={!slot.available}
                    >
                        <span className="slot-time">{timeOf(slot.startAt)}-{timeOf(slot.endAt)}</span>
                        <span className={`slot-status ${slot.available ? "status-available" : "status-booked"}`}>
                            {slot.available ? "פנוי" : "תפוס"}
                        </span>
                    </button>
                ))}
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

                        <div className="calendar-wrapper">
                            <section className="date-picker" aria-labelledby="date-heading">
                                <h2 id="date-heading" className="step-heading">בחרו תאריך</h2>
                                <div className="month-nav">
                                    {/* The page reads right to left, so "earlier" points right. */}
                                    <button
                                        type="button"
                                        className="month-nav-button"
                                        onClick={() => setVisibleMonth(addMonths(visibleMonth, -1))}
                                        disabled={!hasEarlierMonth}
                                        aria-label="החודש הקודם"
                                    >
                                        <ChevronRight aria-hidden="true" />
                                    </button>
                                    <span className="month-name" aria-live="polite">
                                        {visibleMonth.toLocaleDateString("he-IL", { month: "long", year: "numeric" })}
                                    </span>
                                    <button
                                        type="button"
                                        className="month-nav-button"
                                        onClick={() => setVisibleMonth(addMonths(visibleMonth, 1))}
                                        disabled={!hasLaterMonth}
                                        aria-label="החודש הבא"
                                    >
                                        <ChevronLeft aria-hidden="true" />
                                    </button>
                                </div>
                                <div className="month-grid weekday-row" aria-hidden="true">
                                    {WEEKDAY_INITIALS.map(initial => <span key={initial}>{initial}</span>)}
                                </div>
                                <div className="month-grid">
                                    {monthCells(visibleMonth).map(renderDay)}
                                </div>
                                <p className="picker-hint">אפשר להזמין עד שלושה חודשים מראש.</p>
                            </section>

                            <section className="slot-picker" aria-labelledby="slot-heading">
                                <h2 id="slot-heading" className="step-heading">בחרו שעה</h2>
                                <p className="chosen-date">
                                    {selectedRoom.name}, {selectedDate.toLocaleDateString("he-IL", { weekday: "long", day: "numeric", month: "long" })}
                                </p>
                                {renderSlots()}
                            </section>
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
