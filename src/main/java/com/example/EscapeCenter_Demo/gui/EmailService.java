package com.example.EscapeCenter_Demo.gui;

import com.example.EscapeCenter_Demo.Booking;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    public static void sendBookMail(Booking booking, String date, String timeRange) {
        try {
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
                    booking.getFirstName(), booking.getLastName(),
                    booking.getRoom(),
                    date,
                    timeRange,
                    booking.getParticipants(),
                    booking.getExperience(),
                    booking.getPhoneNumber(),
                    booking.getEmail(),
                    booking.getNotes().isEmpty() ? "אין" : booking.getNotes()
            );

            EmailService.sendEmail(
                    booking.getEmail(),
                    booking.getFirstName() + " " + booking.getLastName() + " - אישור הזמנה Escape Center",
                    message
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public static void sendEmail(String to, String subject, String text) throws MessagingException {
        final String from = "t7082687@gmail.com";
        final String password = "rzea wufa tajt zfeu"; // not your Gmail password!

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(text);

        Transport.send(message);
        System.out.println("Email sent successfully");
    }
}

