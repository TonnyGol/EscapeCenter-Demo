export const EMPTY_FORM = {
    firstName: "",
    lastName: "",
    phoneNumber: "",
    email: "",
    experience: "מתחילים",
    notes: "",
    participants: 2
};

const REQUIRED_MESSAGES = {
    firstName: "יש למלא שם פרטי.",
    lastName: "יש למלא שם משפחה.",
    phoneNumber: "יש למלא מספר טלפון."
};
const PHONE_DIGITS_MESSAGE = "מספר הטלפון צריך לכלול ספרות.";
const EMAIL_MESSAGE = "כתובת האימייל לא תקינה.";
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

// The fields in the order they appear on the form, so the first wrong one can get focus.
export const FIELD_ORDER = ["firstName", "lastName", "phoneNumber", "email"];

/** One message per wrong field; an empty object means the form can be sent. */
export const validateForm = form => {
    const errors = {};
    Object.entries(REQUIRED_MESSAGES).forEach(([name, message]) => {
        if (!form[name].trim()) errors[name] = message;
    });
    if (!errors.phoneNumber && !/\d/.test(form.phoneNumber)) errors.phoneNumber = PHONE_DIGITS_MESSAGE;
    if (form.email.trim() && !EMAIL_PATTERN.test(form.email.trim())) errors.email = EMAIL_MESSAGE;
    return errors;
};

export const fieldId = name => `booking-${name}`;

/** The tier with the largest "from" not above the group; a group below the first tier pays the first tier's price. */
export const pricePerPerson = (room, participants) => {
    const count = Number(participants);
    const tier = [...room.prices].reverse().find(candidate => candidate.fromParticipants <= count) ?? room.prices[0];
    return tier ? tier.pricePerParticipant : null;
};

/** null when the room has no price tiers, so the page shows no price instead of a free game. */
export const totalPrice = (room, participants) => {
    const perPerson = pricePerPerson(room, participants);
    return perPerson === null ? null : perPerson * Number(participants);
};

/** Group sizes the room accepts, from its minimum to its capacity. */
export const participantOptions = room =>
    Array.from({ length: room.capacity - room.minParticipants + 1 }, (_, index) => room.minParticipants + index);

export const clampParticipants = (room, participants) =>
    Math.min(Math.max(Number(participants), room.minParticipants), room.capacity);

export const formatPrice = amount => `${amount} ₪`;
