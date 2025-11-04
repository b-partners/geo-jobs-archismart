create table if not exists "zone_detection_job"
(
    id varchar primary key default uuid_generate_v4(),
    email_receiver varchar,
    zone_name varchar,
    zone_tiling_job_id varchar references zone_tiling_job("id"),
    submission_instant timestamp with time zone not null default now()::timestamp with time zone
);
