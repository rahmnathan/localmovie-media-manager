import React from 'react';

export const SkeletonCard = () => {
    return (
        <div className="media-card skeleton-card" aria-hidden="true">
            <div className="skeleton-poster"></div>
            <div className="media-card__description">
                <div className="skeleton-title"></div>
                <div className="skeleton-text"></div>
                <div className="skeleton-text skeleton-text--short"></div>
            </div>
        </div>
    );
};

export const SkeletonLoader = ({ count = 6 }) => {
    return (
        <>
            {Array.from({ length: count }).map((_, index) => (
                <SkeletonCard key={`skeleton-${index}`} />
            ))}
        </>
    );
};
