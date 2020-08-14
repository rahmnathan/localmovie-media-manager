import React from 'react';
import { buildPosterUri } from "./Media.jsx";

const videoBaseUri = '/localmovie/v3/media/';

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

const buildVideoPath = function (mediaFileId) {
    if(mediaFileId !== null) {
        return videoBaseUri + encodeURIComponent(mediaFileId) + "/stream.mp4";
    } else {
        return videoBaseUri;
    }
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
    }

    handleVideoMounted(element) {
        if (element !== null) {
            let position = element.target.duration * (this.props.videoStartPercent * .915);
            element.target.currentTime = isNaN(position) ? 0 : position;
        }
    };

    render() {
        return (
            <div style={videoPlayerStyle}>
                <video width="100%" controls poster={buildPosterUri(this.props.mediaFileId)} onLoadedMetadata={this.handleVideoMounted}>
                    <source src={buildVideoPath(this.props.mediaFileId)} type="video/mp4"/>
                </video>
                <button style={buttonStyle} onClick={() => {
                    window.history.back()
                }}>Exit Video
                </button>
                <div style={backgroundTintStyle}/>
            </div>
        );
    };
}
