import React from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';

const dialogStyle = {
    position: 'absolute',
    width: '90%',
    height: 'auto',
    maxWidth: '600px',
    maxHeight: '90vh',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    textAlign: 'center',
    background: 'rgba(0, 0, 0, 0.7)',
    color: 'rgb(220,220,220)',
    padding: '1rem',
    borderRadius: '8px',
    overflow: 'auto'
};

/**
 * Format seconds to mm:ss display
 */
const formatTime = (seconds) => {
    if (!seconds) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
};

export function CastControls({
    title,
    posterUrl,
    deviceName,
    currentTime,
    duration,
    isPaused,
    castError,
    onSeek,
    onPlayPause,
    onStopCasting,
}) {
    return (
        <Dialog open={true} onClose={() => {}} style={dialogStyle}>
            <DialogPanel>
                <DialogTitle>{title}</DialogTitle>

                {castError && (
                    <div style={{ color: '#ff6b6b', marginBottom: '1rem' }}>
                        Error: {castError}
                    </div>
                )}

                <p>Playing on {deviceName || 'Chromecast device'}...</p>

                <img
                    src={posterUrl}
                    alt=""
                    style={{ maxWidth: '100%', height: 'auto' }}
                />

                {/* Seek bar */}
                <input
                    type="range"
                    min="0"
                    max={duration || 0}
                    value={currentTime || 0}
                    step="1"
                    style={{ width: '80%', margin: '1rem 0' }}
                    onChange={(e) => onSeek(Number(e.target.value))}
                    aria-label="Seek video position"
                />

                {/* Time display */}
                <div>
                    {formatTime(currentTime)} / {formatTime(duration)}
                </div>

                {/* Controls */}
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                    <button onClick={onPlayPause} aria-label={isPaused ? 'Resume playback' : 'Pause playback'}>
                        {isPaused ? 'Resume' : 'Pause'}
                    </button>
                    <button onClick={onStopCasting} aria-label="Stop casting">
                        Stop Casting
                    </button>
                </div>
            </DialogPanel>
        </Dialog>
    );
}
