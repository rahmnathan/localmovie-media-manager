import React from 'react';

export const Category = ({setType}) => {
    return (
        <div className="control-bar__group" role="group" aria-label="Category selection">
            <span className="control-bar__label">Category</span>
            <button
                className="control-bar__button"
                onClick={() => setType('MOVIES')}
                aria-label="Show movies"
            >
                Movies
            </button>
            <button
                className="control-bar__button"
                onClick={() => setType('SERIES')}
                aria-label="Show TV series"
            >
                Series
            </button>
            <button
                className="control-bar__button"
                onClick={() => setType('favorites')}
                aria-label="Show favorites"
            >
                Favorites
            </button>
            <button
                className="control-bar__button"
                onClick={() => setType('history')}
                aria-label="Show viewing history"
            >
                History
            </button>
        </div>
    );
};
