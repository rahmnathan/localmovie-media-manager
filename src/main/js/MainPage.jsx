import React, {useEffect, useState, useCallback} from 'react';
import { MediaList } from './MediaList.jsx';
import { HistoryList } from './HistoryList.jsx';
import { RecommendationsList } from './RecommendationsList.jsx';
import { ControlBar } from './ControlBar.jsx';
import { trackPromise } from 'react-promise-tracker';
import {createSearchParams, useNavigate, useSearchParams} from 'react-router-dom';
import {LoadingIndicator} from "./LoadingIndicator.jsx";
import {SkeletonLoader} from "./SkeletonCard.jsx";
import {UserPreferences} from "./userPreferences.js";

const FAVORITES_TYPE = 'FAVORITES';
const HISTORY_TYPE = 'HISTORY';
const RECOMMENDATIONS_TYPE = 'RECOMMENDATIONS';

const layoutProps = {
    textAlign: 'center'
};

// Breadcrumb component for showing navigation path
const Breadcrumb = ({ path, onNavigateBack }) => {
    if (path.length <= 1) return null;

    // Show path without root (e.g., "Breaking Bad → Season 1")
    const displayPath = path.slice(1);

    return (
        <div className="breadcrumb-bar" onClick={onNavigateBack}>
            <span className="breadcrumb-back">←</span>
            {displayPath.map((segment, index) => (
                <span key={index}>
                    {index > 0 && <span className="breadcrumb-separator">→</span>}
                    <span className="breadcrumb-segment">{segment.name}</span>
                </span>
            ))}
        </div>
    );
};

export function MainPage() {

    const [media, setMedia] = useState([]);
    const [recommendations, setRecommendations] = useState([]);
    const [totalCount, setTotalCount] = useState(0);
    const [navigationPath, setNavigationPath] = useState([{ name: 'Movies', parentId: null }]);
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

    // Parse error to provide user-friendly message
    const getErrorMessage = (error, status) => {
        if (!navigator.onLine) {
            return { title: 'No Internet Connection', message: 'Please check your network and try again.' };
        }
        if (status === 401 || status === 403) {
            return { title: 'Authentication Error', message: 'Your session may have expired. Please refresh the page.' };
        }
        if (status === 404) {
            return { title: 'Not Found', message: 'The requested content could not be found.' };
        }
        if (status >= 500) {
            return { title: 'Server Error', message: 'The server is temporarily unavailable. Please try again later.' };
        }
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            return { title: 'Connection Failed', message: 'Unable to reach the server. Please check your connection.' };
        }
        return { title: 'Something Went Wrong', message: 'Failed to load media. Please try again.' };
    };

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
                        const err = new Error(`HTTP error! status: ${response.status}`);
                        err.status = response.status;
                        throw err;
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
                    setError(getErrorMessage(error, error.status));
                    setIsInitialLoad(false);
                })
        );
    }, []);

    const fetchRecommendations = useCallback(() => {
        setError(null);
        trackPromise(
            fetch('/localmovie/v1/media/recommendations', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                },
            })
                .then(response => {
                    if (!response.ok) {
                        const err = new Error(`HTTP error! status: ${response.status}`);
                        err.status = response.status;
                        throw err;
                    }
                    return response.json();
                })
                .then(data => {
                    setRecommendations(data);
                    setIsInitialLoad(false);
                })
                .catch(error => {
                    console.error('Failed to fetch recommendations:', error);
                    setError(getErrorMessage(error, error.status));
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

        // Recommendations use a different API endpoint
        if (type.toUpperCase() === RECOMMENDATIONS_TYPE) {
            fetchRecommendations();
        } else {
            fetchMedia(false, newState);
        }

    }, [searchParams, fetchMedia, fetchRecommendations]);

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
        // Change type means a fresh view - reset filters and path
        const rootName = type === 'SERIES' ? 'Series' : 'Movies';
        setNavigationPath([{ name: rootName, parentId: null }]);
        search(navigationState.order, navigationState.genre, '', type, null);
    }, [search, navigationState.order, navigationState.genre]);

    const navigateTo = useCallback((type, parentId, name = '') => {
        // Add to navigation path
        if (parentId) {
            setNavigationPath(prev => [...prev, { name: name || 'Unknown', parentId }]);
        }
        // Navigation means a fresh view - reset filters
        // Just navigate - the useEffect will update state
        search(navigationState.order, navigationState.genre, '', type, parentId);
    }, [search, navigationState.order, navigationState.genre]);

    const navigateBack = useCallback(() => {
        if (navigationPath.length <= 1) return;

        // Go back one level
        const newPath = navigationPath.slice(0, -1);
        setNavigationPath(newPath);

        // Navigate to the parent
        const parent = newPath[newPath.length - 1];

        // Determine the type based on path depth
        let type;
        if (newPath.length === 1) {
            // Root level
            type = parent.name === 'Series' ? 'SERIES' : 'MOVIES';
        } else if (newPath.length === 2) {
            // Series level - show seasons
            type = 'SEASONS';
        } else {
            // Season level - show episodes
            type = 'EPISODES';
        }

        search(navigationState.order, navigationState.genre, '', type, parent.parentId);
    }, [navigationPath, search, navigationState.order, navigationState.genre]);

    const showEmptyState = !error && !isInitialLoad && media.length === 0 && totalCount === 0;

    // Favorites are now filtered server-side
    const displayMedia = media;

    const showFavoritesEmpty = navigationState.type?.toUpperCase() === FAVORITES_TYPE && displayMedia.length === 0 && !isInitialLoad;
    const isHistoryView = navigationState.type?.toUpperCase() === HISTORY_TYPE;
    const isRecommendationsView = navigationState.type?.toUpperCase() === RECOMMENDATIONS_TYPE;

    // Show breadcrumb only when navigating into series/seasons (not for special views)
    const showBreadcrumb = navigationPath.length > 1 &&
        !isHistoryView &&
        !isRecommendationsView &&
        navigationState.type?.toUpperCase() !== FAVORITES_TYPE;

    // Check if any filters are active
    const hasActiveFilters = navigationState.q || (navigationState.genre && navigationState.genre !== 'all');

    // Clear all filters
    const clearFilters = useCallback(() => {
        search(navigationState.order, '', '', navigationState.type, navigationState.parentId);
    }, [search, navigationState.order, navigationState.type, navigationState.parentId]);

    const handleMoreLikeThis = useCallback((genreValue) => {
        const firstGenre = genreValue?.split(',')?.[0]?.trim();
        if (!firstGenre) return;
        search(navigationState.order, firstGenre, '', 'MOVIES', null);
    }, [search, navigationState.order]);

    return (
        <div style={layoutProps}>
            <ControlBar
                selectSort={selectSort}
                selectGenre={selectGenre}
                filterMedia={filterMedia}
                navigateTo={navigateTo}
                filterMediaNavigate={filterMediaNavigate}
                setType={setType}
                onClearFilters={clearFilters}
                hasActiveFilters={hasActiveFilters}
            />
            {showBreadcrumb && (
                <Breadcrumb path={navigationPath} onNavigateBack={navigateBack} />
            )}
            {error && (
                <div className="error-state">
                    <div className="error-state__icon">⚠</div>
                    <h2 className="error-state__title">{error.title}</h2>
                    <p className="error-state__message">{error.message}</p>
                    <button
                        className="error-state__retry-btn"
                        onClick={() => fetchMedia(false, navigationState)}
                    >
                        Try Again
                    </button>
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
            ) : isRecommendationsView ? (
                <RecommendationsList
                    recommendations={recommendations}
                    playMedia={playMedia}
                    isLoading={isInitialLoad}
                    onMoreLikeThis={handleMoreLikeThis}
                />
            ) : (
                <MediaList
                    media={displayMedia}
                    navigateTo={navigateTo}
                    playMedia={playMedia}
                    nextPage={nextPage}
                    hasMore={hasMore}
                />
            )}
            <LoadingIndicator loadedCount={media.length} totalCount={totalCount} />
        </div>
    )
}
