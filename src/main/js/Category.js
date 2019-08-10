import React from 'react';

const categoryStyle = {
    align: 'center',
    marginTop: 10,
    color: 'white',
    display: 'inline-block',
    marginRight: 10
};

export const Category = ({setPath}) => {
    return (
        <div style={categoryStyle}>
            <p style={categoryStyle}>Category: </p>
            <button style={{marginRight: 5}} onClick={(e) => setPath(e.target.value)} value='Movies'>Movies</button>
            <button onClick={(e) => setPath(e.target.value)} value='Series'>Series</button>
        </div>
    );
};
