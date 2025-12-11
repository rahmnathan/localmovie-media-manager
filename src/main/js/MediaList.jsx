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
        <main style={mediaListStyle} aria-label="Media library">
            <InfiniteScroll
                dataLength={media.length}
                next={nextPage}
                hasMore={hasMore}
                loader={<p role="status" aria-live="polite">Loading more media...</p>}
                endMessage={<p role="status">No more items to load.</p>}
            >
                <div role="list" aria-label="Media items">
                    {mediaList}
                </div>
            </InfiniteScroll>
        </main>
    )
};
