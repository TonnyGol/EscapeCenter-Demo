import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft } from 'lucide-react';
import RoomCard from '../RoomCard';
import PageTitle from '../PageTitle';
import { ROOMS, VENUE } from '../roomsData';
import { useApiRooms } from '../useApiRooms';
import './Home.css';

const HERO_TEXT = 'חדרי בריחה בראשון לציון, ובכל חדר תיק פתוח. בחרו חדר, תאריך ושעה, והאישור מגיע למייל.';

/** The facts that come from the database: how many rooms and how long a game is. Nothing until the rooms arrive. */
const heroFacts = apiRooms => {
    if (!apiRooms || apiRooms.length === 0) return [];
    const minutes = apiRooms.map(room => room.durationMinutes);
    const minutesLabel = Math.min(...minutes) === Math.max(...minutes) ? String(minutes[0]) : `${Math.min(...minutes)}-${Math.max(...minutes)}`;
    return [
        { value: String(apiRooms.length), label: 'חדרי בריחה' },
        { value: minutesLabel, label: 'דקות למשחק' },
        { value: String(VENUE.worldRank), label: 'בדירוג מתחמי הבריחה בעולם' },
    ];
};

const STEPS = [
    { title: 'בחרו חדר', text: 'כל חדר עם עלילה, אורך משחק ומחירון משלו.' },
    { title: 'בחרו תאריך ושעה', text: 'רואים מיד אילו שעות פנויות, עד שלושה חודשים קדימה.' },
    { title: 'מלאו פרטים', text: 'שם וטלפון מספיקים. אישור ההזמנה נשלח למייל.' },
];

const Home = () => {
    const apiRooms = useApiRooms();
    const facts = heroFacts(apiRooms);

    return (
    <div className="home-page">
        <PageTitle />
        <section className="home-hero">
            <div className="container home-hero-grid">
                <div className="home-hero-copy">
                    <motion.h1
                        className="heading-1"
                        initial={{ opacity: 0, y: 24 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.7 }}
                    >
                        יש לכם שעה.
                        <br />
                        הדלת נעולה.
                    </motion.h1>
                    <motion.div
                        initial={{ opacity: 0, y: 24 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.7, delay: 0.15 }}
                    >
                        <p className="home-hero-text">{HERO_TEXT}</p>
                        <div className="home-hero-actions">
                            <Link to="/booking" className="btn btn-primary">
                                הזמינו משחק
                                <ArrowLeft size={20} aria-hidden="true" />
                            </Link>
                            <Link to="/rooms" className="btn btn-secondary">
                                צפו בחדרים
                            </Link>
                        </div>
                    </motion.div>
                </div>

                <div className="home-case-file" aria-hidden="true">
                    <span className="home-stamp">סודי</span>
                    <span className="home-clock">60:00</span>
                    <span className="home-clock-caption">הזמן מתחיל כשהדלת ננעלת</span>
                </div>
            </div>

            <dl className="container home-facts">
                {facts.map(fact => (
                    <div key={fact.label} className="home-fact">
                        <dt>{fact.label}</dt>
                        <dd>{fact.value}</dd>
                    </div>
                ))}
            </dl>
        </section>

        <section className="home-section" aria-labelledby="home-rooms-title">
            <div className="container">
                <h2 id="home-rooms-title" className="heading-2">החדרים שלנו</h2>
                <ul className="room-grid">
                    {ROOMS.map(room => (
                        <RoomCard key={room.id} room={room} apiRoom={apiRooms?.find(candidate => candidate.name === room.name)} />
                    ))}
                </ul>
            </div>
        </section>

        <section className="home-section" aria-labelledby="home-steps-title">
            <div className="container">
                <h2 id="home-steps-title" className="heading-2">איך מזמינים</h2>
                <ol className="home-steps">
                    {STEPS.map(step => (
                        <li key={step.title} className="home-step">
                            <h3 className="heading-4">{step.title}</h3>
                            <p className="text-muted">{step.text}</p>
                        </li>
                    ))}
                </ol>
                <div className="home-final-cta">
                    <Link to="/booking" className="btn btn-primary">
                        הזמינו משחק
                        <ArrowLeft size={20} aria-hidden="true" />
                    </Link>
                </div>
            </div>
        </section>
    </div>
    );
};

export default Home;
