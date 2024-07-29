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
    order: 'TITLE',
    client: 'WEBAPP',
    q: '',
    page: 0,
    pageSize: 50
}

export function MainPage() {

    const [media, setMedia] = React.useState([]);
    const [totalCount, setTotalCount] = React.useState(0)

    const [searchParams] = useSearchParams();

    useEffect(() => {

        let currentPath = searchParams.get('path');
        let genre = searchParams.get('genre');
        let order = searchParams.get('order');
        let q = searchParams.get('q');

        if(currentPath === null || currentPath === undefined || currentPath === ''){
            currentPath = 'Movies';
        }

        navigationState.path = currentPath;
        navigationState.genre = genre;
        navigationState.order = order;
        navigationState.q = q;

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
                    setTotalCount(parseInt(response.headers.get("Count")))
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
                    setTotalCount(parseInt(response.headers.get("Count")))
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

    function filterMedia(searchText) {
        navigationState.page = 0;
        navigationState.q = searchText;
        loadMedia()
    }

    function filterMediaNavigate(searchText) {
        navigationState.q = searchText;
        search()
    }

    function setPath(path) {
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

        navigate({
            search: urlSearchParams.toString()
        })
    }

    return (
        <div style={layoutProps}>
            <ControlBar selectSort={selectSort} selectGenre={selectGenre} filterMedia={filterMedia} setPath={setPath} filterMediaNavigate={filterMediaNavigate}/>
            <MediaList media={media} setPath={setPath} playMedia={playMedia} nextPage={nextPage} hasMore={hasMore}/>
            <LoadingIndicator/>
        </div>
    )
}
