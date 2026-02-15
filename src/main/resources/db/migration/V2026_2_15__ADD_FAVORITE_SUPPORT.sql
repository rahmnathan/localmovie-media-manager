-- Media Favorites table
CREATE SEQUENCE media_favorite_sequence START WITH 1 INCREMENT BY 50;

CREATE TABLE media_favorite
(
    id            bigint NOT NULL,
    media_file_id bigint NOT NULL,
    media_user_id bigint NOT NULL,
    created       TIMESTAMP(6),
    updated       TIMESTAMP(6),
    version       bigint,
    PRIMARY KEY (id),
    CONSTRAINT idx_media_favorite UNIQUE (media_file_id, media_user_id),
    CONSTRAINT fk_media_favorite_media_file FOREIGN KEY (media_file_id) REFERENCES media_file(id),
    CONSTRAINT fk_media_favorite_media_user FOREIGN KEY (media_user_id) REFERENCES media_user(id)
);

CREATE INDEX idx_media_favorite_user ON media_favorite (media_user_id);
