create table if not exists "detection_address_conversion_task"
(
    id                 varchar primary key,
    job_id             varchar references "detection_address_conversion_job" (id),
    as_job_id          varchar,
    submission_instant timestamp with time zone default now()::timestamp with time zone,
    address            varchar,
    feature            jsonb,
    layer              varchar
);