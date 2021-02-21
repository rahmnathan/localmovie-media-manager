import React from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
import ReactCSSTransitionGroup from 'react-addons-css-transition-group';
import {viewingVideos} from "./VideoPlayer.jsx";

const movieStyle = {
    borderStyle: 'solid',
    borderColor: '#2b2b2b',
    backgroundColor: 'rgb(21, 21, 30)',
    width: 375,
    padding: 3,
    height: 295,
    display: 'inline-block',
    margin: 8,
    verticalAlign: 'top',
    overflow: 'hidden'
};

const textStyle = {
    color: 'white',
    fontSize: 14,
    wordWrap: 'normal',
    margin: 2
};

const plotStyle = {
    color: 'white',
    fontSize: 14,
    wordWrap: 'normal',
    margin: 2
};

const ratingStyle = {
    color: 'white',
    fontSize: 14,
    margin: 2
};

const titleStyle = {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 16,
    wordWrap: 'normal',
    margin: 2,
    maxHeight: '15%',
    overflow: 'hidden'
};

const posterStyle = {
    height: '100%',
    width: '45%',
    float: 'left'
};

const imdbIconStyle = {
    height: '10%',
    width: '10%'
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
        this.state = ({});
        this.selectMedia = this.selectMedia.bind(this);
    }

    selectMedia(mediaFile) {
        this.props.selectMediaFile(mediaFile);
    }

    buildMedia() {
        let mediaFile = this.props.media;
        let media = mediaFile.media;
        let plot = media.plot;
        let genre = media.genre;

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
                                       scrollPosition={this.props.scrollPosition} onClick={() => this.selectMedia(mediaFile)}/>
                        <div>
                            <p style={titleStyle}>{title}</p>
                            <p style={textStyle}>{year}</p>
                            <div style={{ display: 'flex'}}>
                                <img src={'imdb.png'} alt={'IMDB'} style={imdbIconStyle}/>
                                <p style={ratingStyle}>{rating}</p>
                            </div>
                            <p style={textStyle}>{genre}</p>
                            <p style={plotStyle}>{plot}</p>
                        </div>
                </div>
            </ReactCSSTransitionGroup>
        )
    }

    render() {
        return this.buildMedia();
    }
}