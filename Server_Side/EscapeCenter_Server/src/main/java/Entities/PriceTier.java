package Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Price per participant for groups of at least {@code fromParticipants}; the discounted price needs an ID (soldier, student). */
@Embeddable
public record PriceTier(
        @Column(name = "from_participants") int fromParticipants,
        @Column(name = "price_per_participant") int pricePerParticipant,
        @Column(name = "discounted_price") int discountedPrice) {
}
