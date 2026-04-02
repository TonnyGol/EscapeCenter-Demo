import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Clock, Users, Star } from 'lucide-react';
import './Rooms.css';

const Rooms = () => {
    const allRooms = [
        {
            id: 'achuzat-hashachen',
            name: 'אחוזת השכן',
            difficulty: 4,
            participants: '2-6',
            duration: '90 דקות',
            fearLevel: 'גבוה',
            successRate: '42%',
            description: 'חדר מסתורין מותח עצבים עם עלילה מרתקת',
        },
        {
            id: 'mikdash-hakami',
            name: 'מקדש הקאמי',
            difficulty: 5,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'גבוה מאוד',
            successRate: '38%',
            description: 'הרפתקה יפנית מסוכנת במקדש עתיק',
        },
        {
            id: 'hahitarvut',
            name: 'ההתערבות',
            difficulty: 3,
            participants: '2-6',
            duration: '90 דקות',
            fearLevel: 'בינוני',
            successRate: '55%',
            description: 'מרדף אחר האמת במשרד חקירות סודי',
        },
        {
            id: 'infinity',
            name: 'אינפיניטי',
            difficulty: 4,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'בינוני',
            successRate: '48%',
            description: 'מסע בזמן וחלל למציאות אחרת',
        },
        {
            id: 'narcos',
            name: 'נרקוס',
            difficulty: 5,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'גבוה',
            successRate: '35%',
            description: 'משימה מסוכנת בעולם הפשע המאורגן',
        },
    ];

    return (
        <div className="rooms-page">
            <div className="container">
                {/* Page Header */}
                <motion.div
                    className="page-header"
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                >
                    <h1 className="heading-1">חדרי הבריחה שלנו</h1>
                    <p className="page-subtitle text-muted">
                        בחרו את החדר המושלם עבורכם והצטרפו להרפתקה
                    </p>
                </motion.div>

                {/* Rooms Grid */}
                <div className="all-rooms-grid">
                    {allRooms.map((room, index) => (
                        <motion.div
                            key={room.id}
                            className="room-card-large card"
                            initial={{ opacity: 0, y: 30 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: index * 0.1 }}
                        >
                            <div className="room-image-wrapper">
                                <div className="room-image-placeholder">
                                    <Clock size={48} />
                                </div>
                                <div className="room-badges">
                                    <span className="badge badge-difficulty">
                                        {'⭐'.repeat(room.difficulty)}
                                    </span>
                                    <span className="badge badge-fear">{room.fearLevel}</span>
                                </div>
                            </div>

                            <div className="room-content">
                                <h2 className="room-name heading-3">{room.name}</h2>
                                <p className="room-description text-muted">{room.description}</p>

                                <div className="room-stats">
                                    <div className="room-stat">
                                        <Users size={18} />
                                        <span>{room.participants} משתתפים</span>
                                    </div>
                                    <div className="room-stat">
                                        <Clock size={18} />
                                        <span>{room.duration}</span>
                                    </div>
                                    <div className="room-stat">
                                        <Star size={18} />
                                        <span>הצלחה: {room.successRate}</span>
                                    </div>
                                </div>

                                <div className="room-actions">
                                    <Link to={`/rooms/${room.id}`} className="btn btn-primary">
                                        פרטים נוספים
                                    </Link>
                                    <Link to="/booking" className="btn btn-secondary">
                                        הזמינו עכשיו
                                    </Link>
                                </div>
                            </div>
                        </motion.div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Rooms;
