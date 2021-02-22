import React from 'react';
import { SearchBox } from "./SearchBox.jsx";
import { Category } from "./Category.jsx";
import { Genre } from "./Genre.jsx";
import { Sort } from './Sort.jsx';

const controlBarStyle = {
    textAlign: 'center',
    position: 'fixed',
    left: '50%',
    marginLeft: '-50%',
    width: '100%',
    background: 'rgb(0,0,0)',
    marginTop: 0
};

export const ControlBar = ({ filterMedia, setPath, selectGenre, selectSort }) => {
    return (
        <div style={controlBarStyle}>
            <SearchBox filterMedia={filterMedia}/>
            <Category setPath={setPath}/>
            <Genre selectGenre={selectGenre}/>
            <Sort selectSort={selectSort}/>
        </div>
    );
};
