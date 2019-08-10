import React from 'react';
import { Media } from './Media';
import { LazyLoadImage, trackWindowScroll } from 'react-lazy-load-image-component';

const mediaListStyle = {
    margin: 10,
    display: 'inline-block',
    width: '80%',
    paddingTop: 150
};

export const MediaList = ({ media, setPath}) => {
    const mediaList = media.map(media =>
        <Media key={media.path} media={media} setPath={setPath}/>
    );

    return (
        <div style={mediaListStyle}>
            {mediaList}
        </div>
    )
};

export default trackWindowScroll(MediaList);