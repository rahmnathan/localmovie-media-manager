-- Sequences
CREATE SEQUENCE media_subtitle_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE subtitle_job_sequence START WITH 1 INCREMENT BY 50;

-- Subtitle storage table
CREATE TABLE media_subtitle (
    id bigint NOT NULL,
    media_file_id bigint NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'en',
    format VARCHAR(10) NOT NULL DEFAULT 'vtt',
    subtitle_content TEXT NOT NULL,
    opensubtitles_id VARCHAR(50),
    created TIMESTAMP(6),
    updated TIMESTAMP(6),
    version bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_subtitle_media_file FOREIGN KEY (media_file_id) REFERENCES media_file(id) ON DELETE CASCADE,
    CONSTRAINT uq_subtitle_media_file_lang UNIQUE (media_file_id, language_code)
);

CREATE INDEX idx_media_subtitle_media_file_id ON media_subtitle(media_file_id);

-- Subtitle job queue table
CREATE TABLE subtitle_job (
    id bigint NOT NULL,
    media_file_id bigint NOT NULL,
    imdb_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created TIMESTAMP(6),
    updated TIMESTAMP(6),
    version bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_subtitle_job_media_file FOREIGN KEY (media_file_id) REFERENCES media_file(id) ON DELETE CASCADE
);

CREATE INDEX idx_subtitle_job_status ON subtitle_job(status);
CREATE INDEX idx_subtitle_job_media_file_id ON subtitle_job(media_file_id);

-- Add IMDB ID column to media table
ALTER TABLE media ADD COLUMN imdb_id VARCHAR(20);
