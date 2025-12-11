import React, { useEffect, useRef, useState } from 'react';

const searchBoxStyle = {
    color: 'white',
    fontSize: 14,
    margin: 'auto',
    textAlign: 'center',
    paddingTop: 10
};

export const SearchBox = ({ filterMedia, filterMediaNavigate }) => {
    const [searchValue, setSearchValue] = useState('');
    const debounceTimerRef = useRef(null);

    useEffect(() => {
        // Clear previous timer
        if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
        }

        // Set new timer to trigger search after 300ms of inactivity
        debounceTimerRef.current = setTimeout(() => {
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
            filterMediaNavigate(searchValue);
        }
    };

    return (
        <p style={searchBoxStyle}>
            <label htmlFor="media-search">Search</label><br/>
            <input
                id="media-search"
                value={searchValue}
                onChange={handleChange}
                onKeyDown={handleKeyDown}
                type='text'
                aria-label="Search for movies and TV shows"
                placeholder="Search..."
            />
        </p>
    );
};