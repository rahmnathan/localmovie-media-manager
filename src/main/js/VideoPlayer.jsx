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

export const viewingVideos = function (path) {
    if(path === null){
        return false;
    }

    return (path.includes("Movies") && path.split("/").length === 2) || path.split("/").length === 4;
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

    useEffect(() => {
        const script = document.createElement('script');
        script.src = 'https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1';
        script.async = true;
        document.body.appendChild(script);

        script.onload = () => {
            if (window.cast) {
                initializeCast();
            }
        };

        return () => {
            document.body.removeChild(script);
        };
    }, []);

    const initializeCast = () => {
        const context = cast.framework.CastContext.getInstance();
        context.setOptions({
            receiverApplicationId: chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID
        });

        context.addEventListener(cast.framework.CastContextEventType.SESSION_STATE_CHANGED, (event) => {
            if (event.sessionState === cast.framework.SessionState.SESSION_STARTED) {
                console.log('Cast session started');
            } else if (event.sessionState === cast.framework.SessionState.SESSION_ENDED) {
                console.log('Cast session ended');
            }
        });
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

    useEffect(() => {
        if (!window.cast || !mediaFile || !url) return;

        const context = window.cast.framework.CastContext.getInstance();

        const handler = (event) => {
            if (event.sessionState === window.cast.framework.SessionState.SESSION_STARTED) {
                const session = context.getCurrentSession();
                if (session) {
                    playOnCast(
                        url,
                        mediaFile.media.title,
                        `${window.location.origin}${signedUrls.poster}`
                    );
                }
            }
        };

        context.addEventListener(
            window.cast.framework.CastContextEventType.SESSION_STATE_CHANGED,
            handler
        );

        return () => {
            context.removeEventListener(
                window.cast.framework.CastContextEventType.SESSION_STATE_CHANGED,
                handler
            );
        };
    }, [url, signedUrls, mediaFile]);

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
            console.warn('Cast framework not ready');
            return;
        }

        const session = window.cast.framework.CastContext.getInstance().getCurrentSession();
        if (!session) {
            console.log('No cast session active.');
            return;
        }

        const mediaInfo = new window.chrome.cast.media.MediaInfo(mediaUrl.split('#')[0], 'video/mp4');
        mediaInfo.metadata = new window.chrome.cast.media.MovieMediaMetadata();
        mediaInfo.metadata.title = title;
        mediaInfo.metadata.images = [{ url: imageUrl }];

        const request = new window.chrome.cast.media.LoadRequest(mediaInfo);
        session.loadMedia(request).then(
            () => console.log('Cast media loaded successfully'),
            (errorCode) => console.error('Error loading cast media', errorCode)
        );
    };

    if (mediaFile && signedUrls && url) {
        const hasCastSession = window.cast?.framework
            ? window.cast.framework.CastContext.getInstance().getCurrentSession()
            : null;

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

                {!hasCastSession && (
                    <ReactPlayer
                        url={url}
                        controls
                        width="100%"
                        height="100%"
                        onProgress={saveProgress}
                    />
                )}
                <div style={backgroundTintStyle}/>
            </div>
        );
    }

    return null;
}
