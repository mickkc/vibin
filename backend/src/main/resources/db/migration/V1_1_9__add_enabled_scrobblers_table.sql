create table if not exists enabled_scrobbler
(
    id             bigserial primary key,
    user_id        bigint       not null
        constraint fk_enabled_scrobbler_user_id__id
            references "user"
            on update restrict on delete restrict,
    scrobbler_id   varchar(255) not null
);