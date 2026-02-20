import React, { useEffect, useMemo, useRef } from 'react';
import { Media } from './Media.jsx';
import { SkeletonLoader } from './SkeletonCard.jsx';

export const MediaList = ({ media, navigateTo, playMedia, nextPage, hasMore, topPadding = 150, isLoadingMore = false }) => {
    const loadMoreTriggerRef = useRef(null);

    const mediaListStyle = {
        margin: 10,
        display: 'inline-block',
        width: '97%',
        paddingTop: topPadding,
        textAlign: 'center'
    };

    useEffect(() => {
        const trigger = loadMoreTriggerRef.current;
        if (!trigger || typeof IntersectionObserver === 'undefined') return;

        const observer = new IntersectionObserver((entries) => {
            const entry = entries[0];
            if (entry?.isIntersecting && !isLoadingMore && hasMore()) {
                nextPage();
            }
        }, {
            root: null,
            rootMargin: '1000px 0px 1000px 0px',
            threshold: 0
        });

        observer.observe(trigger);

        return () => {
            observer.disconnect();
        };
    }, [nextPage, isLoadingMore, hasMore, media.length]);

    const mediaList = useMemo(() => media.map(item =>
        <Media key={item.mediaFileId} media={item} navigateTo={navigateTo} playMedia={playMedia}/>
    ), [media, navigateTo, playMedia]);

    return (
        <main style={mediaListStyle} aria-label="Media library">
            <div className="media-list__items" role="list" aria-label="Media items">
                {mediaList}
            </div>
            <div ref={loadMoreTriggerRef} aria-hidden="true" />
            {isLoadingMore && (
                <div className="media-list__pagination-loader" role="status" aria-live="polite">
                    <SkeletonLoader count={3} />
                </div>
            )}
            {!hasMore() && media.length > 0 && (
                <p role="status">No more items to load.</p>
            )}
        </main>
    )
};
