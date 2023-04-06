import React from 'react';
import { MediaList } from './MediaList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import * as queryString from "query-string";

const layoutProps = {
    textAlign: 'center'
};

const buildMovieRequest = function (path) {
    return {
        path: path,
        client: "WEBAPP",
        page: 0,
        resultsPerPage: 1000,
        order: "TITLE"
    }
};

export class MainPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            media: [],
            originalMedia: new Map(),
            genre: 'all',
            searchText: '',
            sort: 'title'
        };

        this.filterMedia = this.filterMedia.bind(this);
        this.selectGenre = this.selectGenre.bind(this);
        this.selectSort = this.selectSort.bind(this);
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        let currentPath = 'Movies'
        let previousPath = 'Movies'

        console.log('MainPage updated.')

        if(currentPath === undefined) {
            currentPath = 'Movies';
            previousPath = 'Movies'
        }

        if(!this.state.originalMedia.has(currentPath)){
            console.log('Loading new media.')
            this.loadMedia(currentPath);
        } else if (this.state.genre !== prevState.genre ||
            this.state.searchText !== prevState.searchText ||
            this.state.sort !== prevState.sort ||
            currentPath !== previousPath) {
            console.log('Found update to state impacting media view.')

            if (currentPath !== previousPath) {
                // Reset search-text and genre
                // this.setState({searchText: '', genre: 'all'})
            }

            let resultMedia = this.state.originalMedia.get(currentPath);

            let currentGenre = this.state.genre;
            if (currentGenre !== null && currentGenre !== 'all') {
                resultMedia = resultMedia.filter(function (media) {
                    if (media.media === null || media.media.genre === null) {
                        return false;
                    }

                    return media.media.genre.toLowerCase().includes(currentGenre);
                });
            }

            let currentSearchText = this.state.searchText;
            if (currentSearchText !== null && currentSearchText !== '') {
                resultMedia = resultMedia.filter(function (media) {
                    if (media.media === null || media.media.title === null) {
                        return false;
                    }

                    return media.media.title.toLowerCase().includes(currentSearchText);
                });
            }

            let currentSort = this.state.sort;
            if (currentSort !== null) {
                resultMedia.sort(function (media1, media2) {
                    if (media1 === null || media2 === null || media1.media === null || media2.media === null) {
                        return 1;
                    }

                    switch (currentSort) {
                        case 'title':
                            return media1.media.title > media2.media.title ? 1 : -1;
                        case 'year':
                            return media1.media.releaseYear < media2.media.releaseYear ? 1 : -1;
                        case 'added':
                            return media1.created < media2.created ? 1 : -1;
                        case 'rating':
                            return media1.media.imdbRating < media2.media.imdbRating ? 1 : -1;
                        default:
                            return 0;
                    }
                });
            }

            this.setState({media: resultMedia})
        }
    }

    selectSort(sort){
        if(sort !== null){
            this.setState({sort: sort})
        }
    }

    selectGenre(genre){
        this.setState({genre: genre})
    }

    setPath(path) {
        this.setState({path: path})
    }

    playMedia(media) {

    }

    componentDidMount() {
        this.setState({path: 'Movies'})

        this.loadMedia('Movies');
    }

    loadMedia(path) {
        trackPromise(
            fetch('/localmovie/v1/media', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(buildMovieRequest(path))
            }).then(response => response.json())
                .then(data => {
                    let originalMedia = this.state.originalMedia;
                    originalMedia.set(path, data);
                    this.setState({media: data, originalMedia: originalMedia})
                })
        );
    }

    filterMedia(searchText){
        this.setState({searchText: searchText})
    }

    render() {
        return (
            <div style={layoutProps}>
                <ControlBar selectSort={this.selectSort} selectGenre={this.selectGenre} filterMedia={this.filterMedia} setPath={this.props.setPath}/>
                <MediaList media={this.state.media} setPath={this.setPath} playMedia={this.playMedia}/>
            </div>
        )
    }
}
