import React, { useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { formatPrice } from "./bookingForm";

const timeOf = isoDateTime => isoDateTime.substring(11, 16);

const BookingConfirmation = ({ confirmation, onBookAnother }) => {
    const { roomName, startAt, endAt, participants, total, email } = confirmation;
    const headingRef = useRef(null);

    // Screen readers start reading from the result, and keyboard users start from there too.
    useEffect(() => {
        headingRef.current?.focus();
        window.scrollTo(0, 0);
    }, []);

    return (
        <section className="confirmation" aria-labelledby="confirmation-title">
            <CheckCircle2 className="confirmation-icon" size={56} aria-hidden="true" />
            <h1 id="confirmation-title" ref={headingRef} tabIndex={-1} className="heading-2">ההזמנה אושרה</h1>

            <dl className="confirmation-details">
                <div><dt>חדר</dt><dd>{roomName}</dd></div>
                <div>
                    <dt>תאריך</dt>
                    <dd>{new Date(startAt).toLocaleDateString("he-IL", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}</dd>
                </div>
                <div><dt>שעה</dt><dd dir="ltr">{timeOf(startAt)}-{timeOf(endAt)}</dd></div>
                <div><dt>משתתפים</dt><dd>{participants}</dd></div>
                {total !== null && <div><dt>סה״כ</dt><dd>{formatPrice(total)}</dd></div>}
            </dl>

            {email && <p className="confirmation-email">אישור יישלח אל {email}.</p>}

            <div className="confirmation-actions">
                <button type="button" className="btn btn-primary" onClick={onBookAnother}>הזמנה נוספת</button>
                <Link to="/" className="btn btn-secondary">לדף הבית</Link>
            </div>
        </section>
    );
};

export default BookingConfirmation;
