create table if not exists "zone_detection_task_status"
(
    id varchar primary key default uuid_generate_v4(),
    progression progression_status not null,
    health health_status not null,
    creation_datetime timestamp with time zone not null default now()::timestamp with time zone,
    task_id varchar references zone_detection_task("id"),
    message varchar
);
