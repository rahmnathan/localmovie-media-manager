import React, { useEffect, useState, useRef, useCallback } from 'react';
import ReactPlayer from 'react-player';
import Cookies from 'universal-cookie';
import { trackPromise } from 'react-promise-tracker';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { getAutoplayEnabled } from './ControlBar.jsx';
import { useChromecast } from './hooks/useChromecast.js';

const cookies = new Cookies();

export function VideoPlayer() {
    const [mediaFile, setMediaFile] = useState(null);
    const [url, setUrl] = useState('');
    const [signedUrls, setSignedUrls] = useState(null);
    const { mediaId } = useParams();
    const [searchParams] = useSearchParams();
    const shouldResume = searchParams.get('resume') === 'true';
    const [duration, setDuration] = useState(0);
    const navigate = useNavigate();

    // Next episode auto-play state
    const [nextEpisode, setNextEpisode] = useState(null);
    const [showNextEpisodeOverlay, setShowNextEpisodeOverlay] = useState(false);
    const [countdown, setCountdown] = useState(10);
    const countdownRef = useRef(null);
    const playerRef = useRef(null);

    const saveProgress = useCallback((content) => {
        cookies.set(`progress-${mediaId}`, content.playedSeconds, { sameSite: 'strict' });

        if (!signedUrls || content.playedSeconds <= 0) {
            return;
        }

        // Build URL with duration parameter if available
        const baseUrl = signedUrls.updatePosition.split('?')[0];
        const queryParams = signedUrls.updatePosition.split('?')[1] || '';
        const durationParam = duration > 0 ? `&duration=${Math.round(duration)}` : '';
        const url = `${baseUrl}/${content.playedSeconds}?${queryParams}${durationParam}`;

        fetch(url, {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        });
    }, [mediaId, signedUrls, duration]);

    const {
        isCasting,
        remotePlayer,
        currentTime,
        duration: castDuration,
        castError,
        playOnCast,
        seek,
        playOrPause,
        stopCasting,
    } = useChromecast({});

    // Fetch media details
    useEffect(() => {
        trackPromise(
            fetch(`/localmovie/v1/media/${mediaId}`, {
                method: 'GET',
                headers: {
                    Accept: 'application/json'
                }
            }).then(response => response.json()).then(media => setMediaFile(media))
        );
    }, [mediaId]);

    // Fetch signed URLs
    useEffect(() => {
        trackPromise(
            fetch(`/localmovie/v1/media/${mediaId}/url/signed`, {
                method: 'GET',
                headers: {
                    Accept: 'application/json',
                }
            }).then(response => response.json()).then(urls => setSignedUrls(urls)))
    }, [mediaId]);

    useEffect(() => {
        if (mediaFile && signedUrls) {
            let startPosition = 0;

            if (shouldResume) {
                const mediaView = mediaFile.mediaViews?.[0];
                startPosition = mediaView?.position || 0;
            }

            const newUrl = `${window.location.origin}${signedUrls.stream}#t=${startPosition}`;
            setUrl(newUrl);
        }
    }, [shouldResume, mediaFile, signedUrls]);

    // Fetch next episode if current media is an episode
    useEffect(() => {
        if (!mediaFile) return;

        const mediaType = mediaFile.media?.mediaType;
        const episodeNumber = mediaFile.media?.number;

        // Only fetch next episode if this is an episode
        if (mediaType !== 'EPISODE' || !episodeNumber) {
            setNextEpisode(null);
            return;
        }

        // Get the parent (season) to find the next episode
        const parentId = mediaFile.parent?.mediaFileId;
        if (!parentId) return;

        // Fetch episodes in this season
        fetch('/localmovie/v1/media', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                parentId: parentId,
                page: 0,
                pageSize: 100,
                order: 'title',
                client: 'WEBAPP'
            })
        })
        .then(response => response.json())
        .then(episodes => {
            // Find the current episode index and get the next one
            const currentIndex = episodes.findIndex(ep => ep.mediaFileId === mediaId);
            if (currentIndex >= 0 && currentIndex < episodes.length - 1) {
                setNextEpisode(episodes[currentIndex + 1]);
            } else {
                setNextEpisode(null);
            }
        })
        .catch(err => {
            console.error('Failed to fetch next episode:', err);
            setNextEpisode(null);
        });
    }, [mediaFile, mediaId]);

    // Auto-play media when casting session starts
    useEffect(() => {
        if (!isCasting || !mediaFile || !url || !signedUrls) return;

        // Pause local playback
        const videoElem = document.querySelector('video');
        if (videoElem) videoElem.pause();

        // Start playing on cast device
        playOnCast({
            mediaUrl: url,
            title: mediaFile.media.title,
            imageUrl: `${window.location.origin}${signedUrls.poster}`,
            mimeType: signedUrls.streamContentType,
            subtitleUrl: signedUrls.subtitle ? `${window.location.origin}${signedUrls.subtitle}` : null,
            updatePositionUrl: signedUrls.updatePosition,
            mediaId
        });
    }, [isCasting, mediaFile, url, signedUrls, playOnCast, mediaId]);

    const formatTime = (seconds) => {
        if (!seconds) return '0:00';
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    // Start countdown when video ends and there's a next episode
    const onVideoEnded = useCallback(() => {
        if (!nextEpisode) return;

        // Check if auto-play is enabled
        if (!getAutoplayEnabled()) return;

        setShowNextEpisodeOverlay(true);
        setCountdown(10);

        // Clear any existing countdown
        if (countdownRef.current) {
            clearInterval(countdownRef.current);
        }

        countdownRef.current = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) {
                    clearInterval(countdownRef.current);
                    playNextEpisode();
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    }, [nextEpisode]);

    const playNextEpisode = useCallback(() => {
        if (!nextEpisode) return;

        // Clear countdown
        if (countdownRef.current) {
            clearInterval(countdownRef.current);
        }
        setShowNextEpisodeOverlay(false);

        // Navigate to next episode
        navigate(`/play/${nextEpisode.mediaFileId}`);
    }, [nextEpisode, navigate]);

    const cancelNextEpisode = useCallback(() => {
        if (countdownRef.current) {
            clearInterval(countdownRef.current);
        }
        setShowNextEpisodeOverlay(false);
    }, []);

    // Cleanup countdown on unmount
    useEffect(() => {
        return () => {
            if (countdownRef.current) {
                clearInterval(countdownRef.current);
            }
        };
    }, []);

    if (mediaFile && signedUrls && url) {
        return (
            <div className="video-player">
                {!isCasting && (
                    <ReactPlayer
                        ref={playerRef}
                        url={url}
                        controls
                        width="100%"
                        height="100%"
                        onProgress={saveProgress}
                        onEnded={onVideoEnded}
                        onDuration={(d) => setDuration(d)}
                        config={{
                            file: {
                                forceVideo: true,
                                tracks: signedUrls?.subtitle ? [
                                    {
                                        kind: 'subtitles',
                                        src: `${window.location.origin}${signedUrls.subtitle}`,
                                        srcLang: 'en',
                                        label: 'English',
                                        default: false
                                    }
                                ] : []
                            }
                        }}
                    />
                )}

                {/* Next Episode Overlay */}
                {showNextEpisodeOverlay && nextEpisode && (
                    <div className="next-episode-overlay">
                        <div className="next-episode-card">
                            <div className="next-episode-header">
                                <span className="next-episode-label">Up Next</span>
                                <button className="next-episode-close" onClick={cancelNextEpisode}>✕</button>
                            </div>
                            <div className="next-episode-title">
                                {nextEpisode.media?.number ? `E${nextEpisode.media.number} - ` : ''}
                                {nextEpisode.media?.title || nextEpisode.fileName}
                            </div>
                            <div className="next-episode-countdown">
                                Playing in {countdown}...
                            </div>
                            <button className="next-episode-play-btn" onClick={playNextEpisode}>
                                Play Now
                            </button>
                        </div>
                    </div>
                )}

                {isCasting && (
                    <Dialog open={isCasting} onClose={() => {}} className="cast-dialog">
                        <DialogPanel className="cast-dialog__panel">
                            <DialogTitle className="cast-dialog__title">{mediaFile.media.title}</DialogTitle>
                            {castError && (
                                <div className="cast-dialog__error">
                                    Error: {castError}
                                </div>
                            )}
                            <p className="cast-dialog__device">Playing on {remotePlayer?.deviceFriendlyName || 'Chromecast device'}</p>
                            <img
                                src={`${window.location.origin}${signedUrls.poster}`}
                                alt=""
                                className="cast-dialog__poster"
                            />
                            {/* Seek bar */}
                            <input
                                type="range"
                                min="0"
                                max={castDuration || 0}
                                value={currentTime || 0}
                                step="1"
                                className="cast-dialog__seek"
                                onChange={(e) => seek(Number(e.target.value))}
                            />

                            {/* Time display */}
                            <div className="cast-dialog__time">
                                {formatTime(currentTime)} / {formatTime(castDuration)}
                            </div>

                            {/* Controls */}
                            <div className="cast-dialog__controls">
                                <button
                                    className="cast-dialog__btn cast-dialog__btn--primary"
                                    onClick={playOrPause}
                                >
                                    {remotePlayer?.isPaused ? '▶ Resume' : '⏸ Pause'}
                                </button>
                                <button
                                    className="cast-dialog__btn cast-dialog__btn--stop"
                                    onClick={stopCasting}
                                >
                                    Stop Casting
                                </button>
                            </div>
                        </DialogPanel>
                    </Dialog>
                )}
                <div className="video-player__backdrop"/>
            </div>
        );
    }

    return null;
}
