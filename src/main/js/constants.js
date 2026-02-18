/**
 * Application constants
 */

// API endpoints
export const API = {
    MEDIA: '/localmovie/v1/media',
    MEDIA_BY_ID: (id) => `/localmovie/v1/media/${id}`,
    SIGNED_URLS: (id) => `/localmovie/v1/media/${id}/url/signed`,
};

// Pagination
export const PAGINATION = {
    DEFAULT_PAGE_SIZE: 50,
    SKELETON_COUNT: 6,
};

// Timing (in milliseconds)
export const TIMING = {
    SEARCH_DEBOUNCE_MS: 300,
    CAST_PROGRESS_SAVE_INTERVAL_MS: 10000,
};

// Cast
export const CAST = {
    RECEIVER_APP_ID: '9A05279D',
};

// Media types
export const MEDIA_TYPES = {
    MOVIES: 'MOVIES',
    SERIES: 'SERIES',
    FAVORITES: 'favorites',
};

// Sort options
export const SORT_OPTIONS = [
    { value: 'title', label: 'Title' },
    { value: 'year', label: 'Year' },
    { value: 'rating', label: 'Rating' },
    { value: 'added', label: 'Date Added' },
];

// Genre options
export const GENRE_OPTIONS = [
    { value: 'all', label: 'All' },
    { value: 'action', label: 'Action' },
    { value: 'comedy', label: 'Comedy' },
    { value: 'fantasy', label: 'Fantasy' },
    { value: 'horror', label: 'Horror' },
    { value: 'sci-fi', label: 'Sci-Fi' },
    { value: 'thriller', label: 'Thriller' },
    { value: 'war', label: 'War' },
];

// Client identifier
export const CLIENT_ID = 'WEBAPP';

// Video player
export const VIDEO_PLAYER = {
    MAX_WIDTH: '1200px',
    MAX_HEIGHT: '90vh',
};

// Session storage keys
export const STORAGE_KEYS = {
    SCROLL_POSITION: 'mediaListScrollPosition',
    SORT_PREFERENCE: 'sortPreference',
    FAVORITES: 'favorites',
};

// Cookie names
export const COOKIES = {
    PROGRESS_PREFIX: 'progress-',
};
