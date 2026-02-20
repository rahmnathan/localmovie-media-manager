import React from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { buildPosterUri } from './Media.jsx';

// Path depth constants for determining if media is playable
const MOVIE_DIRECTORY_DEPTH = 2;
const SERIES_EPISODE_DEPTH = 4;

/**
 * Format duration in seconds to human-readable format (e.g., "1h 42m" or "45m")
 */
const formatRuntime = (seconds) => {
    if (!seconds || seconds <= 0) return null;

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (hours > 0) {
        return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
};

export const DetailedMediaView = ({ mediaFile, isOpen, onClose, playMedia, isFavorite = false, onToggleFavorite, isLoadingDetails = false }) => {
    if (!mediaFile) return null;

    const media = mediaFile.media || {};
    const title = media.title || mediaFile.fileName;
    const year = media.releaseYear || '';
    const imdbRating = media.imdbRating || 'N/A';
    const metaRating = media.metaRating || 'N/A';
    const genre = media.genre || 'Unknown';
    const actors = media.actors || 'Not available';
    const plot = media.plot || 'No description available.';
    const mediaType = media.mediaType || '';
    const episodeNumber = media.number;

    // Check if media is playable
    const path = mediaFile.path;
    const pathDepth = path?.split("/")?.length || 0;
    const isPlayable = (path?.includes("Movies") && pathDepth === MOVIE_DIRECTORY_DEPTH) ||
                       pathDepth === SERIES_EPISODE_DEPTH;

    // Check if user can resume and get runtime from views
    const mediaViews = mediaFile.mediaViews;
    const canResume = mediaViews?.length > 0 && (mediaViews[0]?.position || 0) > 0;
    const runtime = mediaViews?.length > 0 ? formatRuntime(mediaViews[0]?.duration) : null;

    const handlePlay = () => {
        if (playMedia) {
            playMedia(mediaFile, false);
        }
        onClose();
    };

    const handleResume = () => {
        if (playMedia) {
            playMedia(mediaFile, true);
        }
        onClose();
    };

    return (
        <Dialog open={isOpen} onClose={onClose} className="detailed-media-dialog">
            <div className="detailed-media-backdrop" onClick={onClose} />
            <DialogPanel className="detailed-media-panel">
                <button
                    className="detailed-media-close"
                    onClick={onClose}
                    aria-label="Close detailed view"
                >
                    ✕
                </button>

                <div className="detailed-media-content">
                    <div className="detailed-media-poster-container">
                        <img
                            src={buildPosterUri(mediaFile.mediaFileId)}
                            alt={`${title} poster`}
                            className="detailed-media-poster"
                            onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = 'noPicture.gif';
                            }}
                        />
                    </div>

                    <div className="detailed-media-info">
                        <div className="detailed-media-title-row">
                            <DialogTitle className="detailed-media-title">
                                {episodeNumber && mediaType === 'EPISODE' ? `E${episodeNumber} - ${title}` : title}
                            </DialogTitle>
                            {onToggleFavorite && (
                                <button
                                    className="detailed-media-favorite-btn"
                                    onClick={onToggleFavorite}
                                    aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                                    title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                                >
                                    <span className={`detailed-media-favorite-icon ${isFavorite ? 'detailed-media-favorite-icon--active' : ''}`}>
                                        ♥
                                    </span>
                                </button>
                            )}
                        </div>

                        <div className="detailed-media-metadata">
                            {year && (
                                <div className="detailed-media-meta-item">
                                    <span className="detailed-media-meta-label">Year:</span>
                                    <span className="detailed-media-meta-value">{year}</span>
                                </div>
                            )}

                            {runtime && (
                                <div className="detailed-media-meta-item">
                                    <span className="detailed-media-meta-label">Runtime:</span>
                                    <span className="detailed-media-meta-value">{runtime}</span>
                                </div>
                            )}

                            <div className="detailed-media-meta-item">
                                <span className="detailed-media-meta-label">Genre:</span>
                                <span className="detailed-media-meta-value">{genre}</span>
                            </div>

                            <div className="detailed-media-meta-item">
                                <span className="detailed-media-meta-label">IMDB Rating:</span>
                                <span className="detailed-media-meta-value">
                                    <img src="imdb.png" alt="IMDB" className="detailed-media-rating-icon" />
                                    {imdbRating}
                                </span>
                            </div>

                            {metaRating !== 'N/A' && (
                                <div className="detailed-media-meta-item">
                                    <span className="detailed-media-meta-label">Metacritic:</span>
                                    <span className="detailed-media-meta-value">{metaRating}</span>
                                </div>
                            )}

                            <div className="detailed-media-meta-item detailed-media-meta-item--full">
                                <span className="detailed-media-meta-label">Cast:</span>
                                <span className="detailed-media-meta-value">{actors}</span>
                            </div>
                        </div>

                        <div className="detailed-media-plot-section">
                            <h3 className="detailed-media-plot-title">Overview</h3>
                            <p className="detailed-media-plot">
                                {isLoadingDetails ? 'Loading details...' : plot}
                            </p>
                        </div>

                        {isPlayable && (
                            <div className="detailed-media-actions">
                                <button
                                    className="detailed-media-btn detailed-media-btn--primary"
                                    onClick={handlePlay}
                                >
                                    ▶ Play
                                </button>
                                {canResume && (
                                    <button
                                        className="detailed-media-btn detailed-media-btn--secondary"
                                        onClick={handleResume}
                                    >
                                        ⏯ Resume
                                    </button>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </DialogPanel>
        </Dialog>
    );
};
