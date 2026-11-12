package notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {
    private static final Map<String, String> VALUES = Map.of("firstName", "Dana", "roomName", "Mansion",
            "startAt", "13/11/2026 10:00", "participants", "4", "bookingId", "7");

    @Mock
    private JavaMailSender mailSender;

    // A typo in a placeholder (for example ${firstname}) would otherwise only show up on the first real cancellation.
    @Test
    void everyTemplateRendersWithoutLeftoverPlaceholders() {
        EmailSender emailSender = new EmailSender(mailSender, "noreply@test");

        for (EmailTemplate template : EmailTemplate.values()) {
            emailSender.send("dana@example.com", template, VALUES);
        }

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(sent.capture());
        assertFalse(sent.getAllValues().isEmpty());
        sent.getAllValues().forEach(message -> assertFalse(message.getText().contains("${")));
    }
}
