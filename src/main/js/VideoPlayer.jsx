import React, {useEffect} from 'react';
import { buildPosterUri } from "./Media.jsx";
import ReactPlayer from 'react-player';
import Cookies from 'universal-cookie';
import {trackPromise} from "react-promise-tracker";
import {useParams} from 'react-router-dom';
import {Description, Dialog, DialogPanel, DialogTitle} from "@headlessui/react";

const videoBaseUri = '/localmovie/v1/media/';

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

    const [mediaFile, setMediaFile] = React.useState(null);
    const [token, setToken] = React.useState(null);
    const [prompted, setPrompted] = React.useState(false);
    const [resumePlayback, setResumePlayback] = React.useState(false);
    const [canResumePlayback, setCanResumePlayback] = React.useState(false);
    const [url, setUrl] = React.useState('');
    let { mediaId } = useParams();

    useEffect(() => {
        const script = document.createElement("script");
        script.src = "https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1";
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
                console.log("Cast session started");
            } else if (event.sessionState === cast.framework.SessionState.SESSION_ENDED) {
                console.log("Cast session ended");
            }
        });
    };

    useEffect(() => {
        trackPromise(
            fetch('/localmovie/v1/media/token', {
                method: 'GET'
            }).then(response => response.text())
                .then(token => setToken(token))
        )
    }, []);

    useEffect(() => {
        trackPromise(
            fetch('/localmovie/v1/media/' + mediaId, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response => response.json())
                .then(media => setMediaFile(media))
        )
    }, []);

    useEffect(() => {
        if (mediaFile === null || mediaFile === undefined) return;

        let startPosition = 0;

        if (canResumePlayback && resumePlayback) {
            let mediaView = mediaFile.mediaViews[0];
            let position = mediaView.position;
            startPosition = position;

            console.log("position: " + position);
        }

        setUrl(
            window.location.origin +
            videoBaseUri +
            encodeURIComponent(mediaFile.mediaFileId) +
            "/stream.mp4?access_token=" +
            token +
            "#t=" +
            startPosition
        );
    }, [resumePlayback, mediaFile, token]);

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
                        buildPosterUri(mediaId) + "?access_token=" + token
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
    }, [url, mediaFile, token, mediaId]);

    useEffect(() => {
        if (mediaFile === null || mediaFile === undefined) return;

        let mediaViews = mediaFile.mediaViews;
        if(mediaViews !== null && mediaViews !== undefined && mediaViews.length !== 0) {
            let mediaView = mediaViews[0];
            let position = mediaView.position;
            setCanResumePlayback(parseFloat(position) > 0)
        }
    }, [mediaFile]);

    function saveProgress(content) {
        cookies.set('progress-' + mediaId, content.playedSeconds, {sameSite: 'strict'});

        if(content.playedSeconds > 0) {
            fetch('/localmovie/v1/media/' + mediaId + '/position/' + content.playedSeconds, {
                method: 'PATCH',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                }
            })
        }
    }

    const playOnCast = (mediaUrl, title, imageUrl) => {
        if (!window.cast) {
            console.warn("Cast framework not ready");
            return;
        }

        const session = window.cast.framework.CastContext.getInstance().getCurrentSession();
        if (!session) {
            console.log("No cast session active.");
            return;
        }

        const mediaInfo = new window.chrome.cast.media.MediaInfo(mediaUrl, "video/mp4");
        mediaInfo.metadata = new window.chrome.cast.media.MovieMediaMetadata();
        mediaInfo.metadata.title = title;
        mediaInfo.metadata.images = [{ url: imageUrl }];

        const request = new window.chrome.cast.media.LoadRequest(mediaInfo);
        session.loadMedia(request).then(
            () => console.log("Cast media loaded successfully"),
            (errorCode) => console.error("Error loading cast media", errorCode)
        );
    };

    if(mediaFile !== null && mediaFile !== undefined) {
        const hasCastSession = window.cast?.framework
            ? window.cast.framework.CastContext.getInstance().getCurrentSession()
            : null;

        return (
            <div style={videoPlayerStyle}>
                <Dialog open={!prompted && canResumePlayback}
                        onClose={() => setPrompted(true)}
                        style={dialogStyle}>
                    <div>
                        <DialogPanel>
                            <DialogTitle>{mediaFile.media.title}</DialogTitle>
                            <img src={buildPosterUri(mediaId)} alt=''/>
                            <Description>Pick up from where you left off, or start from the beginning!</Description>
                            <div>
                                <button onClick={() => {
                                    setResumePlayback(false)
                                    setPrompted(true)
                                }}
                                        style={{marginRight: 5}}>Play
                                </button>
                                <button onClick={() => {
                                    setResumePlayback(true)
                                    setPrompted(true)
                                }}>Resume
                                </button>
                            </div>
                        </DialogPanel>
                    </div>
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
        )
    }
}
