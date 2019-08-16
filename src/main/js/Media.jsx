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
    verticalAlign: 'top'
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
    maxHeight: 40,
    overflow: 'hidden'
};

const posterStyle = {
    height: 200,
    width: 140
};

const hoveredMovieStyle = {
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    width: 210,
    padding: 3,
    height: 360,
    display: 'inline-block',
    margin: -20,
    marginBottom: -25,
    verticalAlign: 'top',
    zIndex: 5,
    position: 'relative'
};

const hoveredTitleStyle = {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 16,
    wordWrap: 'normal',
    margin: 2,
    textAlign: 'center',
    maxHeight: 60,
    overflow: 'hidden'
};

const hoveredPosterStyle = {
    height: 250,
    width: 190
};

const posterBasePath = '/localmovie/v2/media/poster?path=';

export const buildPosterUri = function (path) {
    if(path === null){
        return 'noPicture.gif';
    } else {
        return posterBasePath + encodeURIComponent(path);
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
        this.props.setPathAndStartPercent(this.props.media.path, startPercent);
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
            maxWidth: "90%",
            width: (videoStartPercent * 100) + "%",
            color: "#FF0000",
            backgroundColor: "#FF0000",
            height: 10
        };

        if (this.state !== null && this.state.hovered) {
            return (
                <ReactCSSTransitionGroup
                    transitionName="fadein"
                    transitionAppear={true}
                    transitionAppearTimeout={500}
                    transitionEnter={true}
                    transitionLeave={true}>
                    <div style={hoveredMovieStyle} onClick={() => this.selectMedia(videoStartPercent)} onMouseEnter={this.handleHover} onMouseLeave={this.removeHover}>
                        <div>
                            <LazyLoadImage src={buildPosterUri(mediaFile.path)} onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}} alt={title} style={hoveredPosterStyle} scrollPosition={this.props.scrollPosition}/>
                            <div style={currentPositionStyle}/>
                            <p style={hoveredTitleStyle}>{title}</p>
                            <p style={textStyle}>Year: {year}</p>
                            <p style={textStyle}>IMDB: {rating}</p>
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
                <div style={movieStyle} onClick={() => this.selectMedia(videoStartPercent)} onMouseEnter={this.handleHover} onMouseLeave={this.removeHover}>
                    <div>
                        <LazyLoadImage onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}} src={buildPosterUri(mediaFile.path)} alt={title} style={posterStyle} scrollPosition={this.props.scrollPosition}/>
                        <div style={currentPositionStyle}/>
                        <p style={titleStyle}>{title}</p>
                        <p style={textStyle}>Year: {year}</p>
                        <p style={textStyle}>IMDB: {rating}</p>
                    </div>
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