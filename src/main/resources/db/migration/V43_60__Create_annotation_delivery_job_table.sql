create table if not exists "annotation_delivery_job"
(
    id                  varchar primary key,
    zone_name           varchar,
    email_receiver      varchar,
    submission_instant  timestamp with time zone default now()::timestamp with time zone,
    annotation_job_id   varchar,
    annotation_job_name varchar,
    detection_job_id    varchar,
    labels              jsonb
);

alter type job_type add value if not exists 'ANNOTATION_DELIVERY';