-- LocalMovies DB
CREATE SEQUENCE media_file_event_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE media_file_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE media_image_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE media_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE media_user_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE media_view_sequence START WITH 1 INCREMENT BY 50;
CREATE TABLE media
(
    number       INTEGER,
    created      TIMESTAMP(6),
    id           bigint NOT NULL,
    image_id     bigint UNIQUE,
    updated      TIMESTAMP(6),
    plot         VARCHAR(32600),
    actors       VARCHAR(255),
    genre        VARCHAR(255),
    imdb_rating  VARCHAR(255),
    media_type   VARCHAR(255) check (media_type in ('MOVIE', 'EPISODE', 'SEASON')),
    meta_rating  VARCHAR(255),
    release_year VARCHAR(255),
    title        VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE TABLE media_file
(
    created       TIMESTAMP(6),
    id            bigint NOT NULL,
    media_id      bigint UNIQUE,
    updated       TIMESTAMP(6),
    version       bigint NOT NULL,
    absolute_path VARCHAR(255),
    file_name     VARCHAR(255),
    media_file_id VARCHAR(255),
    parent_path   VARCHAR(255),
    path          VARCHAR(255) UNIQUE,
    PRIMARY KEY (id),
    constraint media_file_id_constraint UNIQUE (media_file_id)
);
CREATE TABLE media_file_event
(
    id            bigint NOT NULL,
    timestamp     TIMESTAMP(6),
    event         VARCHAR(255),
    media_file_id VARCHAR(255) UNIQUE,
    relative_path VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE TABLE media_image
(
    created  TIMESTAMP(6),
    id       bigint NOT NULL,
    media_id bigint UNIQUE,
    updated  TIMESTAMP(6),
    image    bytea,
    PRIMARY KEY (id)
);
CREATE TABLE media_job
(
    created          TIMESTAMP(6),
    id               bigint NOT NULL,
    updated          TIMESTAMP(6),
    handbrake_preset VARCHAR(255),
    input_file       VARCHAR(255),
    job_id           VARCHAR(255),
    output_file      VARCHAR(255),
    status           VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE TABLE media_user
(
    created TIMESTAMP(6),
    id      bigint NOT NULL,
    updated TIMESTAMP(6),
    user_id VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE TABLE media_view
(
    position      float(53),
    created       TIMESTAMP(6),
    id            bigint NOT NULL,
    media_file_id bigint,
    media_user_id bigint,
    updated       TIMESTAMP(6),
    PRIMARY KEY (id),
    constraint idx_media_view UNIQUE (media_file_id, media_user_id)
);
CREATE index idx_media_file_parent_path on media_file (parent_path);
CREATE index idx_media_user_user_id on media_user (user_id);
ALTER TABLE if EXISTS media ADD CONSTRAINT FKk8tt3ys3xp9arl1nyw7mr19vs FOREIGN KEY (image_id) REFERENCES media_image;
ALTER TABLE if EXISTS media_file ADD CONSTRAINT FK19mvpyv93s8p599jy63sqi51t FOREIGN KEY (media_id) REFERENCES media;
ALTER TABLE if EXISTS media_file_event ADD CONSTRAINT FKjmkwttt28e8gc3eueh3m094pm FOREIGN KEY (media_file_id) REFERENCES media_file (media_file_id);
ALTER TABLE if EXISTS media_image ADD CONSTRAINT FK8mrntscy64xf4fgxgnj2noret FOREIGN KEY (media_id) REFERENCES media;
ALTER TABLE if EXISTS media_view ADD CONSTRAINT FKd95knpml5b3lfvusj9abu4a80 FOREIGN KEY (media_file_id) REFERENCES media_file;
ALTER TABLE if EXISTS media_view ADD CONSTRAINT FK2981ravyhk3udmo5sr0euvesw FOREIGN KEY (media_user_id) REFERENCES media_user;


-- Spring Sessions
CREATE TABLE SPRING_SESSION
(
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);

-- ShedLock (distributed locking)
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);