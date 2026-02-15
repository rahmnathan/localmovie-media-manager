import React, {useEffect, useState, useCallback} from 'react';
import { MediaList } from './MediaList.jsx';
import { HistoryList } from './HistoryList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import {createSearchParams, useNavigate, useSearchParams} from 'react-router-dom';
import {LoadingIndicator} from "./LoadingIndicator.jsx";
import {SkeletonLoader} from "./SkeletonCard.jsx";
import {UserPreferences} from "./userPreferences.js";

const FAVORITES_TYPE = 'FAVORITES';
const HISTORY_TYPE = 'HISTORY';

const layoutProps = {
    textAlign: 'center'
};

export function MainPage() {

    const [media, setMedia] = useState([]);
    const [totalCount, setTotalCount] = useState(0);
    const [navigationState, setNavigationState] = useState({
        type: 'MOVIES',
        parentId: null,
        genre: '',
        order: UserPreferences.getSortPreference(),
        client: 'WEBAPP',
        q: '',
        page: 0,
        pageSize: 50
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
        let type = searchParams.get('type');
        let parentId = searchParams.get('parentId');
        let genre = searchParams.get('genre');
        let order = searchParams.get('order');
        let q = searchParams.get('q');

        // Default to MOVIES if no type is specified
        if(type === null || type === undefined || type === ''){
            type = 'MOVIES';
        }

        const newState = {
            type: type,
            parentId: parentId,
            genre: genre,
            order: order,
            q: q,
            page: 0,
            pageSize: 50,
            client: 'WEBAPP'
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

    const playMedia = useCallback((media, shouldResume = false) => {
        // Save scroll position before navigating
        sessionStorage.setItem('mediaListScrollPosition', window.scrollY.toString());
        navigate("/play/" + media.mediaFileId + (shouldResume ? "?resume=true" : ""));
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

    const search = useCallback((order, genre, q, type, parentId) => {
        let urlSearchParams = createSearchParams();

        const setIfValid = (key, value, excludeValues = ['']) => {
            if (value && !excludeValues.includes(value)) {
                urlSearchParams.set(key, value);
            }
        };

        setIfValid('q', q);
        setIfValid('parentId', parentId);
        setIfValid('genre', genre, ['', 'all']);
        setIfValid('order', order);
        setIfValid('type', type, ['', 'MOVIES']);

        // Check if we're navigating to the same URL to avoid duplicate history entries
        const newSearch = urlSearchParams.toString();
        const currentSearch = window.location.search.substring(1); // Remove leading '?'

        navigate({
            search: newSearch
        }, {
            replace: newSearch === currentSearch
        })
    }, [navigate]);

    const selectSort = useCallback((sort) => {
        UserPreferences.setSortPreference(sort);
        // Just navigate - the useEffect will update state
        search(sort, navigationState.genre, navigationState.q, navigationState.type, navigationState.parentId);
    }, [search, navigationState.genre, navigationState.q, navigationState.type, navigationState.parentId]);

    const selectGenre = useCallback((genre) => {
        // Just navigate - the useEffect will update state
        search(navigationState.order, genre, navigationState.q, navigationState.type, navigationState.parentId);
    }, [search, navigationState.order, navigationState.q, navigationState.type, navigationState.parentId]);

    const filterMedia = useCallback((searchText) => {
        setNavigationState(prev => ({
            ...prev,
            q: searchText,
            page: 0
        }));
    }, []);

    const filterMediaNavigate = useCallback((searchText) => {
        search(navigationState.order, navigationState.genre, searchText, navigationState.type, navigationState.parentId);
    }, [search, navigationState.order, navigationState.genre, navigationState.type, navigationState.parentId]);

    const setType = useCallback((type) => {
        // Change type means a fresh view - reset filters
        // Just navigate - the useEffect will update state
        search(navigationState.order, navigationState.genre, '', type, null);
    }, [search, navigationState.order, navigationState.genre]);

    const navigateTo = useCallback((type, parentId) => {
        // Navigation means a fresh view - reset filters
        // Just navigate - the useEffect will update state
        search(navigationState.order, navigationState.genre, '', type, parentId);
    }, [search, navigationState.order, navigationState.genre]);

    const showEmptyState = !error && !isInitialLoad && media.length === 0 && totalCount === 0;

    // Favorites are now filtered server-side
    const displayMedia = media;

    const showFavoritesEmpty = navigationState.type?.toUpperCase() === FAVORITES_TYPE && displayMedia.length === 0 && !isInitialLoad;
    const isHistoryView = navigationState.type?.toUpperCase() === HISTORY_TYPE;

    return (
        <div style={layoutProps}>
            <ControlBar selectSort={selectSort} selectGenre={selectGenre} filterMedia={filterMedia} navigateTo={navigateTo} filterMediaNavigate={filterMediaNavigate} setType={setType}/>
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
            ) : isHistoryView ? (
                <HistoryList media={displayMedia} playMedia={playMedia} />
            ) : (
                <MediaList media={displayMedia} navigateTo={navigateTo} playMedia={playMedia} nextPage={nextPage} hasMore={hasMore}/>
            )}
            <LoadingIndicator/>
        </div>
    )
}
