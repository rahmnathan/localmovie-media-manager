CREATE INDEX IF NOT EXISTS idx_media_view_user_updated
    ON media_view (media_user_id, updated DESC);

CREATE INDEX IF NOT EXISTS idx_media_view_updated
    ON media_view (updated DESC);
