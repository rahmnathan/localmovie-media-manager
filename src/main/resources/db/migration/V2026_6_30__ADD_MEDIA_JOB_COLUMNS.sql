-- Add columns for async subtitle sync support
ALTER TABLE subtitle_job ADD COLUMN IF NOT EXISTS sync_status VARCHAR(20);
ALTER TABLE subtitle_job ADD COLUMN IF NOT EXISTS sync_job_name VARCHAR(100);
ALTER TABLE subtitle_job ADD COLUMN IF NOT EXISTS sync_temp_dir VARCHAR(500);
ALTER TABLE subtitle_job ADD COLUMN IF NOT EXISTS unsynced_content TEXT;
ALTER TABLE subtitle_job ADD COLUMN IF NOT EXISTS opensubtitles_id VARCHAR(50);
