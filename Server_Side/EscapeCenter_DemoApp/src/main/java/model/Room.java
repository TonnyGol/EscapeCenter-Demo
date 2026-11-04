package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** An escape room as the server describes it. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Room(Long id, String name, int capacity, int durationMinutes) {
}
