create table if not exists totp_mail
(
    id bigserial primary key,
    session_id bigint not null
        constraint totp_mail_session_id_unique
            unique
        constraint fk_totp_mail_session_id__id
            references session
            on update restrict on delete restrict,
    code varchar(6) not null,
    sent bigint not null
);
