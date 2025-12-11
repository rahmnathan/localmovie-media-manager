import React from 'react';

export const Genre = ({ selectGenre }) => {
    return (
        <div className="control-bar__group">
            <label htmlFor="genre-select" className="control-bar__label">Genre</label>
            <select
                id="genre-select"
                className="control-bar__select"
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
