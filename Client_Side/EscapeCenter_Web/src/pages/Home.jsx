import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, Clock, Users, Star, Trophy } from 'lucide-react';
import './Home.css';

const Home = () => {
    const featuredRooms = [
        {
            id: 'achuzat-hashachen',
            name: 'אחוזת השכן',
            difficulty: 4,
            participants: '2-6',
            duration: '90 דקות',
            image: '/placeholder-room1.jpg',
            description: 'חדר מסתורין מותח עצבים',
        },
        {
            id: 'mikdash-hakami',
            name: 'מקדש הקאמי',
            difficulty: 5,
            participants: '2-8',
            duration: '90 דקות',
            image: '/placeholder-room2.jpg',
            description: 'הרפתקה יפנית מסוכנת',
        },
        {
            id: 'hahitarvut',
            name: 'ההתערבות',
            difficulty: 3,
            participants: '2-6',
            duration: '90 דקות',
            image: '/placeholder-room3.jpg',
            description: 'מרדף אחר האמת',
        },
    ];

    const stats = [
        { icon: Trophy, label: 'אחוזי הצלחה', value: '45%' },
        { icon: Users, label: 'משתתפים', value: '10,000+' },
        { icon: Star, label: 'דירוג ממוצע', value: '4.9/5' },
    ];

    return (
        <div className="home-page">
            {/* Hero Section */}
            <section className="hero-section">
                <div className="hero-overlay" />
                <div className="container hero-content">
                    <motion.h1
                        className="hero-title heading-1"
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.8 }}
                    >
                        ברוכים הבאים ל-EscapeCenter
                    </motion.h1>
                    <motion.p
                        className="hero-subtitle"
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.8, delay: 0.2 }}
                    >
                        חווית בריחה בלתי נשכחת מחכה לכם
                    </motion.p>
                    <motion.div
                        className="hero-actions"
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.8, delay: 0.4 }}
                    >
                        <Link to="/booking" className="btn btn-primary">
                            הזמינו עכשיו
                            <ArrowRight size={20} />
                        </Link>
                        <Link to="/rooms" className="btn btn-secondary">
                            צפו בחדרים
                        </Link>
                    </motion.div>
                </div>
            </section>

            {/* Stats Section */}
            <section className="stats-section section">
                <div className="container">
                    <div className="stats-grid">
                        {stats.map((stat, index) => (
                            <motion.div
                                key={index}
                                className="stat-card"
                                initial={{ opacity: 0, y: 20 }}
                                whileInView={{ opacity: 1, y: 0 }}
                                viewport={{ once: true }}
                                transition={{ delay: index * 0.1 }}
                            >
                                <stat.icon size={32} className="stat-icon" />
                                <div className="stat-value">{stat.value}</div>
                                <div className="stat-label">{stat.label}</div>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Featured Rooms Section */}
            <section className="featured-rooms section">
                <div className="container">
                    <div className="section-header">
                        <h2 className="heading-2">החדרים שלנו</h2>
                        <p className="section-description text-muted">
                            בחרו את ההרפתקה הבאה שלכם
                        </p>
                    </div>

                    <div className="rooms-grid">
                        {featuredRooms.map((room, index) => (
                            <motion.div
                                key={room.id}
                                className="room-card card"
                                initial={{ opacity: 0, y: 30 }}
                                whileInView={{ opacity: 1, y: 0 }}
                                viewport={{ once: true }}
                                transition={{ delay: index * 0.15 }}
                            >
                                <div className="room-image-wrapper">
                                    <div className="room-image-placeholder">
                                        <Clock size={48} />
                                    </div>
                                    <div className="room-difficulty">
                                        {'⭐'.repeat(room.difficulty)}
                                    </div>
                                </div>

                                <div className="room-content">
                                    <h3 className="room-name heading-4">{room.name}</h3>
                                    <p className="room-description text-muted">{room.description}</p>

                                    <div className="room-meta">
                                        <div className="room-meta-item">
                                            <Users size={16} />
                                            <span>{room.participants}</span>
                                        </div>
                                        <div className="room-meta-item">
                                            <Clock size={16} />
                                            <span>{room.duration}</span>
                                        </div>
                                    </div>

                                    <Link to={`/rooms/${room.id}`} className="btn btn-primary room-btn">
                                        פרטים נוספים
                                        <ArrowRight size={18} />
                                    </Link>
                                </div>
                            </motion.div>
                        ))}
                    </div>

                    <div className="section-cta">
                        <Link to="/rooms" className="btn btn-secondary">
                            צפייה בכל החדרים
                        </Link>
                    </div>
                </div>
            </section>

            {/* Why Us Section */}
            <section className="why-us section">
                <div className="container">
                    <h2 className="heading-2 text-center">למה EscapeCenter?</h2>
                    <div className="features-grid">
                        {[
                            {
                                title: 'חדרים ייחודיים',
                                description: 'עיצובים מקוריים ועלילות מרתקות',
                            },
                            {
                                title: 'טכנולוגיה מתקדמת',
                                description: 'מערכות חכמות וחוויה אינטראקטיבית',
                            },
                            {
                                title: 'שירות מעולה',
                                description: 'צוות מקצועי ותמיכה מלאה',
                            },
                        ].map((feature, index) => (
                            <motion.div
                                key={index}
                                className="feature-card card"
                                initial={{ opacity: 0, scale: 0.9 }}
                                whileInView={{ opacity: 1, scale: 1 }}
                                viewport={{ once: true }}
                                transition={{ delay: index * 0.1 }}
                            >
                                <h3 className="heading-4">{feature.title}</h3>
                                <p className="text-muted">{feature.description}</p>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>
        </div>
    );
};

export default Home;
