import React, {useEffect} from 'react';
import { MediaList } from './MediaList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import {createSearchParams, useNavigate, useSearchParams} from 'react-router-dom';
import {LoadingIndicator} from "./LoadingIndicator.jsx";

const layoutProps = {
    textAlign: 'center'
};

const navigationState = {
    path: 'Movies',
    genre: '',
    order: 'title',
    client: 'WEBAPP',
    q: '',
    page: 0,
    pageSize: 50,
    type: 'movies'
}

export function MainPage() {

    const [media, setMedia] = React.useState([]);
    const [searchParams] = useSearchParams();

    let totalCount = 0;

    useEffect(() => {

        let currentPath = searchParams.get('path');
        let genre = searchParams.get('genre');
        let order = searchParams.get('order');
        let q = searchParams.get('q');
        let type = searchParams.get('type')

        if(currentPath === null || currentPath === undefined || currentPath === ''){
            currentPath = 'Movies';
        }

        navigationState.page = 0;
        navigationState.path = currentPath;
        navigationState.genre = genre;
        navigationState.order = order;
        navigationState.q = q;
        navigationState.type = type;

        loadMedia();
    }, [searchParams]);

    function loadMedia() {
        trackPromise(
            fetch('/localmovie/v1/media', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(navigationState)
            }).then(response => {
                if(navigationState.page === 0) {
                    totalCount = parseInt(response.headers.get("Count"))
                }
                return response.json()
            })
                .then(data => {
                    setMedia(data);
                })
        );
    }

    function loadMoreMedia() {
        trackPromise(
            fetch('/localmovie/v1/media', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(navigationState)
            }).then(response => {
                if(navigationState.page === 0) {
                    totalCount = parseInt(response.headers.get("Count"))
                }
                return response.json();
            })
                .then(data => {
                    setMedia(media.concat(data));
                })
        );
    }

    const navigate = useNavigate();

    function hasMore() {
        return totalCount !== media.length;
    }

    function playMedia(media) {
        navigate("/play/" + media.mediaFileId);
    }

    function nextPage(){
        navigationState.page = navigationState.page + 1;
        loadMoreMedia()
    }

    function selectSort(sort) {
        navigationState.order = sort;
        search()
    }

    function selectGenre(genre) {
        navigationState.genre = genre;
        search()
    }

    function resetSearchParams() {
        navigationState.type = 'movies'
        navigationState.path = 'Movies'
    }

    function filterMedia(searchText) {
        navigationState.page = 0;
        navigationState.q = searchText;
        loadMedia()
    }

    function filterMediaNavigate(searchText) {
        navigationState.q = searchText;
        search()
    }

    function setType(type) {
        // Change type means a fresh view - reset filters
        resetSearchParams()

        navigationState.type = type;
        search()
    }

    function setPath(path) {
        // Change path means a fresh view - reset filters
        resetSearchParams()

        navigationState.path = path;
        search()
    }

    function search() {
        let urlSearchParams = createSearchParams();
        navigationState.page = 0;

        if(navigationState.q !== null && navigationState.q !== undefined && navigationState.q !== '') {
            urlSearchParams.set('q', navigationState.q);
        }
        if(navigationState.path !== null && navigationState.path !== undefined && navigationState.path !== ''){
            urlSearchParams.set('path', navigationState.path);
        }
        if(navigationState.genre !== null && navigationState.genre !== undefined && navigationState.genre !== '' && navigationState.genre !== 'all'){
            urlSearchParams.set('genre', navigationState.genre);
        }
        if(navigationState.order !== null && navigationState.order !== undefined && navigationState.order !== ''){
            urlSearchParams.set('order', navigationState.order);
        }
        if(navigationState.type !== null && navigationState.type !== undefined && navigationState.type !== '' && navigationState.type !== 'movies'){
            urlSearchParams.set('type', navigationState.type);
        }

        navigate({
            search: urlSearchParams.toString()
        })
    }

    return (
        <div style={layoutProps}>
            <ControlBar selectSort={selectSort} selectGenre={selectGenre} filterMedia={filterMedia} setPath={setPath} filterMediaNavigate={filterMediaNavigate} setType={setType}/>
            <MediaList media={media} setPath={setPath} playMedia={playMedia} nextPage={nextPage} hasMore={hasMore}/>
            <LoadingIndicator/>
        </div>
    )
}
