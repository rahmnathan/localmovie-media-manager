// LocalStorage utility for user preferences

const STORAGE_KEYS = {
    SORT_PREFERENCE: 'media_sort_preference',
    WATCH_PROGRESS: 'media_watch_progress'
};

export const UserPreferences = {
    // Favorites/Watchlist - backed by database
    addFavorite: async (mediaId) => {
        try {
            const response = await fetch(`/localmovie/v1/media/${encodeURIComponent(mediaId)}/favorite`, {
                method: 'POST'
            });
            return response.ok;
        } catch (e) {
            console.error('Error adding favorite:', e);
            return false;
        }
    },

    removeFavorite: async (mediaId) => {
        try {
            const response = await fetch(`/localmovie/v1/media/${encodeURIComponent(mediaId)}/favorite`, {
                method: 'DELETE'
            });
            return response.ok;
        } catch (e) {
            console.error('Error removing favorite:', e);
            return false;
        }
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
