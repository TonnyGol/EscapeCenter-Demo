package ui;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.regex.Pattern;

/** Checks of the client fields that the booking and client dialogs run before anything is sent to the server. */
final class FormChecks {

    enum Field { FIRST_NAME, LAST_NAME, PHONE, EMAIL }

    /** The first wrong field and what to tell the user about it. */
    record Problem(Field field, String message) {
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\S+@\\S+\\.\\S+$");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final String INVALID_STYLE_CLASS = "invalid-field";
    private static final String FIRST_NAME_MISSING = "יש למלא שם פרטי.";
    private static final String LAST_NAME_MISSING = "יש למלא שם משפחה.";
    private static final String PHONE_MISSING = "יש למלא מספר טלפון.";
    private static final String PHONE_WITHOUT_DIGITS = "הטלפון צריך לכלול ספרות.";
    private static final String EMAIL_INVALID = "כתובת האימייל לא תקינה.";

    private FormChecks() {
    }

    /** Fields in the order they appear in the form. An empty email is fine; a filled one must look like an address. */
    static Optional<Problem> firstProblem(String firstName, String lastName, String phone, String email) {
        if (isBlank(firstName)) return Optional.of(new Problem(Field.FIRST_NAME, FIRST_NAME_MISSING));
        if (isBlank(lastName)) return Optional.of(new Problem(Field.LAST_NAME, LAST_NAME_MISSING));
        if (isBlank(phone)) return Optional.of(new Problem(Field.PHONE, PHONE_MISSING));
        if (!DIGIT.matcher(phone).find()) return Optional.of(new Problem(Field.PHONE, PHONE_WITHOUT_DIGITS));
        if (!isBlank(email) && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return Optional.of(new Problem(Field.EMAIL, EMAIL_INVALID));
        }
        return Optional.empty();
    }

    /**
     * Keeps the dialog open when Save is pressed with a wrong field: the message goes into {@code errorLabel},
     * the field gets a red border and focus. Typing in a field clears its mark.
     */
    static void guardSave(Dialog<?> dialog, ButtonType saveType, TextField firstName, TextField lastName,
                          TextField phone, TextField email, Label errorLabel) {
        for (TextField field : new TextField[] {firstName, lastName, phone, email}) {
            field.textProperty().addListener((obs, old, now) -> {
                field.getStyleClass().remove(INVALID_STYLE_CLASS);
                errorLabel.setText("");
            });
        }
        dialog.getDialogPane().lookupButton(saveType).addEventFilter(ActionEvent.ACTION, event -> {
            Optional<Problem> problem = firstProblem(firstName.getText(), lastName.getText(), phone.getText(), email.getText());
            if (problem.isEmpty()) {
                return;
            }
            event.consume();
            TextField wrong = switch (problem.get().field()) {
                case FIRST_NAME -> firstName;
                case LAST_NAME -> lastName;
                case PHONE -> phone;
                case EMAIL -> email;
            };
            errorLabel.setText(problem.get().message());
            if (!wrong.getStyleClass().contains(INVALID_STYLE_CLASS)) {
                wrong.getStyleClass().add(INVALID_STYLE_CLASS);
            }
            wrong.requestFocus();
        });
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
