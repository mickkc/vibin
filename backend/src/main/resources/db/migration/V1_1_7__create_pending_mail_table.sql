create table if not exists pending_mail
(
    id       bigserial primary key,
    priority integer default 0 not null,
    "to"     varchar(255)      not null,
    subject  varchar(255)      not null,
    body     text              not null
);

