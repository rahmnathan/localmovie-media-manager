import React from 'react';
import { buildPosterUri } from "./Media.jsx";
import ReactPlayer from 'react-player'
import Cookies from 'universal-cookie';

const videoBaseUri = '/localmovie/v3/media/';

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

export class VideoPlayer extends React.Component {

    constructor(props) {
        super(props);
        this.handleVideoMounted = this.handleVideoMounted.bind(this);
        this.saveProgress = this.saveProgress.bind(this);
        this.buildVideoPath = this.buildVideoPath.bind(this);
    }

    handleVideoMounted(element) {
        if (element !== null) {
            let position = element.target.duration * (this.props.videoStartPercent * .915);
            element.target.currentTime = isNaN(position) ? 0 : position;
        }
    };

    saveProgress(content) {
        cookies.set('progress-' + this.props.mediaFileId, content.playedSeconds, {sameSite: 'strict'});
    };

    buildVideoPath(mediaFileId) {
        if(mediaFileId !== null) {
            return videoBaseUri + encodeURIComponent(mediaFileId) + "/stream.mp4#t=" + (cookies.get('progress-' + this.props.mediaFileId) || 0);
        } else {
            return videoBaseUri;
        }
    };

    render() {
        return (
            <div style={videoPlayerStyle}>
                <ReactPlayer
                    url={this.buildVideoPath(this.props.mediaFileId)}
                    light={buildPosterUri(this.props.mediaFileId)}
                    controls={true}
                    width={'100%'}
                    height={'100%'}
                    onProgress={this.saveProgress}/>
                <button style={buttonStyle} onClick={() => {
                    window.history.back()
                }}>Exit Video
                </button>
                <div style={backgroundTintStyle}/>
            </div>
        );
    };
}
