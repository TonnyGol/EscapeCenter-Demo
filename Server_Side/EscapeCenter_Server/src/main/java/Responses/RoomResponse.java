package Responses;

import Entities.PriceTier;
import Entities.Room;

import java.util.List;

public record RoomResponse(Long id, String name, int capacity, int minParticipants, int durationMinutes,
                           String description, List<PriceTier> prices) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getMinParticipants(),
                room.getDurationMinutes(), room.getDescription(), List.copyOf(room.getPrices()));
    }
}
