ALTER TABLE if EXISTS media DROP CONSTRAINT media_media_type_check;

ALTER TABLE if EXISTS media ADD CONSTRAINT media_media_type_check
    CHECK (media_type IN ('MOVIE', 'EPISODE', 'SEASON', 'SERIES'));
