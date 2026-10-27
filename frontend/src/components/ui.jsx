import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

export function Button({ variant = 'primary-sm', className = '', children, ...props }) {
  const baseClass = {
    primary: 'button-primary',
    secondary: 'button-secondary',
    'primary-sm': 'button-primary-sm',
    'ghost-sm': 'button-ghost-sm',
    danger: 'button-danger'
  }[variant] || 'button-primary-sm';
  
  return (
    <button className={`${baseClass} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Input({ className = '', ...props }) {
  return (
    <input className={`text-input ${className}`} {...props} />
  );
}

export function Select({ className = '', children, ...props }) {
  return (
    <select className={`select-input ${className}`} {...props}>
      {children}
    </select>
  );
}

export function Card({ elevated, className = '', children, ...props }) {
  const baseClass = elevated ? 'feature-card-elevated' : 'feature-card';
  return (
    <div className={`${baseClass} ${className}`} {...props}>
      {children}
    </div>
  );
}

export function Modal({ isOpen, onClose, title, children }) {
  if (!isOpen) return null;
  
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="heading-md">{title}</h2>
        </div>
        <div>
          {children}
        </div>
      </div>
    </div>
  );
}

export function NavBar() {
  const location = useLocation();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  
  return (
    <nav className="nav-bar">
      <div className="flex items-center justify-between" style={{ width: '100%' }}>
        <Link to="/" className="flex items-center gap-xs" style={{ textDecoration: 'none' }}>
          <img src="/neptune_logo.jpg" alt="release-tracker Logo" style={{ height: '32px', width: '32px', borderRadius: 'var(--rounded-sm)' }} />
          <span className="heading-md" style={{ letterSpacing: '-0.8px' }}>Release Tracker</span>
        </Link>
        <button 
          className="menu-toggle" 
          onClick={() => setIsMenuOpen(!isMenuOpen)}
          aria-label="Toggle Menu"
        >
          ☰
        </button>
      </div>
      <div className={`nav-links-container ${isMenuOpen ? 'open' : ''}`}>
        <Link to="/" className={`nav-link ${location.pathname === '/' ? 'active' : ''}`} onClick={() => setIsMenuOpen(false)}>Dashboard</Link>
        <Link to="/repos" className={`nav-link ${location.pathname === '/repos' ? 'active' : ''}`} onClick={() => setIsMenuOpen(false)}>Repositories</Link>
        <Link to="/templates" className={`nav-link ${location.pathname === '/templates' ? 'active' : ''}`} onClick={() => setIsMenuOpen(false)}>Templates</Link>
        <Link to="/settings" className={`nav-link ${location.pathname === '/settings' ? 'active' : ''}`} onClick={() => setIsMenuOpen(false)}>Settings</Link>
      </div>
    </nav>
  );
}

export function Hero() {
  return (
    <div className="hero-band flex-col items-center justify-center">
      <h1 className="display-xl mb-md">Release Tracking, Simplified</h1>
      <p className="body-lg" style={{ maxWidth: '600px', margin: '0 auto', color: 'var(--colors-body)' }}>
        Track GitHub releases and Docker images, and automatically create Jira tickets using custom templates.
      </p>
      <div className="hero-buttons">
        <Link to="/repos" className="button-primary">View Repositories</Link>
        <Link to="/templates" className="button-secondary">Manage Templates</Link>
      </div>
    </div>
  );
}

export function Footer() {
  return (
    <footer className="footer">
      <div className="container flex justify-between items-center">
        <div className="flex items-center gap-xs text-mute">
          <span className="mono-eyebrow">release-tracker</span>
        </div>
        <div className="flex gap-md body-sm text-mute">
          <a href="https://github.com/felix-moser/Release-Tracker" target="_blank" rel="noopener noreferrer" style={{ textDecoration: 'none', color: 'inherit' }}>GitHub</a>
        </div>
      </div>
    </footer>
  );
}
