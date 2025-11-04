create table if not exists "tile_detection_task"
(
    id                 varchar primary key default uuid_generate_v4(),
    detection_task_id  varchar references "detection_task" ("id"),
    parcel_id           varchar references "parcel"(id),
    job_id              varchar references "zone_detection_job"(id),
    tile               jsonb,
    submission_instant timestamp with time zone not null default now()::timestamp with time zone
);