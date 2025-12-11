import React from 'react';

export const Sort = ({ selectSort }) => {
    return (
        <div className="control-bar__group">
            <label htmlFor="sort-select" className="control-bar__label">Sort</label>
            <select
                id="sort-select"
                className="control-bar__select"
                onChange={(e) => selectSort(e.target.value)}
                aria-label="Sort media by"
            >
                <option value='title'>Title</option>
                <option value='year'>Year</option>
                <option value='rating'>Rating</option>
                <option value='added'>Date Added</option>
            </select>
        </div>
    );
};
