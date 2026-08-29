alter table session add column device_name varchar(255) not null default 'Unknown';
alter table trusted_device add column device_name varchar(255) not null default 'Unknown';
alter table login_notification add column device_name varchar(255) not null default 'Unknown';


alter table trusted_device
    drop constraint device_user;

alter table trusted_device
    add constraint device_user
        unique (device_id_hash, user_id, device_name);