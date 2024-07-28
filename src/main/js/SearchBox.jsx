import React from 'react';

const searchBoxStyle = {
    color: 'white',
    fontSize: 14,
    margin: 'auto',
    textAlign: 'center',
    paddingTop: 10
};

export const SearchBox = ({ filterMedia, filterMediaNavigate }) => {
    const handleKeyDown = (event) => {
        if (event.key === 'Enter') {
            filterMediaNavigate(event.target.value)
        } else {
            console.log('key press ' + event.key);
        }
    }

    return (
        <p style={searchBoxStyle}>Search<br/>
            <input
                onChange={(e) => filterMedia(e.target.value)}
                onKeyDown={(e) => handleKeyDown(e)}
                type='text'
            />
        </p>
    );
};