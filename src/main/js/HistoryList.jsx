import React, { useMemo } from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import { buildPosterUri } from './Media.jsx';

/**
 * Groups history items by series for a "Continue Watching" experience.
 * Episodes are grouped under their series, movies remain standalone.
 */
const groupHistoryItems = (mediaList) => {
    const groups = new Map();

    for (const mediaFile of mediaList) {
        const seriesInfo = getSeriesInfo(mediaFile);

        if (seriesInfo) {
            // This is an episode - group by series
            const existing = groups.get(seriesInfo.seriesId);
            if (!existing) {
                // First episode of this series we've seen (most recent due to sort order)
                groups.set(seriesInfo.seriesId, {
                    id: seriesInfo.seriesId,
                    title: seriesInfo.seriesTitle,
                    posterId: seriesInfo.seriesId, // Use series ID for poster
                    isSeries: true,
                    mostRecentEpisode: mediaFile,
                    seasonNumber: seriesInfo.seasonNumber,
                    episodeNumber: mediaFile.media?.number,
                    episodeTitle: mediaFile.media?.title
                });
            }
            // Keep only the first (most recent) episode per series
        } else {
            // Standalone movie or video
            groups.set(mediaFile.mediaFileId, {
                id: mediaFile.mediaFileId,
                title: mediaFile.media?.title || mediaFile.fileName,
                posterId: mediaFile.mediaFileId,
                isSeries: false,
                movie: mediaFile
            });
        }
    }

    return Array.from(groups.values());
};

/**
 * Extract series info from a media file using the parent chain.
 * Returns null if not an episode.
 */
const getSeriesInfo = (mediaFile) => {
    const parent = mediaFile.parent;
    if (!parent) return null;

    // For episodes: parent is season, parent.parent is series
    if (parent.mediaFileType === 'SEASON') {
        const season = parent;
        const series = season.parent;

        return {
            seriesId: series?.mediaFileId || season.mediaFileId,
            seriesTitle: series?.title || season.title || 'Unknown Series',
            seasonNumber: season.number
        };
    }

    // Direct child of series (no season folder)
    if (parent.mediaFileType === 'SERIES') {
        return {
            seriesId: parent.mediaFileId,
            seriesTitle: parent.title || 'Unknown Series',
            seasonNumber: null
        };
    }

    return null;
};

/**
 * Format milliseconds to human-readable duration.
 */
const formatDuration = (millis) => {
    if (!millis || millis <= 0) return null;

    const totalSeconds = Math.floor(millis / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);

    if (hours > 0) {
        return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
};

/**
 * Get the resume position from mediaViews in milliseconds.
 * Position is stored in seconds, so we convert to milliseconds for display.
 */
const getResumePosition = (mediaFile) => {
    const views = mediaFile.mediaViews;
    if (!views || views.length === 0) return null;

    // Get the most recent view with position > 0
    const view = views[0];
    if (view && view.position > 0) {
        return view.position * 1000;  // Convert seconds to milliseconds
    }
    return null;
};

const HistoryCard = ({ group, playMedia }) => {
    const mediaFile = group.mostRecentEpisode || group.movie;
    const resumePosition = getResumePosition(mediaFile);
    const resumeText = formatDuration(resumePosition);

    const handleClick = () => {
        playMedia(mediaFile, resumePosition > 0);
    };

    const handleKeyPress = (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleClick();
        }
    };

    // Build episode info text
    let episodeText = '';
    if (group.isSeries && group.mostRecentEpisode) {
        const parts = [];
        if (group.seasonNumber) parts.push(`S${group.seasonNumber}`);
        if (group.episodeNumber) parts.push(`E${group.episodeNumber}`);
        if (parts.length > 0) episodeText += parts.join(' ');
        if (group.episodeTitle) {
            if (episodeText) episodeText += ' · ';
            episodeText += group.episodeTitle;
        }
    }

    return (
        <div
            className="history-card"
            onClick={handleClick}
            onKeyPress={handleKeyPress}
            tabIndex={0}
            role="button"
            aria-label={`Continue watching ${group.title}${episodeText ? `: ${episodeText}` : ''}`}
        >
            <div className="history-card__poster">
                <LazyLoadImage
                    onError={(e) => { e.target.onerror = null; e.target.src = "noPicture.gif"; }}
                    src={buildPosterUri(group.posterId)}
                    alt={`${group.title} poster`}
                    effect="opacity"
                    threshold={100}
                />
            </div>

            <div className="history-card__content">
                <h3 className="history-card__title">{group.title}</h3>

                {episodeText && (
                    <p className="history-card__episode">{episodeText}</p>
                )}

                {resumeText && (
                    <p className="history-card__resume">Resume at {resumeText}</p>
                )}
            </div>

            <button
                className="history-card__play-btn"
                onClick={(e) => { e.stopPropagation(); handleClick(); }}
                aria-label="Continue watching"
            >
                <span className="history-card__play-icon">▶</span>
            </button>
        </div>
    );
};

export const HistoryList = ({ media, playMedia }) => {
    const groupedHistory = useMemo(() => groupHistoryItems(media), [media]);

    if (groupedHistory.length === 0) {
        return (
            <div className="empty-state" role="status">
                <h2>No viewing history</h2>
                <p>Your recently watched movies and shows will appear here.</p>
            </div>
        );
    }

    return (
        <main className="history-list" aria-label="Continue watching">
            <h2 className="history-list__heading">Continue Watching</h2>
            <div className="history-list__items" role="list">
                {groupedHistory.map(group => (
                    <HistoryCard
                        key={group.id}
                        group={group}
                        playMedia={playMedia}
                    />
                ))}
            </div>
        </main>
    );
};
