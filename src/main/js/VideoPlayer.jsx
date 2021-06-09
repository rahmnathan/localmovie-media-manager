import React from 'react';
import { buildPosterUri } from "./Media.jsx";
import ReactPlayer from 'react-player'
import Cookies from 'universal-cookie';
import {trackPromise} from "react-promise-tracker";

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
        this.saveProgress = this.saveProgress.bind(this);
        this.buildVideoPath = this.buildVideoPath.bind(this);
        this.state = {
            mediaFile: null
        }
    }

    componentDidMount() {
        trackPromise(
                fetch('/localmovie/v3/media/' + this.props.mediaFileId, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response=> response.json())
                    .then(media=> this.setState({mediaFile: media}))
        )
    }

    saveProgress(content) {
        cookies.set('progress-' + this.state.mediaFile.mediaFileId, content.playedSeconds, {sameSite: 'strict'});

        fetch('/localmovie/v3/media/' + this.state.mediaFile.mediaFileId + '/position/' + content.playedSeconds, {
            method: 'PATCH',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            }
        })
    };

    buildVideoPath(mediaFileId) {
        let startPosition = 0;

        // if(this.props.startAtBeginning === false){
        //     startPosition = cookies.get('progress-' + this.props.mediaFileId) || 0;
        //     if(this.state.mediaFile !== null && this.state.mediaFile.mediaViews !== undefined && this.state.mediaFile.mediaViews.length !== 0){
        //         startPosition = this.state.mediaFile.mediaViews[0].position;
        //     }
        // }

        return videoBaseUri + encodeURIComponent(mediaFileId) + "/stream.mp4#t=" + startPosition;
    };

    render() {
        return (
            <div style={videoPlayerStyle}>
                <ReactPlayer
                    url={this.buildVideoPath(this.props.mediaFileId)}
                    config={ { file: { attributes: { poster: buildPosterUri(this.props.mediaFileId) } } } }
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
