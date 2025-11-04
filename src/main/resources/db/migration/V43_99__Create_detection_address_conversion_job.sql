create table if not exists "detection_address_conversion_job"
(
    id                 varchar primary key,
    zone_name          varchar,
    email_receiver     varchar,
    submission_instant timestamp with time zone default now()::timestamp with time zone,
    detection_id       varchar
);

alter type job_type add value if not exists 'DETECTION_ADDRESS_CONVERSION';