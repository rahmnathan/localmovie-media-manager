/**
 * LocalMovies Custom Cast Receiver
 *
 * Features:
 * - Direct-to-server progress tracking (more reliable than sender-side polling)
 * - Subtitle offset support via custom message channel
 * - Queue support for episode auto-play
 */

const NAMESPACE = 'urn:x-cast:com.nathanrahm.localmovies';
const PROGRESS_INTERVAL_MS = 10000; // Save progress every 10 seconds

const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();

let subtitleOffsetSeconds = 0;
let progressTimer = null;
let currentUpdatePositionUrl = null;
let currentMediaId = null;

// --- Custom message channel for subtitle offset ---
context.addCustomMessageListener(NAMESPACE, (event) => {
    const msg = event.data;
    console.log('Received custom message:', msg);

    if (msg.type === 'SET_SUBTITLE_OFFSET') {
        subtitleOffsetSeconds = msg.offsetSeconds;
        console.log('Setting subtitle offset to:', subtitleOffsetSeconds);
        SubtitleOffset.applyOffset(playerManager, subtitleOffsetSeconds);
    }
});

// --- Intercept LOAD to extract custom metadata ---
playerManager.setMessageInterceptor(
    cast.framework.messages.MessageType.LOAD,
    (request) => {
        console.log('LOAD request received:', request);

        const metadata = request.media?.metadata;
        if (metadata) {
            // Read custom fields from metadata (set by Android sender in GoogleCastUtils.kt)
            currentMediaId = metadata['media-id'];
            currentUpdatePositionUrl = metadata['update-position-url'];
            console.log('Media loaded - id:', currentMediaId, 'updateUrl:', currentUpdatePositionUrl);
        }

        return request;
    }
);

// --- Player state changes: start/stop progress tracking ---
playerManager.addEventListener(
    cast.framework.events.EventType.PLAYER_STATE_CHANGED,
    (event) => {
        const state = event.state;
        console.log('Player state changed:', state);

        if (state === cast.framework.messages.PlayerState.PLAYING) {
            startProgressTimer();
        } else {
            stopProgressTimer();
            // Save final progress on pause/stop/idle
            if (state === cast.framework.messages.PlayerState.IDLE ||
                state === cast.framework.messages.PlayerState.PAUSED) {
                saveProgress();
            }
        }
    }
);

// --- Queue item changes: update metadata refs ---
playerManager.addEventListener(
    cast.framework.events.EventType.MEDIA_STATUS,
    (event) => {
        const mediaInfo = playerManager.getMediaInformation();
        if (mediaInfo) {
            const metadata = mediaInfo.metadata;
            if (metadata) {
                const newMediaId = metadata['media-id'];
                if (newMediaId && newMediaId !== currentMediaId) {
                    console.log('Queue advanced to new media:', newMediaId);
                    currentMediaId = newMediaId;
                    currentUpdatePositionUrl = metadata['update-position-url'];

                    // Re-apply subtitle offset for new media
                    if (subtitleOffsetSeconds !== 0) {
                        SubtitleOffset.applyOffset(playerManager, subtitleOffsetSeconds);
                    }
                }
            }
        }
    }
);

// --- Progress tracking functions ---

function startProgressTimer() {
    if (progressTimer) return;
    console.log('Starting progress timer');
    progressTimer = setInterval(saveProgress, PROGRESS_INTERVAL_MS);
}

function stopProgressTimer() {
    if (progressTimer) {
        console.log('Stopping progress timer');
        clearInterval(progressTimer);
        progressTimer = null;
    }
}

function saveProgress() {
    if (!currentUpdatePositionUrl) {
        console.log('No update-position-url, skipping progress save');
        return;
    }

    const positionSec = Math.floor(playerManager.getCurrentTimeSec());
    const durationMs = Math.floor(playerManager.getDurationSec() * 1000);

    // Build the URL: baseUrl/{positionSeconds}?{existingQuery}&duration={durationMs}
    const parts = currentUpdatePositionUrl.split('?');
    const baseUrl = parts[0];
    const existingQuery = parts[1] || '';

    let url = `${baseUrl}/${positionSec}`;
    if (existingQuery) {
        url += `?${existingQuery}&duration=${durationMs}`;
    } else {
        url += `?duration=${durationMs}`;
    }

    console.log('Saving progress:', positionSec, 'seconds to', url);

    fetch(url, { method: 'PATCH' })
        .then(response => {
            if (!response.ok) {
                console.error('Progress save failed:', response.status);
            }
        })
        .catch(err => {
            console.error('Progress save error:', err);
            // Optionally notify sender of error
            context.sendCustomMessage(NAMESPACE, undefined, {
                type: 'PROGRESS_ERROR',
                error: err.message
            });
        });
}

// --- Start the receiver ---
console.log('LocalMovies Cast Receiver starting...');
context.start();
console.log('LocalMovies Cast Receiver started');
