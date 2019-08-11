import React from 'react';

const sortStyle = {
    align: 'center',
    marginTop: 10,
    color: 'white',
    marginRight: 10,
    display: 'inline-block'
};

export const Sort = ({ selectSort }) => {
    return (
        <div style={sortStyle}>
            <p style={sortStyle}>Sort: </p>
            <select onChange={(e) => selectSort(e.target.value)}>
                <option value='title'>Title</option>
                <option value='year'>Year</option>
                <option value='rating'>Rating</option>
                <option value='added'>Date Added</option>
            </select>
        </div>
    );
};
