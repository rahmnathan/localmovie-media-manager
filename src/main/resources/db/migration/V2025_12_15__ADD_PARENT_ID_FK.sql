-- Add parent_id foreign key column to media_file table for proper parent-child relationships

-- Step 1: Add parent_id column as nullable
ALTER TABLE media_file ADD COLUMN parent_id BIGINT;
ALTER TABLE media_file ADD COLUMN streamable BOOLEAN;
ALTER TABLE media_file ADD COLUMN media_file_type VARCHAR(255) check (media_file_type in ('MOVIE', 'SERIES', 'EPISODE', 'SEASON', 'MOVIE_SUBTITLE', 'EPISODE_SUBTITLE'));

-- Step 2: Populate parent_id from existing parent_path data
UPDATE media_file child
SET parent_id = (
    SELECT parent.id
    FROM media_file parent
    WHERE parent.path = child.parent_path
)
WHERE parent_path IS NOT NULL;

-- Step 3: Add foreign key constraint with cascade delete
ALTER TABLE media_file
    ADD CONSTRAINT fk_media_file_parent
    FOREIGN KEY (parent_id)
    REFERENCES media_file(id)
    ON DELETE CASCADE;

-- Step 4: Add index on parent_id for query performance
CREATE INDEX idx_media_file_parent_id ON media_file(parent_id);
