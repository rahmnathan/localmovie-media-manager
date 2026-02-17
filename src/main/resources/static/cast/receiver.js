/**
 * LocalMovies Custom Cast Receiver - Minimal version for debugging
 */

// Start with just the basics - no custom features
const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();

// Log any errors
playerManager.addEventListener(
    cast.framework.events.EventType.ERROR,
    (event) => {
        console.error('Player error:', event.detailedErrorCode, event.error);
    }
);

// Log state changes for debugging
playerManager.addEventListener(
    cast.framework.events.EventType.PLAYER_STATE_CHANGED,
    (event) => {
        console.log('Player state:', event.state);
    }
);

// Start the receiver with minimal config
console.log('Starting minimal Cast receiver...');
context.start();
console.log('Cast receiver started');
