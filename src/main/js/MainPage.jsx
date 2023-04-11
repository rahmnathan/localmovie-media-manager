import React, {useEffect} from 'react';
import { MediaList } from './MediaList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import {useNavigate, useSearchParams} from 'react-router-dom';

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

export function MainPage() {

    const [media, setMedia] = React.useState([]);
    const [originalMedia, setOriginalMedia] = React.useState(new Map());
    const [genre, setGenre] = React.useState('all');
    const [searchText, setSearchText] = React.useState('');
    const [sort, setSort] = React.useState('title');
    const [searchParams] = useSearchParams();

    useEffect(() => {

        let currentPath = searchParams.get('path');
        if(currentPath === null | currentPath === undefined || currentPath === ''){
            currentPath = 'Movies';
        }
        console.log('Current path: ' + currentPath);

        console.log('MainPage updated.')

        if (!originalMedia.has(currentPath)) {
            loadMedia(currentPath);
        } else {
            console.log('Using cached media.')

            let resultMedia = originalMedia.get(currentPath);

            let currentGenre = genre;
            if (currentGenre !== null && currentGenre !== 'all') {
                resultMedia = resultMedia.filter(function (media) {
                    if (media.media === null || media.media.genre === null) {
                        return false;
                    }

                    return media.media.genre.toLowerCase().includes(currentGenre);
                });
            }

            let currentSearchText = searchText;
            if (currentSearchText !== null && currentSearchText !== '') {
                resultMedia = resultMedia.filter(function (media) {
                    if (media.media === null || media.media.title === null) {
                        return false;
                    }

                    return media.media.title.toLowerCase().includes(currentSearchText);
                });
            }

            let currentSort = sort;
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

            setMedia(resultMedia);
        }
    }, [genre, searchText, sort, originalMedia, searchParams]);

    function loadMedia(path) {
        console.log('Loading media for path: ' + path);
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
                    originalMedia.set(path, data);
                    setMedia(data);
                    setOriginalMedia(originalMedia);
                })
        );
    }

    const navigate = useNavigate();

    function playMedia(media) {
        navigate("/play/" + media.mediaFileId);
    }

    function selectSort(sort) {
        setSort(sort);
    }

    function selectGenre(genre) {
        setGenre(genre);
    }

    function filterMedia(searchText) {
        setSearchText(searchText);
    }

    function setPath(path) {
        console.log('setting path to ' + path);
        navigate({
            search: '?path=' + path
        })
    }

    return (
        <div style={layoutProps}>
            <ControlBar selectSort={selectSort} selectGenre={selectGenre} filterMedia={filterMedia} setPath={setPath}/>
            <MediaList media={media} setPath={setPath} playMedia={playMedia}/>
        </div>
    )
}
