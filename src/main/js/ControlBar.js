import React from 'react';
import { SearchBox } from "./SearchBox";
import { Category } from "./Category";
import { Genre } from "./Genre";
import { Sort } from './Sort';

const controlBarStyle = {
    textAlign: 'center',
    position: 'fixed',
    left: '50%',
    marginLeft: '-50%',
    width: '100%',
    background: 'rgb(21, 21, 30)',
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
