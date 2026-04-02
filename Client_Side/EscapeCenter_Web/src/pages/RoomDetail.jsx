import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Clock, Users, Star, ArrowRight, Trophy, AlertCircle } from 'lucide-react';
import './RoomDetail.css';

const RoomDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    // Room data - in real app this would come from API/context
    const roomsData = {
        'achuzat-hashachen': {
            name: 'אחוזת השכן',
            difficulty: 4,
            participants: '2-6',
            duration: '90 דקות',
            fearLevel: 'גבוה',
            successRate: '42%',
            description: 'חדר מסתורין מותח עצבים עם עלילה מרתקת',
            fullStory: `אתם מקבלים הזמנה מסתורית לאחוזה נטושה. השכן הידוע לשמצה נעלם לפני 20 שנה, והותיר אחריו סודות אפלים. 
      עכשיו, אתם צריכים לפענח את התעלומה ולגלות מה באמת קרה באותו לילה גורלי.
      האם תצליחו לפתור את החידות ולברוח לפני שיהיה מאוחר מדי?`,
            features: [
                'חדר מתקדם עם אפקטים מיוחדים',
                'עלילה מרתקת ומסתורין עמוק',
                'חידות מאתגרות ומגוונות',
                'מתאים למשחקים מנוסים',
            ],
        },
        'mikdash-hakami': {
            name: 'מקדש הקאמי',
            difficulty: 5,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'גבוה מאוד',
            successRate: '38%',
            description: 'הרפתקה יפנית מסוכנת במקדש עתיק',
            fullStory: `מקדש יפני עתיק, נטוש זה מאות שנים. אגדות מספרות על רוח הקאמי ששומרת על המקום. 
      אתם, חוקרי העתיקות, נכנסים למקדש בחיפוש אחר אוצר אבוד. אבל המקדש מלא במלכודות ומסתורין.
      האם תצליחו להעיר את רוח הקאמי ולברוח בשלום?`,
            features: [
                'החדר המאתגר ביותר שלנו',
                'עיצוב יפני אותנטי',
                'אווירה מפחידה ומותחת',
                'מומלץ לשחקנים מנוסים בלבד',
            ],
        },
        'hahitarvut': {
            name: 'ההתערבות',
            difficulty: 3,
            participants: '2-6',
            duration: '90 דקות',
            fearLevel: 'בינוני',
            successRate: '55%',
            description: 'מרדף אחר האמת במשרד חקירות סודי',
            fullStory: `אתם חוקרי FBI שנקלעים למזימה מסתורית. המשרד נסגר בפניכם והמידע הסודי נעלם. 
      יש לכם 90 דקות למצוא את הראיות, לפענח את הקוד ולחשוף את האמת.
      האם תצליחו לעצור את המזימה בזמן?`,
            features: [
                'חדר בינוני - מתאים למתחילים',
                'עלילת מתח מרתקת',
                'חידות לוגיות ומאתגרות',
                'מומלץ לקבוצות משפחתיות',
            ],
        },
        'infinity': {
            name: 'אינפיניטי',
            difficulty: 4,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'בינוני',
            successRate: '48%',
            description: 'מסע בזמן וחלל למציאות אחרת',
            fullStory: `ניסוי מדעי משתבש ואתם נלכדים בלולאת זמן אינסופית. 
      כל 90 דקות, הזמן מתאפס והכל מתחיל מחדש. האם תצליחו לשבור את המעגל?
      חוויה ייחודית שמשלבת מדע בדיוני עם חידות מאתגרות.`,
            features: [
                'קונצפט ייחודי ומקורי',
                'טכנולוגיה מתקדמת',
                'חידות יצירתיות',
                'מתאים לקבוצות גדולות',
            ],
        },
        'narcos': {
            name: 'נרקוס',
            difficulty: 5,
            participants: '2-8',
            duration: '90 דקות',
            fearLevel: 'גבוה',
            successRate: '35%',
            description: 'משימה מסוכנת בעולם הפשע המאורגן',
            fullStory: `אתם סוכנים חשאיים שחודרים למעבדת סמים של קרטל מסוכן. 
      המשימה: למצוא ראיות ולברוח לפני שהשומרים יגלו אתכם.
      הזמן עובד נגדכם והסכנה ממשית. האם יש לכם את האומץ?`,
            features: [
                'חדר אקשן מותח',
                'אווירה של סכנה אמיתית',
                'חידות מורכבות',
                'למשחקנים עם עצבים חזקים',
            ],
        },
    };

    const room = roomsData[id];

    if (!room) {
        return (
            <div className="container" style={{ padding: '100px 20px', textAlign: 'center' }}>
                <h1>חדר לא נמצא</h1>
                <Link to="/rooms" className="btn btn-primary" style={{ marginTop: '20px' }}>
                    חזרה לרשימת החדרים
                </Link>
            </div>
        );
    }

    return (
        <div className="room-detail-page">
            <div className="container">
                {/* Hero Section */}
                <motion.div
                    className="room-hero"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                >
                    <div className="room-hero-image">
                        <div className="room-image-placeholder">
                            <Clock size={64} />
                        </div>
                        <div className="room-hero-badges">
                            <span className="badge badge-difficulty">
                                {'⭐'.repeat(room.difficulty)}
                            </span>
                            <span className="badge badge-fear">{room.fearLevel}</span>
                        </div>
                    </div>
                </motion.div>

                {/* Content */}
                <div className="room-detail-content">
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.2 }}
                    >
                        <h1 className="heading-1 room-title">{room.name}</h1>
                        <p className="room-tagline text-lg text-muted">{room.description}</p>

                        {/* Stats Grid */}
                        <div className="room-stats-grid">
                            <div className="stat-box">
                                <Users size={24} />
                                <div>
                                    <div className="stat-label">משתתפים</div>
                                    <div className="stat-value">{room.participants}</div>
                                </div>
                            </div>
                            <div className="stat-box">
                                <Clock size={24} />
                                <div>
                                    <div className="stat-label">זמן משחק</div>
                                    <div className="stat-value">{room.duration}</div>
                                </div>
                            </div>
                            <div className="stat-box">
                                <Trophy size={24} />
                                <div>
                                    <div className="stat-label">אחוז הצלחה</div>
                                    <div className="stat-value">{room.successRate}</div>
                                </div>
                            </div>
                            <div className="stat-box">
                                <Star size={24} />
                                <div>
                                    <div className="stat-label">רמת קושי</div>
                                    <div className="stat-value">{room.difficulty}/5</div>
                                </div>
                            </div>
                        </div>

                        {/* Story Section */}
                        <div className="room-section">
                            <h2 className="heading-3">הסיפור</h2>
                            <p className="room-story">{room.fullStory}</p>
                        </div>

                        {/* Features Section */}
                        <div className="room-section">
                            <h2 className="heading-3">מה כולל החדר?</h2>
                            <ul className="features-list">
                                {room.features.map((feature, index) => (
                                    <li key={index} className="feature-item">
                                        <AlertCircle size={20} />
                                        <span>{feature}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>

                        {/* CTA Section */}
                        <div className="room-cta">
                            <button
                                onClick={() => navigate('/booking')}
                                className="btn btn-primary btn-large"
                            >
                                הזמינו עכשיו
                                <ArrowRight size={20} />
                            </button>
                            <Link to="/rooms" className="btn btn-ghost">
                                חזרה לכל החדרים
                            </Link>
                        </div>
                    </motion.div>
                </div>
            </div>
        </div>
    );
};

export default RoomDetail;
