import React, { useState, useRef } from 'react';
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

export const DetailedMediaView = ({ mediaFile, isOpen, onClose, playMedia, isFavorite = false, onToggleFavorite, isLoadingDetails = false, adminModeEnabled = false }) => {
    const [adminActionStatus, setAdminActionStatus] = useState(null);
    const [isEditingMetadata, setIsEditingMetadata] = useState(false);
    const [metadataForm, setMetadataForm] = useState({});
    const posterInputRef = useRef(null);

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
    const resumePosition = canResume ? formatRuntime(mediaViews[0]?.position) : null;

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

    const handleSyncSubtitles = async () => {
        setAdminActionStatus({ type: 'loading', message: 'Syncing subtitles...' });
        try {
            const response = await fetch(`/admin/v1/media/${mediaFile.mediaFileId}/subtitles/sync`, {
                method: 'POST'
            });
            if (response.status === 202) {
                setAdminActionStatus({ type: 'success', message: 'Subtitle sync queued' });
            } else if (response.status === 400) {
                setAdminActionStatus({ type: 'error', message: 'Media not eligible for subtitles' });
            } else if (response.status === 409) {
                setAdminActionStatus({ type: 'error', message: 'Sync already in progress' });
            } else {
                setAdminActionStatus({ type: 'error', message: 'Failed to sync subtitles' });
            }
        } catch (err) {
            setAdminActionStatus({ type: 'error', message: 'Network error' });
        }
        setTimeout(() => setAdminActionStatus(null), 3000);
    };

    const handleRefreshMetadata = async () => {
        setAdminActionStatus({ type: 'loading', message: 'Refreshing metadata...' });
        try {
            const response = await fetch(`/admin/v1/media/${mediaFile.mediaFileId}/metadata/refresh`, {
                method: 'POST'
            });
            if (response.status === 202) {
                setAdminActionStatus({ type: 'success', message: 'Metadata refresh triggered' });
            } else if (response.status === 404) {
                setAdminActionStatus({ type: 'error', message: 'Media not found' });
            } else {
                setAdminActionStatus({ type: 'error', message: 'Failed to refresh metadata' });
            }
        } catch (err) {
            setAdminActionStatus({ type: 'error', message: 'Network error' });
        }
        setTimeout(() => setAdminActionStatus(null), 3000);
    };

    const handleReconvert = async () => {
        setAdminActionStatus({ type: 'loading', message: 'Queueing reconversion...' });
        try {
            const response = await fetch(`/admin/v1/media/${mediaFile.mediaFileId}/reconvert`, {
                method: 'POST'
            });
            if (response.status === 202) {
                setAdminActionStatus({ type: 'success', message: 'Reconversion queued' });
            } else if (response.status === 400) {
                setAdminActionStatus({ type: 'error', message: 'Media not eligible for reconversion' });
            } else if (response.status === 409) {
                setAdminActionStatus({ type: 'error', message: 'Conversion already in progress' });
            } else {
                setAdminActionStatus({ type: 'error', message: 'Failed to queue reconversion' });
            }
        } catch (err) {
            setAdminActionStatus({ type: 'error', message: 'Network error' });
        }
        setTimeout(() => setAdminActionStatus(null), 3000);
    };

    const openMetadataEditor = () => {
        setMetadataForm({
            title: media.title || '',
            releaseYear: media.releaseYear || '',
            genre: media.genre || '',
            plot: media.plot || '',
            actors: media.actors || '',
            imdbRating: media.imdbRating || '',
            metaRating: media.metaRating || ''
        });
        setIsEditingMetadata(true);
    };

    const handleMetadataChange = (field, value) => {
        setMetadataForm(prev => ({ ...prev, [field]: value }));
    };

    const handleSaveMetadata = async () => {
        setAdminActionStatus({ type: 'loading', message: 'Saving metadata...' });
        setIsEditingMetadata(false);
        try {
            const response = await fetch(`/admin/v1/media/${mediaFile.mediaFileId}/metadata`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(metadataForm)
            });
            if (response.ok) {
                setAdminActionStatus({ type: 'success', message: 'Metadata saved' });
            } else {
                setAdminActionStatus({ type: 'error', message: 'Failed to save metadata' });
            }
        } catch (err) {
            setAdminActionStatus({ type: 'error', message: 'Network error' });
        }
        setTimeout(() => setAdminActionStatus(null), 3000);
    };

    const handlePosterUpload = async (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        setAdminActionStatus({ type: 'loading', message: 'Uploading poster...' });
        try {
            const formData = new FormData();
            formData.append('file', file);
            const response = await fetch(`/admin/v1/media/${mediaFile.mediaFileId}/poster`, {
                method: 'POST',
                body: formData
            });
            if (response.ok) {
                setAdminActionStatus({ type: 'success', message: 'Poster uploaded' });
            } else {
                setAdminActionStatus({ type: 'error', message: 'Failed to upload poster' });
            }
        } catch (err) {
            setAdminActionStatus({ type: 'error', message: 'Network error' });
        }
        // Reset file input
        if (posterInputRef.current) {
            posterInputRef.current.value = '';
        }
        setTimeout(() => setAdminActionStatus(null), 3000);
    };

    return (
        <Dialog open={isOpen} onClose={onClose} className="detailed-media-dialog">
            <div className="detailed-media-backdrop" onClick={onClose} />
            <DialogPanel className="detailed-media-panel" onClick={onClose}>
                <div className="detailed-media-content" onClick={(e) => e.stopPropagation()}>
                    <button
                        className="detailed-media-close"
                        onClick={onClose}
                        aria-label="Close detailed view"
                    >
                        ✕
                    </button>

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
                                        ⏯ Resume{resumePosition ? ` at ${resumePosition}` : ''}
                                    </button>
                                )}
                            </div>
                        )}

                        {adminModeEnabled && (
                            <div className="detailed-media-admin">
                                <div className="detailed-media-admin__header">Admin Actions</div>
                                {adminActionStatus && (
                                    <div className={`detailed-media-admin__status detailed-media-admin__status--${adminActionStatus.type}`}>
                                        {adminActionStatus.message}
                                    </div>
                                )}
                                <div className="detailed-media-admin__actions">
                                    <button
                                        className="detailed-media-admin__btn"
                                        onClick={handleSyncSubtitles}
                                        disabled={adminActionStatus?.type === 'loading'}
                                    >
                                        Sync Subtitles
                                    </button>
                                    <button
                                        className="detailed-media-admin__btn"
                                        onClick={handleRefreshMetadata}
                                        disabled={adminActionStatus?.type === 'loading'}
                                    >
                                        Refresh Metadata
                                    </button>
                                    <button
                                        className="detailed-media-admin__btn"
                                        onClick={handleReconvert}
                                        disabled={adminActionStatus?.type === 'loading'}
                                    >
                                        Reconvert
                                    </button>
                                    <button
                                        className="detailed-media-admin__btn"
                                        onClick={openMetadataEditor}
                                        disabled={adminActionStatus?.type === 'loading'}
                                    >
                                        Edit Metadata
                                    </button>
                                    <button
                                        className="detailed-media-admin__btn"
                                        onClick={() => posterInputRef.current?.click()}
                                        disabled={adminActionStatus?.type === 'loading'}
                                    >
                                        Upload Poster
                                    </button>
                                    <input
                                        ref={posterInputRef}
                                        type="file"
                                        accept="image/*"
                                        onChange={handlePosterUpload}
                                        style={{ display: 'none' }}
                                    />
                                </div>
                            </div>
                        )}

                        {isEditingMetadata && (
                            <div className="detailed-media-metadata-editor">
                                <div className="detailed-media-metadata-editor__header">
                                    <h3>Edit Metadata</h3>
                                    <button
                                        className="detailed-media-metadata-editor__close"
                                        onClick={() => setIsEditingMetadata(false)}
                                    >
                                        ✕
                                    </button>
                                </div>
                                <div className="detailed-media-metadata-editor__form">
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>Title</span>
                                        <input
                                            type="text"
                                            value={metadataForm.title}
                                            onChange={(e) => handleMetadataChange('title', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>Year</span>
                                        <input
                                            type="text"
                                            value={metadataForm.releaseYear}
                                            onChange={(e) => handleMetadataChange('releaseYear', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>Genre</span>
                                        <input
                                            type="text"
                                            value={metadataForm.genre}
                                            onChange={(e) => handleMetadataChange('genre', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>IMDB Rating</span>
                                        <input
                                            type="text"
                                            value={metadataForm.imdbRating}
                                            onChange={(e) => handleMetadataChange('imdbRating', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>Metacritic</span>
                                        <input
                                            type="text"
                                            value={metadataForm.metaRating}
                                            onChange={(e) => handleMetadataChange('metaRating', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field">
                                        <span>Cast</span>
                                        <input
                                            type="text"
                                            value={metadataForm.actors}
                                            onChange={(e) => handleMetadataChange('actors', e.target.value)}
                                        />
                                    </label>
                                    <label className="detailed-media-metadata-editor__field detailed-media-metadata-editor__field--full">
                                        <span>Plot</span>
                                        <textarea
                                            value={metadataForm.plot}
                                            onChange={(e) => handleMetadataChange('plot', e.target.value)}
                                            rows={4}
                                        />
                                    </label>
                                </div>
                                <div className="detailed-media-metadata-editor__buttons">
                                    <button
                                        className="detailed-media-btn detailed-media-btn--secondary"
                                        onClick={() => setIsEditingMetadata(false)}
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        className="detailed-media-btn detailed-media-btn--primary"
                                        onClick={handleSaveMetadata}
                                    >
                                        Save
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </DialogPanel>
        </Dialog>
    );
};
