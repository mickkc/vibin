-- Color scheme --

create table if not exists colorscheme
(
    id        bigserial  primary key,
    "primary" varchar(7) not null,
    light     varchar(7) not null,
    dark      varchar(7) not null
);

-- Image --

create table if not exists image
(
    id               bigserial     primary key,
    "sourceChecksum" varchar(32)   not null,
    source_path      varchar(1024) not null,
    color_scheme_id  bigint
        constraint fk_image_color_scheme_id__id
            references colorscheme
            on update restrict on delete restrict
);

-- User --

create table if not exists "user"
(
    id                 bigserial                primary key,
    created_at         bigint                   not null,
    updated_at         bigint,
    username           varchar(255)             not null
        constraint user_username_unique
            unique,
    display_name       varchar(255),
    description        text    default ''::text not null,
    password_hash      bytea                    not null,
    salt               bytea                    not null,
    email              varchar(255),
    is_active          boolean default true     not null,
    is_admin           boolean default false    not null,
    last_login         bigint,
    profile_picture_id bigint
        constraint fk_user_profile_picture_id__id
            references image
            on update restrict on delete restrict
);

-- Session --

create table if not exists session
(
    id         bigserial   primary key,
    user_id    bigint      not null
        constraint fk_session_user_id__id
            references "user"
            on update restrict on delete restrict,
    token      varchar(64) not null
        constraint session_token_unique
            unique,
    created_at bigint      not null,
    last_used  bigint      not null
);

-- Album --

create table if not exists album
(
    id           bigserial             primary key,
    created_at   bigint                not null,
    updated_at   bigint,
    title        varchar(255)          not null,
    description  text default ''::text not null,
    release_year integer,
    single       boolean,
    cover        bigint

        constraint fk_album_cover__id
            references image
            on update restrict on delete restrict
);

create index if not exists album_title
    on album (title);

-- Track --

create table if not exists track
(
    id            bigserial                primary key,
    created_at    bigint                   not null,
    updated_at    bigint,
    title         varchar(255)             not null,
    album_id      bigint                   not null
        constraint fk_track_album_id__id
            references album
            on update restrict on delete restrict,
    explicit      boolean default false    not null,
    track_number  integer,
    track_count   integer,
    disc_number   integer,
    disc_count    integer,
    year          integer,
    duration      bigint,
    comment       text    default ''::text not null,
    bitrate       integer,
    sample_rate   integer,
    channels      integer,
    cover_id      bigint
        constraint fk_track_cover_id__id
            references image
            on update restrict on delete restrict,
    path          varchar(1024)            not null
        constraint track_path_unique
            unique,
    checksum      text                     not null
        constraint track_checksum_unique
            unique,
    uploader      bigint
        constraint fk_track_uploader__id
            references "user"
            on update restrict on delete restrict,
    volume_offset double precision
);

-- Artist --

create table if not exists artist
(
    id          bigserial             primary key,
    created_at  bigint                not null,
    updated_at  bigint,
    name        varchar(255)          not null,
    description text default ''::text not null,
    image_url   bigint
        constraint fk_artist_image_url__id
            references image
            on update restrict on delete restrict
);

create index if not exists artist_name
    on artist (name);

-- Favorite --

create table if not exists favorite
(
    user_id     bigint      not null
    constraint fk_favorite_user_id__id
        references "user"
        on update restrict on delete restrict,
    entity_type varchar(10) not null,
    entity_id   bigint      not null,
    place       integer     not null,
        constraint pk_favorite_user_entity
            primary key (user_id, entity_type, place)
);


-- Granted permission --

create table if not exists granted_permission
(
    user_id    bigint  not null
        constraint fk_granted_permission_user_id__id
            references "user"
            on update restrict on delete restrict,
    permission integer not null,
        constraint pk_grantedpermission_user_permission
            primary key (user_id, permission)
);

-- Listen --

create table if not exists listen
(
    id          bigserial primary key,
    user_id     bigint  not null
        constraint fk_listen_user_id__id
            references "user"
            on update restrict on delete restrict,
    entity_id   bigint  not null,
    type        integer not null,
    listened_at bigint  not null
);

-- Login notification --

create table if not exists login_notification
(
    id             bigserial primary key,
    token_hash     bytea  not null,
    session_id     bigint not null
        constraint fk_login_notification_session_id__id
            references session
            on update restrict on delete restrict,
    device_id_hash bytea  not null,
    created_at     bigint not null
);

-- Lyrics --

create table if not exists lyrics
(
    id         bigserial primary key,
    created_at bigint not null,
    updated_at bigint,
    track_id   bigint not null
        constraint lyrics_track_id_unique
            unique
        constraint fk_lyrics_track_id__id
            references track
            on update restrict on delete restrict,
    content    text   not null
);

-- Media token --

create table if not exists mediatoken
(
    id         bigserial    primary key,
    user_id    bigint       not null
        constraint fk_mediatoken_user_id__id
            references "user"
            on update restrict on delete restrict,
    token      varchar(255) not null
        constraint mediatoken_token_unique
            unique,
    device_id  varchar(64),
    created_at bigint       not null
);

-- Playlist --

create table if not exists playlist
(
    id          bigserial                primary key,
    created_at  bigint                   not null,
    updated_at  bigint,
    name        varchar(255)             not null,
    description text    default ''::text not null,
    cover_id    bigint
        constraint fk_playlist_cover_id__id
            references image
            on update restrict on delete restrict,
    public      boolean default false    not null,
    vibe_def    text,
    owner_id    bigint                   not null
        constraint fk_playlist_owner_id__id
            references "user"
            on update restrict on delete restrict
);

-- Playlist collaborator --

create table if not exists playlist_collaborator
(
    playlist_id bigint not null
        constraint fk_playlist_collaborator_playlist_id__id
            references playlist
            on update restrict on delete restrict,
    user_id     bigint not null
        constraint fk_playlist_collaborator_user_id__id
            references "user"
            on update restrict on delete restrict,
    constraint pk_playlistcollaborator_playlist_user
        primary key (playlist_id, user_id)
);

-- Playlist track --

create table if not exists playlist_track
(
    id          bigserial         primary key,
    playlist_id bigint            not null
        constraint fk_playlist_track_playlist_id__id
            references playlist
            on update restrict on delete restrict,
    track_id    bigint            not null
        constraint fk_playlist_track_track_id__id
            references track
            on update restrict on delete restrict,
    position    integer default 0 not null,
    user_id     bigint            not null
        constraint fk_playlist_track_user_id__id
            references "user"
            on update restrict on delete restrict,
    added_at    bigint            not null
);

-- Setting --

create table if not exists setting
(
    id    bigserial    primary key,
    key   varchar(255) not null
        constraint setting_key_unique
            unique,
    value text         not null
);

-- Shared widget --

create table if not exists shared_widget
(
    id           varchar(64) not null primary key,
    user_id      bigint      not null
        constraint fk_shared_widget_user_id__id
            references "user"
            on update restrict on delete restrict,
    bg_color     integer,
    fg_color     integer,
    accent_color integer
);

-- Tag --

create table if not exists tag
(
    id          bigserial                primary key,
    created_at  bigint                   not null,
    updated_at  bigint,
    name        varchar(255)             not null,
    description text    default ''::text not null,
    importance  integer default 0        not null
);

create index if not exists tag_name
    on tag (name);

-- Task setting --

create table if not exists task_setting
(
    task_id varchar(255)         not null primary key,
    enabled boolean default true not null
);

create index if not exists track_title
    on track (title);

create index if not exists track_album_id
    on track (album_id);

-- Track Artist --

create table if not exists track_artist
(
    track_id  bigint not null
        constraint fk_track_artist_track_id__id
            references track
            on update restrict on delete restrict,
    artist_id bigint not null
        constraint fk_track_artist_artist_id__id
            references artist
            on update restrict on delete restrict,
    constraint pk_trackartist
        primary key (track_id, artist_id)
);

-- Track Relation --

create table if not exists track_relation
(
    id                  bigserial                                  primary key,
    track_id            bigint                                     not null
        constraint fk_track_relation_track_id__id
            references track
            on update restrict on delete restrict,
    related_track_id    bigint                                     not null
        constraint fk_track_relation_related_track_id__id
            references track
            on update restrict on delete restrict,
    description         varchar(255) default ''::character varying not null,
    mutual              boolean      default false                 not null,
    reverse_description varchar(255)
);

create index if not exists track_relation_track_id
    on track_relation (track_id);

create index if not exists track_relation_related_track_id
    on track_relation (related_track_id);



-- Track Tag --

create table if not exists track_tag
(
    track_id bigint not null
        constraint fk_track_tag_track_id__id
            references track
            on update restrict on delete restrict,
    tag_id   bigint not null
        constraint fk_track_tag_tag_id__id
            references tag
            on update restrict on delete restrict,
    constraint pk_tracktag
        primary key (track_id, tag_id)
);

-- Track listen duration --

create table if not exists tracklistenduration
(
    id                        bigserial primary key,
    track_id                  bigint
        constraint fk_tracklistenduration_track_id__id
            references track
            on update restrict on delete restrict,
    user_id                   bigint not null
        constraint fk_tracklistenduration_user_id__id
            references "user"
            on update restrict on delete restrict,
    listened_duration_seconds bigint not null,
    created_at                bigint not null
);

-- Trusted device --

create table if not exists trusted_device
(
    id             bigserial primary key,
    device_id_hash bytea  not null,
    user_id        bigint not null
        constraint fk_trusted_device_user_id__id
            references "user"
            on update restrict on delete restrict,
    trusted_at     bigint not null,
    constraint device_user
        unique (device_id_hash, user_id)
);

create index if not exists trusted_device_device_id_hash
    on trusted_device (device_id_hash);

-- User setting --

create table if not exists user_setting
(
    id      bigserial    primary key,
    key     varchar(255) not null,
    user_id bigint       not null,
    value   text         not null
);

create index if not exists user_setting_key
    on user_setting (key);

create index if not exists user_setting_user_id
    on user_setting (user_id);

-- Widget Type --

create table if not exists widget_type
(
    widget_id varchar(64) not null
        constraint fk_widget_type_widget_id__id
            references shared_widget
            on update restrict on delete restrict,
    type      varchar(50) not null,
    index     integer     not null,
    constraint pk_widgettype_widget_type
        primary key (widget_id, type)
);