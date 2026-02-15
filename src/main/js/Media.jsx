import React, { memo, useState } from 'react';
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

    const selectMedia = (mediaFile) => {
        // If the media is streamable, it should be played
        if(mediaFile.streamable) {
            props.playMedia(mediaFile, false);
        } else {
            // Otherwise navigate using parentId with the correct type
            const requestType = getRequestType(mediaFile.mediaFileType);
            props.navigateTo(requestType, mediaFile.mediaFileId);
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

    const openDetailedView = (event) => {
        event.stopPropagation();
        setIsDetailedViewOpen(true);
    };

    const handleKeyPress = (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            selectMedia(mediaFile);
        }
    };

    let title = mediaFile.fileName.substring(0, mediaFile.fileName.length - 4);
    if(mediaFile.streamable && media.number != null) {
        if(media.title !== null && media.mediaType === "EPISODE"){
            title = "#" + media.number + " - " + media.title;
        } else {
            title = "#" + media.number;
        }
    } else if(media.title != null){
        title = media.title;
    }

    const year = media.releaseYear || 0;
    const rating = media.imdbRating || '';

    const isPlayable = mediaFile.streamable;
    const ariaLabel = isPlayable
        ? `Play ${title} from ${year}. Rating: ${rating || 'not rated'}`
        : `Open folder ${title}`;

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
            <LazyLoadImage
                onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}}
                src={buildPosterUri(mediaFile.mediaFileId)}
                alt={`${title} (${year}) poster`}
                className="media-card__poster"
                scrollPosition={props.scrollPosition}
                effect="opacity"
                threshold={100}
                placeholder={
                    <div className="media-card__poster" style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)'
                    }} />
                }
            />
            <div className="media-card__description">
                <h3 className="media-card__title">{title}</h3>
                <p className="media-card__year">{year}</p>
                <div className="media-card__rating-container">
                    <img src={'imdb.png'} alt={'IMDB logo'} className="media-card__imdb-icon"/>
                    <span className="media-card__rating">{rating}</span>
                </div>
                <p className="media-card__text">{genre}</p>
                <p className="media-card__plot">{plot}</p>
            </div>
            <DetailedMediaView
                mediaFile={mediaFile}
                isOpen={isDetailedViewOpen}
                onClose={() => setIsDetailedViewOpen(false)}
                playMedia={props.playMedia}
                isFavorite={isFavorite}
                onToggleFavorite={toggleFavorite}
            />
        </div>
    );
};

export const Media = memo(MediaComponent);