import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Clock, TriangleAlert, Trophy, Users } from 'lucide-react';
import PageTitle from '../PageTitle';
import NotFound from './NotFound';
import { RoomIcon } from '../roomVisuals';
import { ROOM_THEMES } from '../roomThemes';
import { playersLabel, useApiRooms } from '../useApiRooms';
import { HEALTH_WARNING, PRICE_NOTES, VENUE, roomById } from '../roomsData';
import './RoomDetail.css';

const NOT_FOUND_TITLE = 'חדר לא נמצא';
const BACK_TO_ROOMS_LABEL = 'חזרה לרשימת החדרים';
const PRICE_UNIT = '₪ למשתתף';
const PRICES_LOADING_MESSAGE = 'טוענים את המחירון…';
const PRICES_FAILED_MESSAGE = 'לא הצלחנו לטעון את המחירון. אפשר לברר במחירים בטלפון:';

// The last tier covers every larger group, the others are one group size each.
const groupLabel = (tier, index, tiers) => (index === tiers.length - 1 && tiers.length > 1 ? `${tier.fromParticipants} ומעלה` : String(tier.fromParticipants));

const RoomDetail = () => {
    const room = roomById(useParams().id);
    const apiRooms = useApiRooms();
    const apiRoom = apiRooms?.find(candidate => candidate.name === room?.name);
    const prices = apiRoom?.prices ?? [];

    if (!room) {
        return <NotFound title={NOT_FOUND_TITLE} backTo="/rooms" backLabel={BACK_TO_ROOMS_LABEL} />;
    }

    const theme = ROOM_THEMES[room.name];
    const notes = [room.mood, room.physical, room.challenge, room.pairNote, room.teamNote].filter(Boolean);

    return (
        <div className="room-detail-page" data-room={theme.key}>
            <PageTitle>{room.name}</PageTitle>
            <div className="container">
                <header className="room-detail-header">
                    <div className="room-detail-poster" aria-hidden="true">
                        <span className="room-detail-stamp">{theme.stamp}</span>
                        <RoomIcon name={room.name} size={72} strokeWidth={1.5} />
                    </div>
                    <div>
                        <h1 className="heading-1">{room.name}</h1>
                        {room.award && <p className="room-detail-award"><Trophy size={18} aria-hidden="true" />{room.award}</p>}
                        <dl className="room-detail-facts">
                            {apiRoom && (
                                <>
                                    <div><dt><Users size={18} aria-hidden="true" />משתתפים</dt><dd>{playersLabel(apiRoom)}</dd></div>
                                    <div><dt><Clock size={18} aria-hidden="true" />משך המשחק</dt><dd>{apiRoom.durationMinutes} דקות</dd></div>
                                </>
                            )}
                            <div><dt>גיל מינימלי ללא מבוגר</dt><dd>{room.minAgeAlone}</dd></div>
                            <div><dt>מומלץ מגיל, בליווי מבוגרים</dt><dd>{room.minAgeWithAdults}</dd></div>
                        </dl>
                        <Link to={`/booking?room=${encodeURIComponent(room.name)}`} className="btn btn-primary">
                            הזמינו את החדר
                            <ArrowLeft size={20} aria-hidden="true" />
                        </Link>
                    </div>
                </header>

                <section className="room-detail-section" aria-labelledby="story-title">
                    <h2 id="story-title" className="heading-3">סיפור החדר</h2>
                    <p className="room-detail-story">{room.story}</p>
                    <ul className="room-detail-notes">
                        {notes.map(note => <li key={note}>{note}</li>)}
                    </ul>
                    <p className="room-detail-warning">
                        <TriangleAlert size={20} aria-hidden="true" />
                        {HEALTH_WARNING} טלפון: {VENUE.phone}
                    </p>
                </section>

                <section className="room-detail-section" aria-labelledby="prices-title">
                    <h2 id="prices-title" className="heading-3">מחירון</h2>
                    {apiRooms === null && <p className="text-muted" role="status">{PRICES_LOADING_MESSAGE}</p>}
                    {apiRooms !== null && prices.length === 0 && (
                        <p className="text-muted" role="alert">{PRICES_FAILED_MESSAGE} <span dir="ltr">{VENUE.phone}</span></p>
                    )}
                    {prices.length > 0 && (
                        <div className="room-detail-table-wrap">
                            <table className="room-detail-table">
                                <thead>
                                    <tr>
                                        <th scope="col">משתתפים</th>
                                        <th scope="col">מחיר רגיל</th>
                                        <th scope="col">חיילים, סטודנטים ושירות לאומי</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {prices.map((tier, index) => (
                                        <tr key={tier.fromParticipants}>
                                            <th scope="row">{groupLabel(tier, index, prices)}</th>
                                            <td>{tier.pricePerParticipant} {PRICE_UNIT}</td>
                                            <td>{tier.discountedPrice} {PRICE_UNIT}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                    <ul className="room-detail-notes">
                        {PRICE_NOTES.map(note => <li key={note}>{note}</li>)}
                    </ul>
                </section>
            </div>
        </div>
    );
};

export default RoomDetail;
