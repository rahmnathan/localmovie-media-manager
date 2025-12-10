import React, { useEffect, useState } from 'react';
import ReactPlayer from 'react-player';
import Cookies from 'universal-cookie';
import { trackPromise } from 'react-promise-tracker';
import { useParams } from 'react-router-dom';
import { Description, Dialog, DialogPanel, DialogTitle } from '@headlessui/react';

const cookies = new Cookies();

const backgroundTintStyle = {
    zIndex: -1,
    height: '100%',
    width: '100%',
    position: 'fixed',
    overflow: 'auto',
    top: 0,
    left: 0,
    background: 'rgba(0, 0, 0, 0.7)'
};

const videoPlayerStyle = {
    position: 'absolute',
    width: '80%',
    height: '80%',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    textAlign: 'center'
};

const dialogStyle = {
    position: 'absolute',
    width: '80%',
    height: '80%',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    textAlign: 'center',
    background: 'rgba(0, 0, 0, 0.7)',
    color: 'rgb(220,220,220)'
};

export function VideoPlayer() {
    const [mediaFile, setMediaFile] = useState(null);
    const [prompted, setPrompted] = useState(false);
    const [resumePlayback, setResumePlayback] = useState(false);
    const [canResumePlayback, setCanResumePlayback] = useState(false);
    const [url, setUrl] = useState('');
    const [signedUrls, setSignedUrls] = useState(null);
    const { mediaId } = useParams();
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
            document.body.removeChild(script);
        };
    }, []);

    const initializeCast = () => {
        console.log('Initializing Cast framework...');
        const context = cast.framework.CastContext.getInstance();

        context.setOptions({
            receiverApplicationId: chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID,
            autoJoinPolicy: chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED
        });

        // Store event listeners for cleanup
        let eventListeners = [];

        // Listen for session state changes
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
                    setRemotePlayer(player);
                    setRemotePlayerController(controller);

                    // Use local variables instead of state to avoid race condition
                    const timeChangeHandler = () => {
                        setCurrentTime(player.currentTime);
                        saveProgress({ playedSeconds: player.currentTime });
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

                    // Store listeners for cleanup
                    eventListeners = [
                        { type: cast.framework.RemotePlayerEventType.CURRENT_TIME_CHANGED, handler: timeChangeHandler },
                        { type: cast.framework.RemotePlayerEventType.DURATION_CHANGED, handler: durationChangeHandler }
                    ];

                    console.log('Remote player ready');
                }

                if (event.sessionState === cast.framework.SessionState.SESSION_ENDED) {
                    // Clean up event listeners
                    if (remotePlayerController && eventListeners.length > 0) {
                        eventListeners.forEach(({ type, handler }) => {
                            remotePlayerController.removeEventListener(type, handler);
                        });
                        eventListeners = [];
                    }

                    setIsCasting(false);
                    setRemotePlayer(null);
                    setRemotePlayerController(null);
                    setCurrentTime(0);
                    setDuration(0);
                }
            }
        );
    };

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

            if (canResumePlayback && resumePlayback) {
                const mediaView = mediaFile.mediaViews?.[0];
                startPosition = mediaView?.position || 0;
            }

            const newUrl = `${window.location.origin}${signedUrls.stream}#t=${startPosition}`;
            setUrl(newUrl);
        }
    }, [resumePlayback, mediaFile, signedUrls]);

    // Auto-play media when casting session starts
    useEffect(() => {
        if (!isCasting || !mediaFile || !url || !signedUrls) return;

        // Pause local playback
        const videoElem = document.querySelector('video');
        if (videoElem) videoElem.pause();

        // Start playing on cast device
        playOnCast(
            url,
            mediaFile.media.title,
            `${window.location.origin}${signedUrls.poster}`
        );
    }, [isCasting]);

    useEffect(() => {
        if (!mediaFile) return;

        const mediaViews = mediaFile.mediaViews;
        if (mediaViews?.length > 0) {
            const position = mediaViews[0]?.position || 0;
            setCanResumePlayback(position > 0);
        }
    }, [mediaFile]);

    const saveProgress = (content) => {
        cookies.set(`progress-${mediaId}`, content.playedSeconds, { sameSite: 'strict' });

        if (content.playedSeconds > 0) {
            fetch(
                `${signedUrls.updatePosition.split('?')[0]}/${content.playedSeconds}?${signedUrls.updatePosition.split('?')[1]}`,
                {
                    method: 'PATCH',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    }
                }
            );
        }
    };

    const playOnCast = (mediaUrl, title, imageUrl) => {
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

        // Extract start time from URL fragment if present
        let startTime = 0;
        const fragmentMatch = mediaUrl.match(/#t=(\d+)/);
        if (fragmentMatch) {
            startTime = parseInt(fragmentMatch[1], 10);
        }

        const mediaInfo = new window.chrome.cast.media.MediaInfo(mediaUrl.split('#')[0], 'video/mp4');
        mediaInfo.metadata = new window.chrome.cast.media.MovieMediaMetadata();
        mediaInfo.metadata.title = title;
        mediaInfo.metadata.images = [{ url: imageUrl }];

        const request = new window.chrome.cast.media.LoadRequest(mediaInfo);
        request.currentTime = startTime;

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
    };

    useEffect(() => {
        if (isCasting && currentTime > 0 && remotePlayer && signedUrls) {
            const timeout = setTimeout(() => {
                saveProgress({ playedSeconds: currentTime });
            }, 10000); // every 10s
            return () => clearTimeout(timeout);
        }
    }, [isCasting, currentTime]);

    const formatTime = (seconds) => {
        if (!seconds) return '0:00';
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    if (mediaFile && signedUrls && url) {
        return (
            <div style={videoPlayerStyle}>
                <Dialog open={!prompted && canResumePlayback} onClose={() => setPrompted(true)} style={dialogStyle}>
                    <DialogPanel>
                        <DialogTitle>{mediaFile.media.title}</DialogTitle>
                        <img src={`${window.location.origin}${signedUrls.poster}`} alt="" />
                        <Description>Pick up from where you left off, or start from the beginning!</Description>
                        <div>
                            <button onClick={() => {
                                setResumePlayback(false);
                                setPrompted(true);
                            }} style={{ marginRight: 5 }}>
                                Play
                            </button>
                            <button onClick={() => {
                                setResumePlayback(true);
                                setPrompted(true);
                            }}>
                                Resume
                            </button>
                        </div>
                    </DialogPanel>
                </Dialog>

                {!isCasting && (
                    <ReactPlayer
                        url={url}
                        controls
                        width="100%"
                        height="100%"
                        onProgress={saveProgress}
                    />
                )}

                {isCasting && (
                    <Dialog open={isCasting} onClose={() => {}} style={dialogStyle}>
                        <DialogPanel>
                            <DialogTitle>{mediaFile.media.title}</DialogTitle>
                            {castError && (
                                <div style={{ color: '#ff6b6b', marginBottom: '1rem' }}>
                                    Error: {castError}
                                </div>
                            )}
                            <p>Playing on {remotePlayer?.deviceFriendlyName || 'Chromecast device'}...</p>
                            <img src={`${window.location.origin}${signedUrls.poster}`} alt="" />
                            {/* Seek bar */}
                            <input
                                type="range"
                                min="0"
                                max={duration || 0}
                                value={currentTime || 0}
                                step="1"
                                style={{ width: '80%', margin: '1rem 0' }}
                                onChange={(e) => {
                                    const seekTo = Number(e.target.value);
                                    if (remotePlayer && remotePlayerController) {
                                        remotePlayer.currentTime = seekTo;
                                        remotePlayerController.seek();
                                        setCurrentTime(seekTo);
                                    }
                                }}
                            />

                            {/* Time display */}
                            <div>
                                {formatTime(currentTime)} / {formatTime(duration)}
                            </div>

                            {/* Controls */}
                            <div style={{ marginTop: '0.5rem' }}>
                                <button onClick={() => {
                                    if (remotePlayerController) {
                                        remotePlayerController.playOrPause();
                                    }
                                }}>
                                    {remotePlayer?.isPaused ? 'Resume' : 'Pause'}
                                </button>
                                <button
                                    onClick={() =>
                                        window.cast.framework.CastContext.getInstance().endCurrentSession(true)
                                    }
                                >
                                    Stop Casting
                                </button>
                            </div>
                        </DialogPanel>
                    </Dialog>
                )}
                <div style={backgroundTintStyle}/>
            </div>
        );
    }

    return null;
}
