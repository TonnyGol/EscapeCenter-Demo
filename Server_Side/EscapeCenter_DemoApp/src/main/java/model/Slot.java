package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One game start time of a room on a day, as the availability endpoint lists it. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Slot(String startAt, String endAt, boolean available) {
}
