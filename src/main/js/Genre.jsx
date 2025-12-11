import React from 'react';

const genreStyle = {
    align: 'center',
    marginTop: 10,
    color: 'white',
    marginRight: 10,
    display: 'inline-block'
};

export const Genre = ({ selectGenre }) => {
    return (
        <div style={genreStyle}>
            <label htmlFor="genre-select" style={genreStyle}>Genre: </label>
            <select
                id="genre-select"
                onChange={(e) => selectGenre(e.target.value)}
                aria-label="Filter by genre"
            >
                <option value='all'>All</option>
                <option value='action'>Action</option>
                <option value='comedy'>Comedy</option>
                <option value='fantasy'>Fantasy</option>
                <option value='horror'>Horror</option>
                <option value='sci-fi'>Sci-Fi</option>
                <option value='thriller'>Thriller</option>
                <option value='war'>War</option>
            </select>
        </div>
    );
};
