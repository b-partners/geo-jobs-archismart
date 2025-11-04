create table if not exists "parcel_detection_job"
(
    id                 varchar primary key               default uuid_generate_v4(),
    email_receiver     varchar,
    zone_name          varchar,
    submission_instant timestamp with time zone not null default now()::timestamp with time zone
);