import React from 'react';

const searchBoxStyle = {
    color: 'white',
    fontSize: 14,
    margin: 'auto',
    textAlign: 'center',
    paddingTop: 10
};

export const SearchBox = ({ filterMedia }) => {
    return (<p style={searchBoxStyle}>Search<br/><input onChange={(e) => filterMedia(e.target.value)} type='text'/></p>);
};