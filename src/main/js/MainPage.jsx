import React from 'react';
import { MediaList } from './MediaList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';

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
        if (this.props.mediaPath !== prevProps.mediaPath) {
            this.loadMedia();
            this.setState({searchText: '', genre: 'all'})
        } else if (this.state.genre !== prevState.genre ||
            this.state.searchText !== prevState.searchText ||
            this.state.sort !== prevState.sort) {

            let resultMedia = [];
            if (this.state.originalMedia.has(this.props.mediaPath)) {
                resultMedia = this.state.originalMedia.get(this.props.mediaPath);
            }

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
                resultMedia = resultMedia.sort(function (media1, media2) {
                    if (media1 === null || media2 === null || media1.media === null || media2.media === null) {
                        return true;
                    }

                    switch (currentSort) {
                        case 'title':
                            return media1.media.title > media2.media.title;
                        case 'year':
                            return media1.media.releaseYear < media2.media.releaseYear;
                        case 'added':
                            return media1.created < media2.created;
                        case 'rating':
                            return media1.media.imdbRating < media2.media.imdbRating;
                        default:
                            return true;
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

    componentDidMount() {
        this.loadMedia();
    }

    loadMedia() {
        if(this.state.originalMedia.has(this.props.mediaPath)){
            this.setState({ media: this.state.originalMedia.get(this.props.mediaPath)});
        }

        trackPromise(
            fetch('/localmovie/v2/media', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(buildMovieRequest(this.props.mediaPath))
            }).then(response => response.json())
                .then(data => {
                    let originalMedia = this.state.originalMedia;
                    originalMedia.set(this.props.mediaPath, data);
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
                <MediaList media={this.state.media} selectMediaFile={this.props.selectMediaFile} playMedia={this.props.playMedia}/>
            </div>
        )
    }
}
