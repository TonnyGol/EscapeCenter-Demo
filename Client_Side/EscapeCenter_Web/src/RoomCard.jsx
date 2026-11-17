import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Clock, Trophy, Users } from 'lucide-react';
import { RoomIcon } from './roomVisuals';
import { ROOM_THEMES } from './roomThemes';
import { playersLabel } from './useApiRooms';
import './RoomCard.css';

/** One room as a poster card. The whole card is the link; data-room gives it the room's accent colour.
 * apiRoom carries the facts the database owns (group size, game length); the card shows them once they arrive. */
const RoomCard = ({ room, apiRoom }) => {
    const theme = ROOM_THEMES[room.name];
    return (
        <li data-room={theme.key}>
            <Link to={`/rooms/${room.id}`} className="room-card">
                <span className="room-card-poster">
                    <span className="room-card-stamp">{theme.stamp}</span>
                    <RoomIcon name={room.name} size={52} strokeWidth={1.5} />
                </span>
                <span className="room-card-body">
                    <span className="room-card-name heading-4">{room.name}</span>
                    <span className="room-card-teaser">{room.teaser}</span>
                    {apiRoom && (
                        <span className="room-card-meta">
                            <span><Users size={16} aria-hidden="true" />{playersLabel(apiRoom)} משתתפים</span>
                            <span><Clock size={16} aria-hidden="true" />{apiRoom.durationMinutes} דקות</span>
                        </span>
                    )}
                    {room.award && (
                        <span className="room-card-award"><Trophy size={16} aria-hidden="true" />{room.award}</span>
                    )}
                    <span className="room-card-more">
                        פרטים ומחירון
                        <ArrowLeft size={16} aria-hidden="true" />
                    </span>
                </span>
            </Link>
        </li>
    );
};

export default RoomCard;
