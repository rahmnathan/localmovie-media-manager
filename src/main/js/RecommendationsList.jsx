import React, { useState } from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import { buildPosterUri } from './Media.jsx';
import { DetailedMediaView } from './DetailedMediaView.jsx';
import { UserPreferences } from './userPreferences.js';

const RecommendationCard = ({ recommendation, playMedia }) => {
    const mediaFile = recommendation.mediaFile;
    const media = mediaFile.media || {};
    const reason = recommendation.reason;

    const [isFavorite, setIsFavorite] = useState(mediaFile.favorite || false);
    const [isDetailedViewOpen, setIsDetailedViewOpen] = useState(false);

    const title = media.title || mediaFile.fileName?.replace(/\.[^/.]+$/, '') || 'Unknown';
    const year = media.releaseYear || 0;
    const rating = media.imdbRating || '';

    const handleClick = () => {
        if (mediaFile.streamable) {
            playMedia(mediaFile, false);
        } else {
            setIsDetailedViewOpen(true);
        }
    };

    const handleKeyPress = (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleClick();
        }
    };

    const toggleFavorite = async (event) => {
        event.stopPropagation();
        const newFavoriteState = !isFavorite;
        setIsFavorite(newFavoriteState);

        const success = newFavoriteState
            ? await UserPreferences.addFavorite(mediaFile.mediaFileId)
            : await UserPreferences.removeFavorite(mediaFile.mediaFileId);

        if (!success) {
            setIsFavorite(!newFavoriteState);
        }
    };

    const openDetailedView = (event) => {
        event.stopPropagation();
        setIsDetailedViewOpen(true);
    };

    return (
        <div
            className="recommendation-card"
            onClick={handleClick}
            onKeyPress={handleKeyPress}
            tabIndex={0}
            role="button"
            aria-label={`Play ${title}`}
        >
            <div className="recommendation-card__poster">
                <LazyLoadImage
                    onError={(e) => { e.target.onerror = null; e.target.src = "noPicture.gif"; }}
                    src={buildPosterUri(mediaFile.mediaFileId)}
                    alt={`${title} poster`}
                    effect="opacity"
                    threshold={100}
                />
                <button
                    className="recommendation-card__info-btn"
                    onClick={openDetailedView}
                    aria-label="View details"
                    title="View details"
                >
                    <span className="recommendation-card__info-icon">i</span>
                </button>
                <button
                    className="recommendation-card__favorite-btn"
                    onClick={toggleFavorite}
                    aria-label={isFavorite ? "Remove from favorites" : "Add to favorites"}
                    title={isFavorite ? "Remove from favorites" : "Add to favorites"}
                >
                    <span className={`recommendation-card__favorite-icon ${isFavorite ? 'recommendation-card__favorite-icon--active' : ''}`}>
                        {isFavorite ? '♥' : '♡'}
                    </span>
                </button>
            </div>

            <div className="recommendation-card__content">
                <h3 className="recommendation-card__title">{title}</h3>

                <div className="recommendation-card__meta">
                    {year > 0 && <span className="recommendation-card__year">{year}</span>}
                    {rating && (
                        <span className="recommendation-card__rating">
                            <img src="imdb.png" alt="IMDB" className="recommendation-card__imdb-icon"/>
                            {rating}
                        </span>
                    )}
                </div>

                {reason && (
                    <p className="recommendation-card__reason">{reason}</p>
                )}
            </div>

            <button
                className="recommendation-card__play-btn"
                onClick={(e) => { e.stopPropagation(); handleClick(); }}
                aria-label="Play"
            >
                <span className="recommendation-card__play-icon">▶</span>
            </button>

            <DetailedMediaView
                mediaFile={mediaFile}
                isOpen={isDetailedViewOpen}
                onClose={() => setIsDetailedViewOpen(false)}
                playMedia={playMedia}
                isFavorite={isFavorite}
                onToggleFavorite={toggleFavorite}
            />
        </div>
    );
};

export const RecommendationsList = ({ recommendations, playMedia, onRefresh, isLoading }) => {
    if (!isLoading && (!recommendations || recommendations.length === 0)) {
        return (
            <div className="empty-state" role="status">
                <h2>No recommendations yet</h2>
                <p>Watch some movies or shows to get personalized recommendations based on your viewing history.</p>
            </div>
        );
    }

    return (
        <main className="recommendations-list" aria-label="Personalized recommendations">
            <div className="recommendations-list__header">
                <h2 className="recommendations-list__heading">For You</h2>
                <p className="recommendations-list__subheading">Personalized picks based on what you've watched</p>
            </div>
            <div className="recommendations-list__items" role="list">
                {recommendations.map((rec, index) => (
                    <RecommendationCard
                        key={rec.mediaFile?.mediaFileId || index}
                        recommendation={rec}
                        playMedia={playMedia}
                    />
                ))}
            </div>
        </main>
    );
};
