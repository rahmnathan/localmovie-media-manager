import React from 'react';
import { Media } from './Media.jsx';
import { trackWindowScroll } from 'react-lazy-load-image-component';

const mediaListStyle = {
    margin: 10,
    display: 'inline-block',
    width: '97%',
    paddingTop: 150,
    textAlign: 'center'
};

export const MediaList = ({ media, setPath, playMedia }) => {
    const mediaList = media.map(media =>
        <Media key={media.path} media={media} setPath={setPath} playMedia={playMedia}/>
    );

    return (
        <div style={mediaListStyle}>
            {mediaList}
        </div>
    )
};

export default trackWindowScroll(MediaList);