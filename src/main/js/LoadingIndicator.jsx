import { usePromiseTracker } from "react-promise-tracker";
import { ColorRing } from 'react-loader-spinner';
import React from "react";

export const LoadingIndicator = ({ loadedCount, totalCount }) => {
    const { promiseInProgress } = usePromiseTracker();

    if (!promiseInProgress) return null;

    const showProgress = totalCount > 0 && loadedCount > 0;

    return (
        <div className="loading-indicator" role="status" aria-live="polite">
            <ColorRing
                visible={true}
                height="60"
                width="60"
                ariaLabel="Loading content"
                wrapperClass="loading-indicator__spinner"
                colors={['#667eea', '#764ba2', '#667eea', '#764ba2', '#667eea']}
            />
            {showProgress && (
                <p className="loading-indicator__text">
                    Loading... {loadedCount} of {totalCount}
                </p>
            )}
        </div>
    );
};