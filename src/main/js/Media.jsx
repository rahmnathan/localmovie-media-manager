import React, { memo, useEffect, useState } from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import { UserPreferences } from './userPreferences.js';
import { DetailedMediaView } from './DetailedMediaView.jsx';

const posterBasePath = '/localmovie/v1/media/';

export const buildPosterUri = function (id) {
    if(id === null){
        return 'noPicture.gif';
    } else {
        return window.location.origin + posterBasePath + encodeURIComponent(id) + '/poster';
    }
};

// Map mediaFileType to the request type for navigation
const getRequestType = (mediaFileType) => {
    switch(mediaFileType) {
        case 'SERIES':
            return 'SEASONS';
        case 'SEASON':
            return 'EPISODES';
        case 'MOVIE_FOLDER':
            return 'MOVIES';
        case 'EPISODE_FOLDER':
            return 'EPISODES';
        default:
            return null;
    }
};

const MediaComponent = (props) => {
    const mediaFile = props.media;
    const media = mediaFile.media;
    const plot = media.plot;
    const genre = media.genre;

    const [isFavorite, setIsFavorite] = useState(mediaFile.favorite || false);
    const [isDetailedViewOpen, setIsDetailedViewOpen] = useState(false);
    const [detailedMediaFile, setDetailedMediaFile] = useState(mediaFile);
    const [isLoadingDetails, setIsLoadingDetails] = useState(false);

    useEffect(() => {
        setDetailedMediaFile(mediaFile);
    }, [mediaFile]);

    const selectMedia = (mediaFile) => {
        // If the media is streamable, it should be played
        if(mediaFile.streamable) {
            props.playMedia(mediaFile, false);
        } else {
            // Otherwise navigate using parentId with the correct type
            const requestType = getRequestType(mediaFile.mediaFileType);
            const name = mediaFile.media?.title || mediaFile.fileName;
            props.navigateTo(requestType, mediaFile.mediaFileId, name);
        }
    };

    const toggleFavorite = async (event) => {
        event.stopPropagation();
        const newFavoriteState = !isFavorite;
        setIsFavorite(newFavoriteState); // Optimistic update

        const success = newFavoriteState
            ? await UserPreferences.addFavorite(mediaFile.mediaFileId)
            : await UserPreferences.removeFavorite(mediaFile.mediaFileId);

        if (!success) {
            setIsFavorite(!newFavoriteState); // Revert on failure
        }
    };

    const openDetailedView = async (event) => {
        event.stopPropagation();
        setIsDetailedViewOpen(true);
        const hasDetails = Boolean(detailedMediaFile?.media?.plot) || Boolean(detailedMediaFile?.media?.actors);
        if (hasDetails) {
            return;
        }

        setIsLoadingDetails(true);
        try {
            const response = await fetch(`/localmovie/v1/media/${encodeURIComponent(mediaFile.mediaFileId)}`);
            if (!response.ok) {
                return;
            }
            const data = await response.json();
            if (data) {
                setDetailedMediaFile(data);
            }
        } catch (err) {
            console.error('Failed to fetch detailed media info', err);
        } finally {
            setIsLoadingDetails(false);
        }
    };

    const handleKeyPress = (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            selectMedia(mediaFile);
        }
    };

    let title = mediaFile.fileName.substring(0, mediaFile.fileName.length - 4);
    if(media.number != null && media.mediaType === "EPISODE") {
        // Always show episode number for episodes
        if(media.title !== null){
            title = "E" + media.number + " - " + media.title;
        } else {
            title = "E" + media.number;
        }
    } else if(media.title != null){
        title = media.title;
    }

    // Build parent context (series/season info) for episodes and seasons
    const getParentContext = () => {
        const parent = mediaFile.parent;
        if (!parent) return null;

        // For episodes: show "Series Name · S1"
        if (parent.mediaFileType === 'SEASON') {
            const season = parent;
            const series = season.parent;
            const parts = [];
            if (series?.title) parts.push(series.title);
            if (season.number) parts.push(`S${season.number}`);
            return parts.length > 0 ? parts.join(' · ') : null;
        }

        // For seasons: show "Series Name"
        if (parent.mediaFileType === 'SERIES') {
            return parent.title || null;
        }

        return null;
    };
    const parentContext = getParentContext();

    const year = media.releaseYear || 0;
    const rating = media.imdbRating || '';

    const isPlayable = mediaFile.streamable;
    const ariaLabel = isPlayable
        ? `Play ${title} from ${year}. Rating: ${rating || 'not rated'}`
        : `Open folder ${title}`;

    // Calculate watch progress (0-100%)
    const getWatchProgress = () => {
        const views = mediaFile.mediaViews;
        if (!views || views.length === 0) return null;
        const view = views[0]; // Most recent view
        if (!view.position || !view.duration || view.duration <= 0) return null;
        const progress = (view.position / view.duration) * 100;
        return progress > 1 ? Math.min(progress, 100) : null; // Only show if > 1%
    };
    const watchProgress = isPlayable ? getWatchProgress() : null;

    return (
        <div
            className="media-card"
            onClick={() => selectMedia(mediaFile)}
            onKeyPress={handleKeyPress}
            tabIndex={0}
            role="button"
            aria-label={ariaLabel}
        >
            <button
                className="media-card__info-btn"
                onClick={openDetailedView}
                aria-label="View details"
                title="View details"
            >
                <span className="media-card__info-icon">ⓘ</span>
            </button>
            <div className="media-card__poster-container">
                <LazyLoadImage
                    onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}}
                    src={buildPosterUri(mediaFile.mediaFileId)}
                    alt={`${title} (${year}) poster`}
                    className="media-card__poster"
                    scrollPosition={props.scrollPosition}
                    effect="opacity"
                    threshold={100}
                />
                {watchProgress && (
                    <div className="media-card__progress-bar">
                        <div
                            className="media-card__progress-fill"
                            style={{ width: `${watchProgress}%` }}
                        />
                    </div>
                )}
                <div className="media-card__overlay">
                    {parentContext && (
                        <p className="media-card__parent-context">{parentContext}</p>
                    )}
                    <h3 className="media-card__title">{title}</h3>
                    <div className="media-card__meta">
                        {year > 0 && <span className="media-card__year">{year}</span>}
                        {rating && (
                            <span className="media-card__rating">
                                <img src={'imdb.png'} alt={'IMDB'} className="media-card__imdb-icon"/>
                                {rating}
                            </span>
                        )}
                    </div>
                </div>
            </div>
            {isDetailedViewOpen && (
                <DetailedMediaView
                    mediaFile={detailedMediaFile}
                    isOpen={isDetailedViewOpen}
                    onClose={() => setIsDetailedViewOpen(false)}
                    playMedia={props.playMedia}
                    isFavorite={isFavorite}
                    onToggleFavorite={toggleFavorite}
                    isLoadingDetails={isLoadingDetails}
                />
            )}
        </div>
    );
};

export const Media = memo(MediaComponent);
