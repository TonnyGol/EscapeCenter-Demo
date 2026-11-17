import React, { useRef } from "react";
import { useModalFocus } from "../useModalFocus";
import { fieldId, formatPrice, participantOptions, pricePerPerson, totalPrice } from "./bookingForm";

const timeOf = isoDateTime => isoDateTime.substring(11, 16);

const Field = ({ name, label, optional, error, children }) => (
    <div className="field">
        <label htmlFor={fieldId(name)} className="field-label">
            {label}
            {optional && <span className="field-optional"> (לא חובה)</span>}
        </label>
        {children}
        {error && <p id={`${fieldId(name)}-error`} className="field-error">{error}</p>}
    </div>
);

const TextInput = ({ name, label, optional, form, errors, onChange, ...inputProps }) => (
    <Field name={name} label={label} optional={optional} error={errors[name]}>
        <input
            id={fieldId(name)}
            name={name}
            value={form[name]}
            onChange={onChange}
            className={errors[name] ? "invalid" : undefined}
            aria-invalid={Boolean(errors[name])}
            aria-describedby={errors[name] ? `${fieldId(name)}-error` : undefined}
            {...inputProps}
        />
    </Field>
);

const BookingDialog = ({ room, slot, form, fieldErrors, formError, isSaving, onChange, onSubmit, onClose }) => {
    const dialogRef = useRef(null);
    useModalFocus(dialogRef, onClose);
    const total = totalPrice(room, form.participants);

    return (
        <div className="booking-modal-overlay" onClick={onClose}>
            <div
                ref={dialogRef}
                className="booking-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="booking-dialog-title"
                onClick={event => event.stopPropagation()}
            >
                <h2 id="booking-dialog-title" className="heading-3">הזמנת משחק</h2>
                <p className="dialog-step"><span className="step-number">4</span>הפרטים שלכם</p>
                <div className="modal-info">
                    <p><strong>חדר:</strong> {room.name}</p>
                    <p><strong>תאריך:</strong> {new Date(slot.startAt).toLocaleDateString("he-IL")}</p>
                    <p><strong>שעה:</strong> {timeOf(slot.startAt)}-{timeOf(slot.endAt)}</p>
                </div>

                <form
                    className="booking-form"
                    noValidate
                    onSubmit={event => {
                        event.preventDefault();
                        onSubmit();
                    }}
                >
                    {formError && (
                        <div className="form-banner" role="alert">
                            <p>{formError.message}</p>
                            {formError.reason && <p className="form-banner-reason">{formError.reason}</p>}
                        </div>
                    )}

                    <div className="form-row">
                        <TextInput name="firstName" label="שם פרטי" form={form} errors={fieldErrors} onChange={onChange}
                                   autoComplete="given-name" required />
                        <TextInput name="lastName" label="שם משפחה" form={form} errors={fieldErrors} onChange={onChange}
                                   autoComplete="family-name" required />
                    </div>
                    <TextInput name="phoneNumber" label="טלפון" type="tel" inputMode="tel" form={form} errors={fieldErrors}
                               onChange={onChange} autoComplete="tel" required />
                    <TextInput name="email" label="אימייל" optional type="email" inputMode="email" form={form}
                               errors={fieldErrors} onChange={onChange} autoComplete="email" />

                    <div className="form-row">
                        <Field name="participants" label="מספר משתתפים">
                            <select id={fieldId("participants")} name="participants" value={form.participants} onChange={onChange}>
                                {participantOptions(room).map(count => (
                                    <option key={count} value={count}>{count} משתתפים</option>
                                ))}
                            </select>
                        </Field>
                        <Field name="experience" label="רמת ניסיון">
                            <select id={fieldId("experience")} name="experience" value={form.experience} onChange={onChange}>
                                <option value="מתחילים">מתחילים</option>
                                <option value="מנוסים">מנוסים</option>
                            </select>
                        </Field>
                    </div>
                    <Field name="notes" label="הערות" optional>
                        <textarea id={fieldId("notes")} name="notes" value={form.notes} onChange={onChange} />
                    </Field>

                    {total !== null && (
                        <p className="price-total">
                            <strong>סה״כ לתשלום במקום:</strong> {formatPrice(total)}
                            <span className="text-muted"> ({form.participants} × {formatPrice(pricePerPerson(room, form.participants))})</span>
                        </p>
                    )}

                    <div className="modal-actions">
                        <button type="submit" className="btn btn-primary" disabled={isSaving}>
                            {isSaving ? "שומרים…" : "אשר הזמנה"}
                        </button>
                        <button type="button" className="btn btn-secondary" onClick={onClose}>ביטול</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default BookingDialog;
