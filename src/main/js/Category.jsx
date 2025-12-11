import React from 'react';

export const Category = ({setPath, setType}) => {
    return (
        <div className="control-bar__group" role="group" aria-label="Category selection">
            <span className="control-bar__label">Category</span>
            <button
                className="control-bar__button"
                onClick={(e) => setPath(e.target.value)}
                value='Movies'
                aria-label="Show movies"
            >
                Movies
            </button>
            <button
                className="control-bar__button"
                onClick={(e) => setPath(e.target.value)}
                value='Series'
                aria-label="Show TV series"
            >
                Series
            </button>
            <button
                className="control-bar__button"
                onClick={(e) => setType('favorites')}
                value='favorites'
                aria-label="Show favorites"
            >
                Favorites
            </button>
            <button
                className="control-bar__button"
                onClick={(e) => setType('history')}
                value='history'
                aria-label="Show viewing history"
            >
                History
            </button>
        </div>
    );
};
