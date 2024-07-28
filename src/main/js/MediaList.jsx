import React from 'react';
import { Media } from './Media.jsx';
import InfiniteScroll from "react-infinite-scroll-component";

const mediaListStyle = {
    margin: 10,
    display: 'inline-block',
    width: '97%',
    paddingTop: 150,
    textAlign: 'center'
};

export const MediaList = ({ media, setPath, playMedia, nextPage, hasMore }) => {
    const mediaList = media.map(media =>
        <Media key={media.path} media={media} setPath={setPath} playMedia={playMedia}/>
    );

    return (
        <div style={mediaListStyle}>
            <InfiniteScroll
                dataLength={media.length}
                next={nextPage}
                hasMore={hasMore}
                loader={<p>Loading...</p>}
                endMessage={<p>No more data to load.</p>}
            >
                <ul>
                    {mediaList}
                </ul>
            </InfiniteScroll>
        </div>
    )
};
