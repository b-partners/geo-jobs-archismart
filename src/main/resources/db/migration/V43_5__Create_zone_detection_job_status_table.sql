create table if not exists "zone_detection_job_status"
(
    id varchar primary key default uuid_generate_v4(),
    progression progression_status ,
    health health_status ,
    creation_datetime timestamp with time zone not null default now()::timestamp with time zone,
    job_id varchar references zone_detection_job("id"),
    message varchar
);
