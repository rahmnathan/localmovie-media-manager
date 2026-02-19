import { useEffect, useState, useCallback } from 'react';
import { CAST } from '../constants.js';

/**
 * Custom hook for Chromecast functionality
 */
export function useChromecast({ onProgress, receiverApplicationId = CAST.RECEIVER_APP_ID }) {
    const [isCasting, setIsCasting] = useState(false);
    const [remotePlayer, setRemotePlayer] = useState(null);
    const [remotePlayerController, setRemotePlayerController] = useState(null);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [castError, setCastError] = useState(null);

    useEffect(() => {
        const script = document.createElement('script');
        script.src = 'https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1';
        script.async = true;
        document.body.appendChild(script);

        window['__onGCastApiAvailable'] = (isAvailable) => {
            if (isAvailable) {
                initializeCast();
            } else {
                console.warn('Google Cast API not available');
            }
        };

        return () => {
            if (document.body.contains(script)) {
                document.body.removeChild(script);
            }
        };
    }, []);

    const initializeCast = () => {
        console.log('Initializing Cast framework...');
        const context = cast.framework.CastContext.getInstance();

        context.setOptions({
            receiverApplicationId: receiverApplicationId || CAST.RECEIVER_APP_ID,
            autoJoinPolicy: chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED
        });

        let eventListeners = [];
        let activeController = null;

        context.addEventListener(
            cast.framework.CastContextEventType.SESSION_STATE_CHANGED,
            (event) => {
                console.log('Cast session state changed:', event.sessionState);
                if (event.sessionState === cast.framework.SessionState.SESSION_STARTED ||
                    event.sessionState === cast.framework.SessionState.SESSION_RESUMED) {
                    setIsCasting(true);
                    setCastError(null);

                    const player = new cast.framework.RemotePlayer();
                    const controller = new cast.framework.RemotePlayerController(player);
                    activeController = controller;
                    setRemotePlayer(player);
                    setRemotePlayerController(controller);

                    const timeChangeHandler = () => {
                        setCurrentTime(player.currentTime);
                        if (onProgress) {
                            onProgress({ playedSeconds: player.currentTime, durationSeconds: player.duration });
                        }
                    };

                    const durationChangeHandler = () => {
                        setDuration(player.duration);
                    };

                    controller.addEventListener(
                        cast.framework.RemotePlayerEventType.CURRENT_TIME_CHANGED,
                        timeChangeHandler
                    );

                    controller.addEventListener(
                        cast.framework.RemotePlayerEventType.DURATION_CHANGED,
                        durationChangeHandler
                    );

                    eventListeners = [
                        { type: cast.framework.RemotePlayerEventType.CURRENT_TIME_CHANGED, handler: timeChangeHandler },
                        { type: cast.framework.RemotePlayerEventType.DURATION_CHANGED, handler: durationChangeHandler }
                    ];

                    console.log('Remote player ready');
                }

                if (event.sessionState === cast.framework.SessionState.SESSION_ENDED) {
                    if (activeController && eventListeners.length > 0) {
                        eventListeners.forEach(({ type, handler }) => {
                            activeController.removeEventListener(type, handler);
                        });
                        eventListeners = [];
                    }
                    activeController = null;

                    setIsCasting(false);
                    setRemotePlayer(null);
                    setRemotePlayerController(null);
                    setCurrentTime(0);
                    setDuration(0);
                }
            }
        );
    };

    const playOnCast = useCallback((optionsOrMediaUrl, titleArg, imageUrlArg) => {
        const options = typeof optionsOrMediaUrl === 'object' && optionsOrMediaUrl !== null
            ? optionsOrMediaUrl
            : {
                mediaUrl: optionsOrMediaUrl,
                title: titleArg,
                imageUrl: imageUrlArg
            };

        const mediaUrl = options.mediaUrl;
        const title = options.title;
        const imageUrl = options.imageUrl;
        const subtitleUrl = options.subtitleUrl;
        const updatePositionUrl = options.updatePositionUrl;
        const mediaId = options.mediaId;

        if (!window.cast) {
            const error = 'Cast framework not ready';
            console.warn(error);
            setCastError(error);
            return;
        }

        const session = window.cast.framework.CastContext.getInstance().getCurrentSession();
        if (!session) {
            const error = 'No cast session active';
            console.log(error);
            setCastError(error);
            return;
        }

        let startTime = 0;
        const fragmentMatch = mediaUrl.match(/#t=(\d+)/);
        if (fragmentMatch) {
            startTime = parseInt(fragmentMatch[1], 10);
        }

        const mediaInfo = new window.chrome.cast.media.MediaInfo(mediaUrl.split('#')[0], 'video/mp4');
        mediaInfo.metadata = new window.chrome.cast.media.MovieMediaMetadata();
        mediaInfo.metadata.title = title;
        mediaInfo.metadata.images = [{ url: imageUrl }];
        mediaInfo.customData = {
            updatePositionUrl,
            'update-position-url': updatePositionUrl,
            mediaId,
            'media-id': mediaId
        };
        mediaInfo.metadata['update-position-url'] = updatePositionUrl;
        mediaInfo.metadata['media-id'] = mediaId;

        if (subtitleUrl) {
            const track = new window.chrome.cast.media.Track(1, window.chrome.cast.media.TrackType.TEXT);
            track.trackContentId = subtitleUrl;
            track.trackContentType = 'text/vtt';
            track.subtype = window.chrome.cast.media.TextTrackType.SUBTITLES;
            track.name = 'English';
            track.language = 'en-US';
            mediaInfo.tracks = [track];
        }

        const request = new window.chrome.cast.media.LoadRequest(mediaInfo);
        request.currentTime = startTime;
        request.customData = {
            updatePositionUrl,
            'update-position-url': updatePositionUrl,
            mediaId,
            'media-id': mediaId
        };

        session.loadMedia(request).then(
            () => {
                console.log('Cast media loaded successfully');
                setCastError(null);
            },
            (errorCode) => {
                const error = `Failed to load media on cast device: ${errorCode}`;
                console.error(error);
                setCastError(error);
            }
        );
    }, [receiverApplicationId]);

    const seek = useCallback((seekTo) => {
        if (remotePlayer && remotePlayerController) {
            remotePlayer.currentTime = seekTo;
            remotePlayerController.seek();
            setCurrentTime(seekTo);
        }
    }, [remotePlayer, remotePlayerController]);

    const playOrPause = useCallback(() => {
        if (remotePlayerController) {
            remotePlayerController.playOrPause();
        }
    }, [remotePlayerController]);

    const stopCasting = useCallback(() => {
        window.cast?.framework?.CastContext?.getInstance()?.endCurrentSession(true);
    }, []);

    return {
        isCasting,
        remotePlayer,
        currentTime,
        duration,
        castError,
        playOnCast,
        seek,
        playOrPause,
        stopCasting,
    };
}
