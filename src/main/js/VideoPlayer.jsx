import React, {useEffect} from 'react';
import { buildPosterUri } from "./Media.jsx";
import ReactPlayer from 'react-player'
import Cookies from 'universal-cookie';
import {trackPromise} from "react-promise-tracker";
import {useLocation, useNavigate, useNavigation, useParams, useSearchParams} from 'react-router-dom';

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

const buttonStyle = {
    color: 'gray',
    backgroundColor: 'black',
    borderColor: 'black'
};

export function VideoPlayer() {

    const [mediaFile, setMediaFile] = React.useState(null);
    let { mediaId } = useParams();

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
    });

    function saveProgress(content) {
        cookies.set('progress-' + mediaFile.mediaFileId, content.playedSeconds, {sameSite: 'strict'});

        fetch('/localmovie/v1/media/' + mediaFile.mediaFileId + '/position/' + content.playedSeconds, {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            }
        })
    };

    function buildVideoPath(mediaFileId) {
        let startPosition = 0;

        // if (props.startAtBeginning === false) {
        //     startPosition = cookies.get('progress-' + mediaFile.mediaFileId) || 0;
        //     if (mediaFile.mediaViews !== undefined && mediaFile.mediaViews.length !== 0) {
        //         startPosition = mediaFile.mediaViews[0].position;
        //     }
        // }

        return videoBaseUri + encodeURIComponent(mediaFileId) + "/stream.mp4#t=" + startPosition;
    };

    return (
        <div style={videoPlayerStyle}>
            <ReactPlayer
                url={buildVideoPath(mediaId)}
                config={{file: {attributes: {poster: buildPosterUri(mediaId)}}}}
                controls={true}
                width={'100%'}
                height={'100%'}
                onProgress={saveProgress}/>
            <div style={backgroundTintStyle}/>
        </div>
    );
}
