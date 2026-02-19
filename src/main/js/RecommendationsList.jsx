import React, { useMemo, useState } from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import { buildPosterUri } from './Media.jsx';
import { DetailedMediaView } from './DetailedMediaView.jsx';
import { UserPreferences } from './userPreferences.js';

const RECOMMENDATION_DISMISS_KEY = 'localmovies_recommendations_hidden';

const loadDismissed = () => {
    try {
        return new Set(JSON.parse(localStorage.getItem(RECOMMENDATION_DISMISS_KEY) || '[]'));
    } catch {
        return new Set();
    }
};

const saveDismissed = (ids) => {
    localStorage.setItem(RECOMMENDATION_DISMISS_KEY, JSON.stringify(Array.from(ids)));
};

const buildConfidence = (media, reason) => {
    const rating = Number.parseFloat(media?.imdbRating || '0');
    const hasDetailedReason = (reason || '').length >= 40;
    if (rating >= 7.8 && hasDetailedReason) return { label: 'High confidence', tone: 'high' };
    if (rating >= 6.8 || hasDetailedReason) return { label: 'Medium confidence', tone: 'medium' };
    return { label: 'Exploration pick', tone: 'low' };
};

const genreChips = (genre) => (genre || '')
    .split(',')
    .map(value => value.trim())
    .filter(Boolean)
    .slice(0, 2);

const RecommendationCard = ({ recommendation, playMedia, onDismiss, onMoreLikeThis }) => {
    const mediaFile = recommendation.mediaFile;
    const media = mediaFile.media || {};
    const reason = recommendation.reason;
    const confidence = buildConfidence(media, reason);
    const genres = genreChips(media.genre);

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

                <div className="recommendation-card__chips">
                    <span className={`recommendation-card__chip recommendation-card__chip--${confidence.tone}`}>
                        {confidence.label}
                    </span>
                    {genres.map((genre) => (
                        <button
                            key={genre}
                            className="recommendation-card__chip recommendation-card__chip--genre"
                            onClick={(event) => {
                                event.stopPropagation();
                                onMoreLikeThis?.(genre);
                            }}
                            title={`Show more ${genre}`}
                        >
                            {genre}
                        </button>
                    ))}
                </div>

                <div className="recommendation-card__actions">
                    <button
                        className="recommendation-card__action-btn"
                        onClick={(event) => {
                            event.stopPropagation();
                            onMoreLikeThis?.(media.genre);
                        }}
                    >
                        More like this
                    </button>
                    <button
                        className="recommendation-card__action-btn recommendation-card__action-btn--dismiss"
                        onClick={(event) => {
                            event.stopPropagation();
                            onDismiss?.(mediaFile.mediaFileId);
                        }}
                    >
                        Not interested
                    </button>
                </div>
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

export const RecommendationsList = ({ recommendations, playMedia, onRefresh, isLoading, onMoreLikeThis }) => {
    const [dismissedIds, setDismissedIds] = useState(loadDismissed);

    const visibleRecommendations = useMemo(() => {
        if (!recommendations) return [];
        return recommendations.filter(rec => !dismissedIds.has(rec?.mediaFile?.mediaFileId));
    }, [recommendations, dismissedIds]);

    const dismissRecommendation = (mediaFileId) => {
        const next = new Set(dismissedIds);
        next.add(mediaFileId);
        setDismissedIds(next);
        saveDismissed(next);
    };

    if (!isLoading && (!visibleRecommendations || visibleRecommendations.length === 0)) {
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
                {visibleRecommendations.map((rec, index) => (
                    <RecommendationCard
                        key={rec.mediaFile?.mediaFileId || index}
                        recommendation={rec}
                        playMedia={playMedia}
                        onDismiss={dismissRecommendation}
                        onMoreLikeThis={onMoreLikeThis}
                    />
                ))}
            </div>
        </main>
    );
};
