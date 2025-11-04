create table if not exists "detected_tile"
(
    id varchar primary key default uuid_generate_v4(),
    tile jsonb,
    bucket_path varchar,
    creation_datetime timestamp with time zone not null default now()::timestamp with time zone
);
