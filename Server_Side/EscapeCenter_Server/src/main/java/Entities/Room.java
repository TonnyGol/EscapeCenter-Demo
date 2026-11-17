package Entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private String description;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT) // one query for the prices of all rooms, not one per room
    @CollectionTable(name = "room_prices", joinColumns = @JoinColumn(name = "room_id"))
    @OrderBy("fromParticipants")
    private List<PriceTier> prices = new ArrayList<>();

    protected Room() {
    }

    public List<PriceTier> getPrices() {
        return prices;
    }

    public String getDescription() {
        return description;
    }

    public int getMinParticipants() {
        return minParticipants;
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
