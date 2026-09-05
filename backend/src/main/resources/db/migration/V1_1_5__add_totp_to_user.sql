alter table "user" add column totp_secret varchar(32) default null;
alter table "user" add column totp_type integer default 0;
alter table "session" add column state integer default 0;