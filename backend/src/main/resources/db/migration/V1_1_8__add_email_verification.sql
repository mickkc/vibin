alter table "user" add column if not exists "email_verified" boolean default false;

create table if not exists email_verification_token
(
    token_hash varchar(64) not null primary key,
    user_id       bigint       not null
        constraint fk_email_verification_token_user_id__id
            references "user"
            on update restrict on delete restrict,
    email_address varchar(255) not null,
    created_at    bigint       not null
);

