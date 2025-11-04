alter table if exists "community_authorization"
    add column if not exists creation_datetime timestamp default current_timestamp;