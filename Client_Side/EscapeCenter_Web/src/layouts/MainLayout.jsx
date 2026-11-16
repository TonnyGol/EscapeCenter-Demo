import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, X, Calendar, Home as HomeIcon, DoorOpen } from 'lucide-react';
import { VENUE } from '../roomsData';
import './MainLayout.css';

const MainLayout = ({ children }) => {
    const [mobileMenuOpen, setMobileMenuOpen] = React.useState(false);
    const location = useLocation();

    const navLinks = [
        { path: '/booking', label: 'הזמנות', icon: Calendar },
        { path: '/rooms', label: 'חדרי בריחה', icon: DoorOpen },
        { path: '/', label: 'דף הבית', icon: HomeIcon },
    ];


    const isActive = (path) => location.pathname === path;

    return (
        <div className="main-layout">
            {/* Navigation */}
            <nav className="navbar">
                <div className="container nav-container">
                    <Link to="/" className="logo">
                        <span className="logo-text">EscapeCenter</span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="nav-links desktop-nav">
                        {navLinks.map((link) => (
                            <Link
                                key={link.path}
                                to={link.path}
                                className={`nav-link ${isActive(link.path) ? 'active' : ''}`}
                            >
                                <link.icon size={18} />
                                <span>{link.label}</span>
                            </Link>
                        ))}
                    </div>

                    {/* Mobile Menu Button */}
                    <button
                        className="mobile-menu-btn"
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                        aria-label="פתיחה וסגירה של התפריט"
                    >
                        {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
                    </button>
                </div>

                {/* Mobile Navigation */}
                {mobileMenuOpen && (
                    <div className="mobile-nav">
                        {navLinks.map((link) => (
                            <Link
                                key={link.path}
                                to={link.path}
                                className={`mobile-nav-link ${isActive(link.path) ? 'active' : ''}`}
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                <link.icon size={20} />
                                <span>{link.label}</span>
                            </Link>
                        ))}
                    </div>
                )}
            </nav>

            {/* Main Content */}
            <main className="main-content">{children}</main>

            {/* Footer */}
            <footer className="footer">
                <div className="container">
                    <div className="footer-content">
                        <div className="footer-section">
                            <h3 className="footer-title">EscapeCenter</h3>
                            <p className="footer-text">מתחם חדרי הבריחה בראשון לציון</p>
                        </div>

                        <div className="footer-section">
                            <h4 className="footer-subtitle">קישורים</h4>
                            <div className="footer-links">
                                {navLinks.map((link) => (
                                    <Link key={link.path} to={link.path} className="footer-link">
                                        {link.label}
                                    </Link>
                                ))}
                            </div>
                        </div>

                        <div className="footer-section">
                            <h4 className="footer-subtitle">צור קשר</h4>
                            <p className="footer-text">{VENUE.address}</p>
                            <p className="footer-text">טלפון: <span dir="ltr">{VENUE.phone}</span></p>
                            <p className="footer-text"><span dir="ltr">{VENUE.email}</span></p>
                            {VENUE.hours.map(slot => (
                                <p key={slot.days} className="footer-text">{slot.days}: <span dir="ltr">{slot.time}</span></p>
                            ))}
                        </div>
                    </div>

                    <div className="footer-bottom">
                        <p>&copy; {new Date().getFullYear()} EscapeCenter. כל הזכויות שמורות.</p>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default MainLayout;
