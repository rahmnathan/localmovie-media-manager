import React from 'react';

const categoryStyle = {
    align: 'center',
    marginTop: 10,
    color: 'white',
    display: 'inline-block',
    marginRight: 10
};

export const Category = ({setPath, setType}) => {
    return (
        <div style={categoryStyle} role="group" aria-label="Category selection">
            <p style={categoryStyle}>Category: </p>
            <button
                style={{marginRight: 5}}
                onClick={(e) => setPath(e.target.value)}
                value='Movies'
                aria-label="Show movies"
            >
                Movies
            </button>
            <button
                style={{marginRight: 5}}
                onClick={(e) => setPath(e.target.value)}
                value='Series'
                aria-label="Show TV series"
            >
                Series
            </button>
            <button
                onClick={(e) => setType('history')}
                value='history'
                aria-label="Show viewing history"
            >
                History
            </button>
        </div>
    );
};
