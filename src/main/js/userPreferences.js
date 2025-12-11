// LocalStorage utility for user preferences

const STORAGE_KEYS = {
    FAVORITES: 'media_favorites',
    SORT_PREFERENCE: 'media_sort_preference',
    WATCH_PROGRESS: 'media_watch_progress'
};

export const UserPreferences = {
    // Favorites/Watchlist
    getFavorites: () => {
        try {
            const favorites = localStorage.getItem(STORAGE_KEYS.FAVORITES);
            return favorites ? JSON.parse(favorites) : [];
        } catch (e) {
            console.error('Error reading favorites:', e);
            return [];
        }
    },

    addFavorite: (mediaId) => {
        const favorites = UserPreferences.getFavorites();
        if (!favorites.includes(mediaId)) {
            favorites.push(mediaId);
            localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(favorites));
        }
    },

    removeFavorite: (mediaId) => {
        const favorites = UserPreferences.getFavorites();
        const updated = favorites.filter(id => id !== mediaId);
        localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(updated));
    },

    isFavorite: (mediaId) => {
        return UserPreferences.getFavorites().includes(mediaId);
    },

    // Sort Preference
    getSortPreference: () => {
        return localStorage.getItem(STORAGE_KEYS.SORT_PREFERENCE) || 'title';
    },

    setSortPreference: (sortBy) => {
        localStorage.setItem(STORAGE_KEYS.SORT_PREFERENCE, sortBy);
    },

    // Watch Progress
    getWatchProgress: (mediaId) => {
        try {
            const allProgress = localStorage.getItem(STORAGE_KEYS.WATCH_PROGRESS);
            const progressMap = allProgress ? JSON.parse(allProgress) : {};
            return progressMap[mediaId] || null;
        } catch (e) {
            console.error('Error reading watch progress:', e);
            return null;
        }
    },

    setWatchProgress: (mediaId, progress) => {
        try {
            const allProgress = localStorage.getItem(STORAGE_KEYS.WATCH_PROGRESS);
            const progressMap = allProgress ? JSON.parse(allProgress) : {};
            progressMap[mediaId] = progress;
            localStorage.setItem(STORAGE_KEYS.WATCH_PROGRESS, JSON.stringify(progressMap));
        } catch (e) {
            console.error('Error saving watch progress:', e);
        }
    }
};
