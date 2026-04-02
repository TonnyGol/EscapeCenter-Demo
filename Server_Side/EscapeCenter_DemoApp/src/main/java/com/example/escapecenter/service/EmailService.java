package com.example.escapecenter.service;

import com.example.escapecenter.model.Booking;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

/**
 * Email service for sending booking confirmations and cancellation notices.
 * Reads SMTP credentials from Session (loaded from config.properties).
 */
public final class EmailService {

    private EmailService() {}

    /**
     * Send a booking confirmation email to the client.
     */
    public static void sendBookingConfirmation(Booking booking, String date, String timeRange) {
        String message = String.format("""
                שלום %s %s,

                ההזמנה שלך התקבלה בהצלחה!

                פרטי ההזמנה:
                ---------------------------
                חדר: %s
                תאריך: %s
                שעה: %s
                מספר משתתפים: %d
                ניסיון קודם: %s
                טלפון: %s
                אימייל: %s
                הערות: %s

                מצפים לראותך,
                צוות Escape Center
                """,
                booking.firstName(), booking.lastName(),
                booking.room(),
                date,
                timeRange,
                booking.participants(),
                booking.experience(),
                booking.phoneNumber(),
                booking.email(),
                booking.notes().isEmpty() ? "אין" : booking.notes()
        );

        try {
            sendEmail(
                    booking.email(),
                    booking.firstName() + " " + booking.lastName() + " - אישור הזמנה Escape Center",
                    message
            );
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send booking confirmation: " + e.getMessage());
        }
    }

    /**
     * Send a booking cancellation email to the client.
     */
    public static void sendCancellationNotice(Booking booking, String date, String timeRange) {
        String message = String.format("""
                שלום %s %s,

                ההזמנה שלך בוטלה בהצלחה.

                פרטי ההזמנה שבוטלה:
                ---------------------------
                חדר: %s
                תאריך: %s
                שעה: %s
                מספר משתתפים: %d
                ניסיון קודם: %s
                טלפון: %s
                אימייל: %s
                הערות: %s

                נשמח לראותך בפעם הבאה!
                צוות Escape Center
                """,
                booking.firstName(), booking.lastName(),
                booking.room(),
                date,
                timeRange,
                booking.participants(),
                booking.experience(),
                booking.phoneNumber(),
                booking.email(),
                booking.notes().isEmpty() ? "אין" : booking.notes()
        );

        try {
            sendEmail(
                    booking.email(),
                    booking.firstName() + " " + booking.lastName() + " - ביטול הזמנה Escape Center",
                    message
            );
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send cancellation notice: " + e.getMessage());
        }
    }

    /**
     * Send a plain-text email via Gmail SMTP.
     * Credentials are loaded from config.properties via Session.
     */
    public static void sendEmail(String to, String subject, String text) throws MessagingException {
        Session session = Session.getInstance();
        String from = session.getEmailSender();
        String password = session.getEmailPassword();

        Properties props = new Properties();
        props.put("mail.smtp.host", session.getSmtpHost());
        props.put("mail.smtp.port", session.getSmtpPort());
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        jakarta.mail.Session mailSession = jakarta.mail.Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(text);

        Transport.send(message);
        System.out.println("[EmailService] Email sent successfully to " + to);
    }
}
