import React from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import ReactCSSTransitionGroup from 'react-addons-css-transition-group';
import {viewingVideos} from "./VideoPlayer.jsx";

const movieStyle = {
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    width: 150,
    padding: 3,
    height: 295,
    display: 'inline-block',
    margin: 8,
    verticalAlign: 'top',
    overflow: 'hidden'
};

const buttonDivStyle = {
    width: '100%',
    height: '11%',
    position: 'absolute',
    bottom: 0,
    left: 0
};

const playButtonStyle = {
    cursor: 'pointer',
    width: '50%',
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    align: 'left',
    height: '100%'
};

const resumeButtonStyle = {
    cursor: 'pointer',
    width: '50%',
    align: 'right',
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    height: '100%'
};

const textStyle = {
    color: 'white',
    fontSize: 14,
    wordWrap: 'normal',
    margin: 2,
    textAlign: 'center'
};

const titleStyle = {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 16,
    wordWrap: 'normal',
    margin: 2,
    textAlign: 'center',
    maxHeight: '15%',
    overflow: 'hidden'
};

const posterStyle = {
    height: '65%',
    width: '90%'
};

const hoveredMovieStyle = {
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    width: 210,
    padding: 3,
    height: 380,
    display: 'inline-block',
    margin: -20,
    marginBottom: -25,
    verticalAlign: 'top',
    zIndex: 5,
    position: 'relative',
    overflow: 'hidden'
};

const hoveredTitleStyle = {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 16,
    wordWrap: 'normal',
    margin: 2,
    textAlign: 'center',
    maxHeight: '11%',
    overflow: 'hidden'
};

const hoveredPosterStyle = {
    height: '62%',
    width: '90%'
};

const posterBasePath = '/localmovie/v3/media/';

export const buildPosterUri = function (id) {
    if(id === null){
        return 'noPicture.gif';
    } else {
        return posterBasePath + encodeURIComponent(id) + '/poster';
    }
};

export class Media extends React.Component {
    constructor(props) {
        super(props);
        this.state = ({ hovered: false });
        this.selectMedia = this.selectMedia.bind(this);
        this.handleHover = this.handleHover.bind(this);
        this.removeHover = this.removeHover.bind(this);
    }

    selectMedia(startPercent) {
        this.props.setPathAndStartPercent(this.props.media.path, startPercent, this.props.media.mediaFileId);
    }

    buildMedia() {
        let mediaFile = this.props.media;
        let media = mediaFile.media;

        let title = mediaFile.fileName.substr(0, mediaFile.fileName.length - 4);
        if(viewingVideos(mediaFile.path) && media.number != null) {
            if(media.title !== null && media.mediaType === "EPISODE"){
                title = "#" + media.number + " - " + media.title;
            } else {
                title = "#" + media.number;
            }
        } else if(media.title != null){
            title = media.title;
        }

        let year = 0;
        if (media.releaseYear !== null) {
            year = media.releaseYear;
        }
        let rating = '';
        if (media.imdbRating !== null) {
            rating = media.imdbRating;
        }

        let videoStartPercent = 0;
        if(mediaFile.mediaViews.length !== 0){
            videoStartPercent = mediaFile.mediaViews[0].position / mediaFile.length;
        }

        let currentPositionStyle = {
            width: ((videoStartPercent * 100) * .8) + '%',
            color: "#FF0000",
            backgroundColor: "#FF0000",
            height: 10,
            marginLeft: '5%'
        };

        if (this.state !== null && this.state.hovered) {
            if(viewingVideos(mediaFile.path)) {
                return (
                    <ReactCSSTransitionGroup
                        transitionName="fadein"
                        transitionAppear={true}
                        transitionAppearTimeout={500}
                        transitionEnter={true}
                        transitionLeave={true}>
                        <div style={hoveredMovieStyle} onMouseLeave={this.removeHover}>
                                <LazyLoadImage src={buildPosterUri(mediaFile.mediaFileId)} onError={(e) => {
                                    e.target.onerror = null;
                                    e.target.src = "noPicture.gif"
                                }} alt={title} style={hoveredPosterStyle} scrollPosition={this.props.scrollPosition} onClick={() => this.selectMedia(0)}/>
                                <div style={currentPositionStyle}/>
                                <p style={hoveredTitleStyle}>{title}</p>
                                <p style={textStyle}>Year: {year}</p>
                                <p style={textStyle}>IMDB: {rating}</p>
                                <div style={buttonDivStyle}>
                                    <button style={playButtonStyle} onClick={() => this.selectMedia(0)}>
                                        Play
                                    </button>
                                    <button style={resumeButtonStyle} onClick={() => this.selectMedia(videoStartPercent)}>
                                        Resume
                                    </button>
                                </div>
                        </div>
                    </ReactCSSTransitionGroup>
                )
            }

            return (
                <ReactCSSTransitionGroup
                    transitionName="fadein"
                    transitionAppear={true}
                    transitionAppearTimeout={500}
                    transitionEnter={true}
                    transitionLeave={true}>
                    <div style={hoveredMovieStyle} onMouseLeave={this.removeHover}>
                            <LazyLoadImage src={buildPosterUri(mediaFile.mediaFileId)} onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = "noPicture.gif"
                            }} alt={title} style={hoveredPosterStyle} scrollPosition={this.props.scrollPosition} onClick={() => this.selectMedia(0)}/>
                            <div style={currentPositionStyle}/>
                            <p style={hoveredTitleStyle}>{title}</p>
                            <p style={textStyle}>Year: {year}</p>
                            <p style={textStyle}>IMDB: {rating}</p>
                    </div>
                </ReactCSSTransitionGroup>
            )
        }

        return (
            <ReactCSSTransitionGroup
                transitionName="fadein"
                transitionAppear={true}
                transitionAppearTimeout={500}
                transitionEnter={true}
                transitionLeave={true}>
                <div style={movieStyle} onMouseEnter={this.handleHover}>
                        <LazyLoadImage onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}}
                                       src={buildPosterUri(mediaFile.mediaFileId)} alt={title} style={posterStyle}
                                       scrollPosition={this.props.scrollPosition} onClick={() => this.selectMedia(0)}/>
                        <div style={currentPositionStyle}/>
                        <p style={titleStyle}>{title}</p>
                        <p style={textStyle}>Year: {year}</p>
                        <p style={textStyle}>IMDB: {rating}</p>
                </div>
            </ReactCSSTransitionGroup>
        )
    }

    render() {
        return this.buildMedia();
    }

    handleHover() {
        this.setState( {hovered: true});
    }

    removeHover() {
        this.setState( {hovered: false});
    }
}