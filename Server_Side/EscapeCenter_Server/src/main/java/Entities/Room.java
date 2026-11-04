package Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "first_game_start", nullable = false)
    private LocalTime firstGameStart;

    @Column(name = "games_per_day", nullable = false)
    private int gamesPerDay;

    @Column(name = "break_minutes", nullable = false)
    private int breakMinutes;

    protected Room() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public LocalTime getFirstGameStart() {
        return firstGameStart;
    }

    public int getGamesPerDay() {
        return gamesPerDay;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }
}
