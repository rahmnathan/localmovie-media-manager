import React from 'react';
import { SearchBox } from "./SearchBox.jsx";
import { Category } from "./Category.jsx";
import { Genre } from "./Genre.jsx";
import { Sort } from './Sort.jsx';

const controlBarStyle = {
    textAlign: 'center',
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    background: 'rgb(0,0,0)',
    zIndex: 100,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)'
};

export const ControlBar = ({ filterMedia, setPath, selectGenre, selectSort, filterMediaNavigate, setType }) => {
    return (
        <nav style={controlBarStyle} aria-label="Media filters and controls">
            <SearchBox filterMedia={filterMedia} filterMediaNavigate={filterMediaNavigate}/>
            <Category setPath={setPath} setType={setType}/>
            <Genre selectGenre={selectGenre}/>
            <Sort selectSort={selectSort}/>
        </nav>
    );
};
