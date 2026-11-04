package Requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Limits match the column sizes in db/migration/V1__init.sql.
public record ClientDetails(
        @NotBlank @Size(max = ClientDetails.MAX_NAME_LENGTH) String firstName,
        @NotBlank @Size(max = ClientDetails.MAX_NAME_LENGTH) String lastName,
        @NotBlank @Size(max = ClientDetails.MAX_PHONE_LENGTH) String phoneNumber,
        @Email @Size(max = ClientDetails.MAX_EMAIL_LENGTH) String email) {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_EMAIL_LENGTH = 100;
}
