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

// Path depth constants for determining if media is playable
const MOVIE_DIRECTORY_DEPTH = 2;
const SERIES_EPISODE_DEPTH = 4;

const MediaComponent = (props) => {
    const mediaFile = props.media;
    const media = mediaFile.media;
    const plot = media.plot;
    const genre = media.genre;

    const [isFavorite, setIsFavorite] = useState(() =>
        UserPreferences.isFavorite(mediaFile.mediaFileId)
    );
    const [isDetailedViewOpen, setIsDetailedViewOpen] = useState(false);

    const selectMedia = (mediaFile) => {
        const path = mediaFile.path;
        const pathDepth = path.split("/").length;
        const isPlayable = (path.includes("Movies") && pathDepth === MOVIE_DIRECTORY_DEPTH) ||
                          pathDepth === SERIES_EPISODE_DEPTH;

        if(isPlayable) {
            props.playMedia(mediaFile, false);
        } else {
            props.setPath(mediaFile.path);
        }
    };

    const toggleFavorite = (event) => {
        event.stopPropagation();
        if (isFavorite) {
            UserPreferences.removeFavorite(mediaFile.mediaFileId);
            setIsFavorite(false);
        } else {
            UserPreferences.addFavorite(mediaFile.mediaFileId);
            setIsFavorite(true);
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

    const path = mediaFile.path;
    const pathDepth = path.split("/").length;
    const isPlayable = (path.includes("Movies") && pathDepth === MOVIE_DIRECTORY_DEPTH) ||
                      pathDepth === SERIES_EPISODE_DEPTH;
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
                className="media-card__favorite-btn"
                onClick={toggleFavorite}
                aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
            >
                <span
                    className={`media-card__favorite-icon ${
                        isFavorite ? 'media-card__favorite-icon--active' : 'media-card__favorite-icon--inactive'
                    }`}
                >
                    ♥
                </span>
            </button>
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
            />
        </div>
    );
};

export const Media = memo(MediaComponent);