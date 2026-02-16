import React, { useState, useEffect, useRef } from 'react';
import { SearchBox } from "./SearchBox.jsx";
import { Category } from "./Category.jsx";
import { Genre } from "./Genre.jsx";
import { Sort } from './Sort.jsx';

const controlBarStyle = {
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    background: 'rgb(0,0,0)',
    zIndex: 100,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)'
};

const AUTOPLAY_KEY = 'localmovies_autoplay_enabled';

export const getAutoplayEnabled = () => {
    const stored = localStorage.getItem(AUTOPLAY_KEY);
    return stored === null ? true : stored === 'true'; // Default to true
};

const SettingsDropdown = ({ isOpen, onClose, autoplayEnabled, setAutoplayEnabled }) => {
    const dropdownRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const toggleAutoplay = () => {
        const newValue = !autoplayEnabled;
        setAutoplayEnabled(newValue);
        localStorage.setItem(AUTOPLAY_KEY, String(newValue));
    };

    return (
        <div className="settings-dropdown" ref={dropdownRef}>
            <div className="settings-dropdown__header">Settings</div>
            <label className="settings-dropdown__item">
                <span className="settings-dropdown__label">Auto-play next episode</span>
                <button
                    className={`settings-dropdown__toggle ${autoplayEnabled ? 'settings-dropdown__toggle--on' : ''}`}
                    onClick={toggleAutoplay}
                    role="switch"
                    aria-checked={autoplayEnabled}
                >
                    <span className="settings-dropdown__toggle-knob" />
                </button>
            </label>
        </div>
    );
};

export const ControlBar = ({ filterMedia, selectGenre, selectSort, filterMediaNavigate, setType, onClearFilters, hasActiveFilters }) => {
    const [showSettings, setShowSettings] = useState(false);
    const [autoplayEnabled, setAutoplayEnabled] = useState(getAutoplayEnabled);

    return (
        <nav style={controlBarStyle} aria-label="Media filters and controls">
            <div className="control-bar">
                <SearchBox filterMedia={filterMedia} filterMediaNavigate={filterMediaNavigate}/>
                <div className="control-bar__filters">
                    <Category setType={setType}/>
                    <Genre selectGenre={selectGenre}/>
                    <Sort selectSort={selectSort}/>
                    {hasActiveFilters && (
                        <button
                            className="control-bar__clear-btn"
                            onClick={onClearFilters}
                            aria-label="Clear all filters"
                        >
                            Clear
                        </button>
                    )}
                    <div className="control-bar__settings-container">
                        <button
                            className="control-bar__settings-btn"
                            onClick={() => setShowSettings(!showSettings)}
                            aria-label="Settings"
                            aria-expanded={showSettings}
                        >
                            ⚙
                        </button>
                        <SettingsDropdown
                            isOpen={showSettings}
                            onClose={() => setShowSettings(false)}
                            autoplayEnabled={autoplayEnabled}
                            setAutoplayEnabled={setAutoplayEnabled}
                        />
                    </div>
                </div>
            </div>
        </nav>
    );
};
