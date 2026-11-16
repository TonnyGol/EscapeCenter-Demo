package Requests;

import Entities.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull BookingStatus status) {
}
