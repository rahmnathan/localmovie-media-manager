/**
 * Subtitle Offset Module
 *
 * Applies timing offsets to WebVTT subtitle tracks on the Cast receiver.
 * Since CAF v3 has no built-in subtitle delay, we fetch the VTT file,
 * shift all timestamps, and substitute a Blob URL.
 */

window.SubtitleOffset = (() => {
    // Cache to avoid re-fetching the same VTT
    let originalVttCache = {};
    let currentBlobUrls = [];

    /**
     * Shift all timestamps in a WebVTT file by the given offset.
     * @param {string} vttText - The raw VTT file content
     * @param {number} offsetSec - Offset in seconds (positive = later, negative = earlier)
     * @returns {string} - Modified VTT content
     */
    function shiftVtt(vttText, offsetSec) {
        // WebVTT timestamp format: HH:MM:SS.mmm or MM:SS.mmm
        // Match timestamps in cue timing lines (00:00:00.000 --> 00:00:05.000)
        return vttText.replace(
            /(\d{2}:\d{2}:\d{2}\.\d{3}|\d{2}:\d{2}\.\d{3})/g,
            (match) => shiftTimestamp(match, offsetSec)
        );
    }

    /**
     * Shift a single timestamp by the offset.
     * @param {string} ts - Timestamp string (HH:MM:SS.mmm or MM:SS.mmm)
     * @param {number} offsetSec - Offset in seconds
     * @returns {string} - Shifted timestamp string
     */
    function shiftTimestamp(ts, offsetSec) {
        const parts = ts.split(':');
        let totalSec;

        if (parts.length === 3) {
            // HH:MM:SS.mmm format
            totalSec = parseInt(parts[0], 10) * 3600
                     + parseInt(parts[1], 10) * 60
                     + parseFloat(parts[2]);
        } else {
            // MM:SS.mmm format
            totalSec = parseInt(parts[0], 10) * 60
                     + parseFloat(parts[1]);
        }

        // Apply offset, but don't go negative
        totalSec = Math.max(0, totalSec + offsetSec);

        // Format back to string
        const h = Math.floor(totalSec / 3600);
        const m = Math.floor((totalSec % 3600) / 60);
        const s = (totalSec % 60).toFixed(3).padStart(6, '0');

        if (parts.length === 3) {
            return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${s}`;
        } else {
            // For MM:SS.mmm format, keep same format unless hours needed
            const totalMinutes = h * 60 + m;
            return `${String(totalMinutes).padStart(2, '0')}:${s}`;
        }
    }

    /**
     * Clean up any previously created Blob URLs to prevent memory leaks.
     */
    function cleanupBlobUrls() {
        currentBlobUrls.forEach(url => {
            try {
                URL.revokeObjectURL(url);
            } catch (e) {
                console.warn('Failed to revoke blob URL:', e);
            }
        });
        currentBlobUrls = [];
    }

    /**
     * Apply subtitle offset to the current media's text tracks.
     * @param {cast.framework.PlayerManager} playerManager - The CAF player manager
     * @param {number} offsetSec - Offset in seconds
     */
    function applyOffset(playerManager, offsetSec) {
        const mediaInfo = playerManager.getMediaInformation();
        if (!mediaInfo || !mediaInfo.tracks) {
            console.log('No media info or tracks available');
            return;
        }

        const textTracks = mediaInfo.tracks.filter(
            t => t.type === cast.framework.messages.TrackType.TEXT
        );

        if (textTracks.length === 0) {
            console.log('No text tracks found');
            return;
        }

        console.log('Applying subtitle offset:', offsetSec, 'to', textTracks.length, 'tracks');

        // Clean up old blob URLs
        cleanupBlobUrls();

        textTracks.forEach(track => {
            const originalUrl = track.trackContentId;
            if (!originalUrl) {
                console.warn('Track has no contentId');
                return;
            }

            // Check cache first
            const cacheKey = originalUrl;
            const cachedVtt = originalVttCache[cacheKey];

            if (cachedVtt) {
                // Use cached original VTT
                applyOffsetToVtt(track, cachedVtt, offsetSec, playerManager);
            } else {
                // Fetch and cache the VTT
                fetch(originalUrl)
                    .then(response => {
                        if (!response.ok) {
                            throw new Error(`VTT fetch failed: ${response.status}`);
                        }
                        return response.text();
                    })
                    .then(vttText => {
                        // Cache the original
                        originalVttCache[cacheKey] = vttText;
                        applyOffsetToVtt(track, vttText, offsetSec, playerManager);
                    })
                    .catch(err => {
                        console.error('Failed to fetch VTT:', err);
                    });
            }
        });
    }

    /**
     * Apply the offset to VTT content and update the track.
     */
    function applyOffsetToVtt(track, vttText, offsetSec, playerManager) {
        const shiftedVtt = shiftVtt(vttText, offsetSec);
        const blob = new Blob([shiftedVtt], { type: 'text/vtt' });
        const blobUrl = URL.createObjectURL(blob);
        currentBlobUrls.push(blobUrl);

        console.log('Created shifted VTT blob:', blobUrl);

        // Update the track's content ID to the blob URL
        // Note: CAF may require reloading tracks for this to take effect
        track.trackContentId = blobUrl;

        // Get current active track IDs
        const mediaStatus = playerManager.getMediaStatus();
        const activeTrackIds = mediaStatus?.activeTrackIds || [];

        // If this track is active, we need to refresh it
        if (activeTrackIds.includes(track.trackId)) {
            console.log('Refreshing active subtitle track');
            // Briefly disable and re-enable to force reload
            playerManager.editTracksInfo({
                activeTrackIds: [],
                textTrackStyle: mediaStatus?.textTracksStyle
            });

            setTimeout(() => {
                playerManager.editTracksInfo({
                    activeTrackIds: [track.trackId],
                    textTrackStyle: mediaStatus?.textTracksStyle
                });
            }, 100);
        }
    }

    /**
     * Clear the VTT cache (useful when media changes).
     */
    function clearCache() {
        originalVttCache = {};
        cleanupBlobUrls();
    }

    return {
        applyOffset,
        clearCache,
        // Expose for testing
        shiftVtt,
        shiftTimestamp
    };
})();
