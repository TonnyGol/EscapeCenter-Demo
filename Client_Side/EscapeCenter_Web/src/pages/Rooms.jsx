import React from 'react';
import RoomCard from '../RoomCard';
import { ROOMS } from '../roomsData';
import { useApiRooms } from '../useApiRooms';
import './Rooms.css';

const Rooms = () => {
    const apiRooms = useApiRooms();

    return (
    <div className="rooms-page">
        <div className="container">
            <header className="rooms-header">
                <h1 className="heading-1">חדרי הבריחה שלנו</h1>
                <p className="text-muted">כל חדר עם עלילה, אורך משחק ומחירון משלו. בחרו חדר כדי לראות פרטים.</p>
            </header>
            <ul className="room-grid">
                {ROOMS.map(room => (
                    <RoomCard key={room.id} room={room} apiRoom={apiRooms?.find(candidate => candidate.name === room.name)} />
                ))}
            </ul>
        </div>
    </div>
    );
};

export default Rooms;
