import React, { useState, useEffect, useMemo } from "react";
import axios from "axios";
import { Link, useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight, Clock, Users } from "lucide-react";
import { RoomIcon } from "../roomVisuals";
import { ROOM_THEMES } from "../roomThemes";
import { ROOMS } from "../roomsData";
import { API_URL } from "../api";
import BookingConfirmation from "./BookingConfirmation";
import BookingDialog from "./BookingDialog";
import PageTitle from "../PageTitle";
import { EMPTY_FORM, FIELD_ORDER, clampParticipants, fieldId, totalPrice, validateForm } from "./bookingForm";
import "./Booking.css";

const SAVE_FAILED_MESSAGE = "שגיאה בשמירת ההזמנה.";
const SLOT_TAKEN_MESSAGE = "המשבצת הזו כבר נתפסה. בחרו שעה אחרת מהרשימה.";
const TRY_AGAIN_MESSAGE = "ההזמנה לא נשמרה, נסו שוב.";
// Sent by the API in the "code" field of a 409 that is not a taken slot.
const TRY_AGAIN_CODE = "TRY_AGAIN";
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const MONTHS_BOOKABLE_AHEAD = 3;
const WEEKDAY_INITIALS = ["א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"];
const LOADING = "loading";
const CONFIRMATION_TITLE = "ההזמנה אושרה";
const ROOMS_LOADING_MESSAGE = "טוענים את החדרים…";
const SERVER_UNREACHABLE_MESSAGE = "לא ניתן להתחבר לשרת. אנא נסה שוב מאוחר יותר.";
const READY = "ready";
const FAILED = "failed";

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

// The booking is a sequence, so the steps are numbered: room, date, time, then the details in the dialog.
const StepHeading = ({ id, number, children }) => (
    <h2 id={id} className="step-heading">
        <span className="step-number">{number}</span>
        {children}
    </h2>
);

const priceRangeOf = room => {
    const prices = room.prices.map(tier => tier.pricePerParticipant);
    return prices.length ? `${Math.min(...prices)}-${Math.max(...prices)} ₪ למשתתף` : null;
};

const RoomBanner = ({ room }) => {
    const theme = ROOM_THEMES[room.name];
    const detailId = ROOMS.find(candidate => candidate.name === room.name)?.id;
    const priceRange = priceRangeOf(room);
    return (
        <div className="room-banner">
            <div className="room-banner-poster" aria-hidden="true">
                {theme && <span className="room-banner-stamp">{theme.stamp}</span>}
                <RoomIcon name={room.name} size={56} strokeWidth={1.5} />
            </div>
            <div>
                <h3 className="heading-3">{room.name}</h3>
                <p className="text-muted">{room.description}</p>
                <ul className="room-banner-facts">
                    <li><Users size={16} aria-hidden="true" />{room.minParticipants}-{room.capacity} משתתפים</li>
                    <li><Clock size={16} aria-hidden="true" />{room.durationMinutes} דקות</li>
                    {priceRange && <li>{priceRange}</li>}
                </ul>
                {detailId && (
                    <Link to={`/rooms/${detailId}`} className="room-banner-link">פרטים ומחירון מלא</Link>
                )}
            </div>
        </div>
    );
};

const Booking = () => {
    const [searchParams] = useSearchParams();
    const preSelectedRoom = searchParams.get('room');

    const [roomsStatus, setRoomsStatus] = useState(LOADING);
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
    const [fieldErrors, setFieldErrors] = useState({});
    // Set when the server refuses the booking; the dialog shows it above the form.
    const [formError, setFormError] = useState(null);
    // A message on the page itself for what happened after the dialog closed: the slot was taken.
    const [notice, setNotice] = useState(null);
    // The booking that was just saved; while it is set the page shows the confirmation instead of the booking steps.
    const [confirmation, setConfirmation] = useState(null);
    const [isSaving, setIsSaving] = useState(false);

    const reloadDay = () => setReloadCount(count => count + 1);

    const openDialog = slot => {
        setFieldErrors({});
        setFormError(null);
        setNotice(null);
        setFormData(prev => ({ ...prev, participants: clampParticipants(selectedRoom, prev.participants) }));
        setSelectedSlot(slot);
    };
    const closeDialog = () => setSelectedSlot(null);

    useEffect(() => {
        axios.get(`${API_URL}/api/rooms`)
            .then(res => {
                setRooms(res.data);
                setSelectedRoom(res.data.find(room => room.name === preSelectedRoom) || res.data[0] || null);
                setRoomsStatus(READY);
            })
            .catch(err => {
                console.error("Error loading rooms", err);
                setRoomsStatus(FAILED);
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

    useEffect(() => {
        setNotice(null);
    }, [selectedRoom, selectedDate]);

    const handleInputChange = e => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        setFieldErrors(prev => ({ ...prev, [name]: undefined }));
    };

    const handleSubmit = () => {
        if (!selectedSlot) return;

        const errors = validateForm(formData);
        const firstWrongField = FIELD_ORDER.find(name => errors[name]);
        if (firstWrongField) {
            setFieldErrors(errors);
            document.getElementById(fieldId(firstWrongField))?.focus();
            return;
        }

        const { firstName, lastName, phoneNumber, email, experience, notes, participants } = formData;
        setIsSaving(true);
        setFormError(null);
        axios.post(`${API_URL}/api/bookings`, {
            roomId: selectedRoom.id,
            startAt: selectedSlot.startAt,
            client: { firstName, lastName, phoneNumber, email },
            booking: { participants: Number(participants), experience, notes }
        })
            .then(() => {
                closeDialog();
                setFormData(EMPTY_FORM);
                setConfirmation({
                    roomName: selectedRoom.name,
                    startAt: selectedSlot.startAt,
                    endAt: selectedSlot.endAt,
                    participants: Number(participants),
                    total: totalPrice(selectedRoom, participants),
                    email: email.trim()
                });
            })
            .catch(err => {
                const status = err.response?.status;
                if (status === HTTP_CONFLICT && err.response.data?.code === TRY_AGAIN_CODE) {
                    setFormError({ message: TRY_AGAIN_MESSAGE });
                    return;
                }
                if (status === HTTP_CONFLICT) {
                    closeDialog();
                    setNotice({ type: "warning", text: SLOT_TAKEN_MESSAGE });
                    reloadDay();
                    return;
                }
                console.error(err);
                setFormError({
                    message: SAVE_FAILED_MESSAGE,
                    reason: status === HTTP_BAD_REQUEST ? err.response.data.detail : undefined
                });
            })
            .finally(() => setIsSaving(false));
    };

    const bookAnother = () => {
        setConfirmation(null);
        reloadDay();
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
                        onClick={() => openDialog(slot)}
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

    const themeKeyOf = roomName => ROOM_THEMES[roomName]?.key;

    if (confirmation) {
        return (
            <div className="booking-page" data-room={themeKeyOf(confirmation.roomName)}>
                <PageTitle>{CONFIRMATION_TITLE}</PageTitle>
                <div className="container">
                    <BookingConfirmation confirmation={confirmation} onBookAnother={bookAnother} />
                </div>
            </div>
        );
    }

    return (
        <div className="booking-page" data-room={selectedRoom ? themeKeyOf(selectedRoom.name) : undefined}>
            <PageTitle>הזמנת חדר</PageTitle>
            <div className="container">
                <div className="booking-header">
                    <h1 className="heading-1">הזמנת חדר בריחה</h1>
                    <p className="booking-subtitle text-muted">
                        בחרו חדר, תאריך ושעה - ואנחנו נדאג לשאר
                    </p>
                </div>

                {roomsStatus === READY && selectedRoom ? (
                    <>
                        <section aria-labelledby="room-heading">
                        <StepHeading id="room-heading" number={1}>בחרו חדר</StepHeading>
                        <div className="room-selector">
                            {rooms.map(room => (
                                <button
                                    key={room.id}
                                    data-room={themeKeyOf(room.name)}
                                    className={`room-tab ${selectedRoom.id === room.id ? "active" : ""}`}
                                    aria-pressed={selectedRoom.id === room.id}
                                    onClick={() => setSelectedRoom(room)}
                                >
                                    <RoomIcon name={room.name} className="room-icon" size={20} />
                                    <span className="room-label">{room.name}</span>
                                </button>
                            ))}
                        </div>
                        <RoomBanner room={selectedRoom} />
                        </section>

                        <div className="calendar-wrapper">
                            <section className="date-picker" aria-labelledby="date-heading">
                                <StepHeading id="date-heading" number={2}>בחרו תאריך</StepHeading>
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
                                <StepHeading id="slot-heading" number={3}>בחרו שעה</StepHeading>
                                <p className="chosen-date">
                                    {selectedRoom.name}, {selectedDate.toLocaleDateString("he-IL", { weekday: "long", day: "numeric", month: "long" })}
                                </p>
                                {notice && (
                                    <p className={`slots-notice ${notice.type}`} role={notice.type === "warning" ? "alert" : "status"}>
                                        {notice.text}
                                    </p>
                                )}
                                {renderSlots()}
                            </section>
                        </div>
                    </>
                ) : (
                    roomsStatus === LOADING
                        ? <p className="slots-message" role="status">{ROOMS_LOADING_MESSAGE}</p>
                        : <div className="error-message">{SERVER_UNREACHABLE_MESSAGE}</div>
                )}

                {selectedSlot && (
                    <BookingDialog
                        room={selectedRoom}
                        slot={selectedSlot}
                        form={formData}
                        fieldErrors={fieldErrors}
                        formError={formError}
                        isSaving={isSaving}
                        onChange={handleInputChange}
                        onSubmit={handleSubmit}
                        onClose={closeDialog}
                    />
                )}
            </div>
        </div>
    );
};

export default Booking;
