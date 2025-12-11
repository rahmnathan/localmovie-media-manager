import React, {useEffect, useState, useCallback} from 'react';
import { MediaList } from './MediaList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import {createSearchParams, useNavigate, useSearchParams} from 'react-router-dom';
import {LoadingIndicator} from "./LoadingIndicator.jsx";
import {SkeletonLoader} from "./SkeletonCard.jsx";
import {UserPreferences} from "./userPreferences.js";

const layoutProps = {
    textAlign: 'center'
};

export function MainPage() {

    const [media, setMedia] = useState([]);
    const [totalCount, setTotalCount] = useState(0);
    const [navigationState, setNavigationState] = useState({
        path: 'Movies',
        genre: '',
        order: UserPreferences.getSortPreference(),
        client: 'WEBAPP',
        q: '',
        page: 0,
        pageSize: 50,
        type: 'movies'
    });
    const [searchParams] = useSearchParams();
    const [error, setError] = useState(null);
    const [isInitialLoad, setIsInitialLoad] = useState(true);

    const fetchMedia = useCallback((append = false, stateToUse = navigationState) => {
        setError(null);
        trackPromise(
            fetch('/localmovie/v1/media', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(stateToUse)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    if(stateToUse.page === 0) {
                        setTotalCount(parseInt(response.headers.get("Count")))
                    }
                    return response.json();
                })
                .then(data => {
                    setMedia(prev => append ? prev.concat(data) : data);
                    setIsInitialLoad(false);
                })
                .catch(error => {
                    console.error('Failed to fetch media:', error);
                    setError('Failed to load media. Please try again.');
                    setIsInitialLoad(false);
                })
        );
    }, []);

    useEffect(() => {
        let currentPath = searchParams.get('path');
        let genre = searchParams.get('genre');
        let order = searchParams.get('order');
        let q = searchParams.get('q');
        let type = searchParams.get('type')

        if(currentPath === null || currentPath === undefined || currentPath === ''){
            currentPath = 'Movies';
        }

        const newState = {
            ...navigationState,
            page: 0,
            path: currentPath,
            genre: genre,
            order: order,
            q: q,
            type: type
        };

        setNavigationState(newState);
        fetchMedia(false, newState);
    }, [searchParams, fetchMedia]);

    const navigate = useNavigate();

    // Restore scroll position on mount
    useEffect(() => {
        const savedScrollPosition = sessionStorage.getItem('mediaListScrollPosition');
        if (savedScrollPosition) {
            window.scrollTo(0, parseInt(savedScrollPosition));
            sessionStorage.removeItem('mediaListScrollPosition');
        }
    }, []);

    const hasMore = useCallback(() => {
        return totalCount !== media.length;
    }, [totalCount, media.length]);

    const playMedia = useCallback((media) => {
        // Save scroll position before navigating
        sessionStorage.setItem('mediaListScrollPosition', window.scrollY.toString());
        navigate("/play/" + media.mediaFileId);
    }, [navigate]);

    const nextPage = useCallback(() => {
        setNavigationState(prev => {
            const newState = {
                ...prev,
                page: prev.page + 1
            };
            fetchMedia(true, newState);
            return newState;
        });
    }, [fetchMedia]);

    const search = useCallback((order, genre, q, type, path) => {
        let urlSearchParams = createSearchParams();

        const setIfValid = (key, value, excludeValues = ['']) => {
            if (value && !excludeValues.includes(value)) {
                urlSearchParams.set(key, value);
            }
        };

        setIfValid('q', q);
        setIfValid('path', path);
        setIfValid('genre', genre, ['', 'all']);
        setIfValid('order', order);
        setIfValid('type', type, ['', 'movies']);

        navigate({
            search: urlSearchParams.toString()
        })
    }, [navigate]);

    const selectSort = useCallback((sort) => {
        UserPreferences.setSortPreference(sort);
        setNavigationState(prev => ({
            ...prev,
            order: sort,
            page: 0
        }));
        search(sort, navigationState.genre, navigationState.q, navigationState.type, navigationState.path);
    }, [search, navigationState.genre, navigationState.q, navigationState.type, navigationState.path]);

    const selectGenre = useCallback((genre) => {
        setNavigationState(prev => ({
            ...prev,
            genre: genre,
            page: 0
        }));
        search(navigationState.order, genre, navigationState.q, navigationState.type, navigationState.path);
    }, [search, navigationState.order, navigationState.q, navigationState.type, navigationState.path]);

    const filterMedia = useCallback((searchText) => {
        setNavigationState(prev => ({
            ...prev,
            q: searchText,
            page: 0
        }));
    }, []);

    const filterMediaNavigate = useCallback((searchText) => {
        search(navigationState.order, navigationState.genre, searchText, navigationState.type, navigationState.path);
    }, [search, navigationState.order, navigationState.genre, navigationState.type, navigationState.path]);

    const setType = useCallback((type) => {
        // Change type means a fresh view - reset filters
        setNavigationState(prev => ({
            ...prev,
            type: type,
            path: 'Movies',
            q: '',
            page: 0
        }));
        search(navigationState.order, navigationState.genre, '', type, 'Movies');
    }, [search, navigationState.order, navigationState.genre]);

    const setPath = useCallback((path) => {
        // Change path means a fresh view - reset filters
        setNavigationState(prev => ({
            ...prev,
            path: path,
            type: 'movies',
            q: '',
            page: 0
        }));
        search(navigationState.order, navigationState.genre, '', 'movies', path);
    }, [search, navigationState.order, navigationState.genre]);

    const showEmptyState = !error && !isInitialLoad && media.length === 0 && totalCount === 0;

    // Filter media for favorites view
    const displayMedia = navigationState.type === 'favorites'
        ? media.filter(item => UserPreferences.isFavorite(item.mediaFileId))
        : media;

    const showFavoritesEmpty = navigationState.type === 'favorites' && displayMedia.length === 0 && media.length > 0;

    return (
        <div style={layoutProps}>
            <ControlBar selectSort={selectSort} selectGenre={selectGenre} filterMedia={filterMedia} setPath={setPath} filterMediaNavigate={filterMediaNavigate} setType={setType}/>
            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}
            {isInitialLoad ? (
                <div style={{ margin: 10, paddingTop: 150, textAlign: 'center' }}>
                    <SkeletonLoader count={6} />
                </div>
            ) : showFavoritesEmpty ? (
                <div className="empty-state" role="status">
                    <h2>No favorites yet</h2>
                    <p>Click the heart icon on any media card to add it to your favorites!</p>
                </div>
            ) : showEmptyState ? (
                <div className="empty-state" role="status">
                    <h2>No media found</h2>
                    <p>
                        {navigationState.q
                            ? `No results for "${navigationState.q}". Try a different search term.`
                            : navigationState.genre && navigationState.genre !== 'all'
                            ? `No ${navigationState.genre} movies or shows found. Try selecting a different genre.`
                            : 'No media available in this category.'}
                    </p>
                </div>
            ) : (
                <MediaList media={displayMedia} setPath={setPath} playMedia={playMedia} nextPage={nextPage} hasMore={hasMore}/>
            )}
            <LoadingIndicator/>
        </div>
    )
}
