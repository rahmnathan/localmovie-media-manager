import React, { useMemo } from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import { buildPosterUri } from './Media.jsx';

const getResumePositionMs = (mediaFile) => {
    const view = mediaFile?.mediaViews?.[0];
    if (!view || !view.position || view.position <= 0) return 0;
    return view.position * 1000;
};

const getProgressPercent = (mediaFile) => {
    const view = mediaFile?.mediaViews?.[0];
    if (!view || !view.position || !view.duration || view.duration <= 0) return 0;
    return Math.max(0, Math.min(100, (view.position / view.duration) * 100));
};

const ContinueCard = ({ mediaFile, playMedia }) => {
    const title = mediaFile?.media?.title || mediaFile?.fileName || 'Unknown';
    const resumePositionMs = getResumePositionMs(mediaFile);
    const progress = getProgressPercent(mediaFile);

    const handlePlay = () => {
        playMedia(mediaFile, resumePositionMs > 0);
    };

    return (
        <button className="continue-rail__card" onClick={handlePlay}>
            <div className="continue-rail__poster-wrap">
                <LazyLoadImage
                    onError={(e) => { e.target.onerror = null; e.target.src = 'noPicture.gif'; }}
                    src={buildPosterUri(mediaFile.mediaFileId)}
                    alt={`${title} poster`}
                    className="continue-rail__poster"
                    effect="opacity"
                />
                <div className="continue-rail__progress">
                    <div className="continue-rail__progress-fill" style={{ width: `${progress}%` }} />
                </div>
            </div>
            <span className="continue-rail__title">{title}</span>
        </button>
    );
};

export const ContinueWatchingRail = ({ media, playMedia }) => {
    const items = useMemo(() => {
        if (!media) return [];
        const unique = new Map();
        for (const item of media) {
            if (!item?.streamable) continue;
            const progress = getProgressPercent(item);
            if (progress < 2 || progress >= 98) continue;
            if (!unique.has(item.mediaFileId)) {
                unique.set(item.mediaFileId, item);
            }
        }
        return Array.from(unique.values()).slice(0, 12);
    }, [media]);

    if (items.length === 0) return null;

    return (
        <section className="continue-rail" aria-label="Continue watching">
            <div className="continue-rail__header">
                <h2 className="continue-rail__heading">Continue Watching</h2>
            </div>
            <div className="continue-rail__items">
                {items.map((item) => (
                    <ContinueCard key={item.mediaFileId} mediaFile={item} playMedia={playMedia} />
                ))}
            </div>
        </section>
    );
};
