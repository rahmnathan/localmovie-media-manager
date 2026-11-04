import React, { useEffect, useRef, useState } from 'react';

export const SearchBox = ({ filterMedia, filterMediaNavigate, initialValue = '' }) => {
    const [searchValue, setSearchValue] = useState(initialValue);
    const debounceTimerRef = useRef(null);
    const inputRef = useRef(null);
    const lastNavigatedValue = useRef(initialValue);

    // Sync with URL params when they change externally (e.g., browser refresh, back button, clear filters)
    // Only sync if the input is not focused (user is not actively typing)
    useEffect(() => {
        const inputIsFocused = document.activeElement === inputRef.current;
        if (!inputIsFocused && initialValue !== searchValue) {
            setSearchValue(initialValue);
            lastNavigatedValue.current = initialValue;
        }
    }, [initialValue]);

    useEffect(() => {
        // Skip if we already navigated to this value
        if (searchValue === lastNavigatedValue.current) {
            return;
        }

        // Clear previous timer
        if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
        }

        // Set new timer to trigger search after 300ms of inactivity
        debounceTimerRef.current = setTimeout(() => {
            lastNavigatedValue.current = searchValue;
            filterMediaNavigate(searchValue);
        }, 300);

        // Cleanup on unmount
        return () => {
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
        };
    }, [searchValue, filterMediaNavigate]);

    const handleChange = (event) => {
        const value = event.target.value;
        setSearchValue(value);
        filterMedia(value);
    };

    const handleKeyDown = (event) => {
        if (event.key === 'Enter') {
            // Immediate search on Enter
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
            lastNavigatedValue.current = searchValue;
            filterMediaNavigate(searchValue);
        }
    };

    return (
        <div className="control-bar__search">
            <input
                ref={inputRef}
                id="media-search"
                value={searchValue}
                onChange={handleChange}
                onKeyDown={handleKeyDown}
                type='text'
                aria-label="Search for movies and TV shows"
                placeholder="Search for movies and TV shows..."
            />
        </div>
    );
};