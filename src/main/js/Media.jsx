import React from 'react';
import { LazyLoadImage } from 'react-lazy-load-image-component';
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
    margin: 2,
    fontWeight: 'bold'
};

const ratingStyle = {
    color: 'white',
    fontSize: 14,
    wordWrap: 'normal',
    margin: 2,
    fontWeight: 'bold',
    marginTop: 6
};

const plotStyle = {
    color: 'white',
    fontSize: 14,
    wordWrap: 'normal',
    margin: 2,
    marginTop: '10px'
};

const titleStyle = {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 18,
    wordWrap: 'normal',
    margin: 2,
    maxHeight: '50%',
    overflow: 'hidden'
};

const posterStyle = {
    height: '100%',
    width: '45%',
    float: 'left'
};

const imdbIconStyle = {
    height: '15%',
    width: '15%',
    float: 'left'
};

const descriptionStyle = {
    textAlign: 'left',
    paddingLeft: '10px',
    width: '50%',
    height: 295,
    overflowY: 'scroll'
}

const posterBasePath = '/localmovie/v1/media/';

export const buildPosterUri = function (id) {
    if(id === null){
        return 'noPicture.gif';
    } else {
        return posterBasePath + encodeURIComponent(id) + '/poster';
    }
};

export function Media(props) {

    function selectMedia(mediaFile) {
        let path = mediaFile.path;
        if((path.includes("Movies") && path.split("/").length === 2) || path.split("/").length === 4){
            props.playMedia(mediaFile, true)
        } else {
            props.setPath(mediaFile.path)
        }
    }

    function buildMedia() {
        let mediaFile = props.media;
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
                <div style={movieStyle} onClick={() => selectMedia(mediaFile)}>
                        <LazyLoadImage onError={(e)=>{e.target.onerror = null; e.target.src="noPicture.gif"}}
                                       src={buildPosterUri(mediaFile.mediaFileId)} alt={title} style={posterStyle}
                                       scrollPosition={props.scrollPosition}/>
                        <div style={descriptionStyle} className='container'>
                            <p style={titleStyle}>{title}</p>
                            <p style={titleStyle}>{year}</p>
                            <div style={{ display: 'flex'}}>
                                <img src={'imdb.png'} alt={'IMDB'} style={imdbIconStyle}/>
                                <p style={ratingStyle}>{rating}</p>
                            </div>
                            <p style={textStyle}>{genre}</p>
                            <p style={plotStyle}>{plot}</p>
                        </div>
                </div>
        )
    }

    return buildMedia();
}