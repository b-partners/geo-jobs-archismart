create table if not exists "geo_json_conversion_job"
(
    id                  varchar primary key,
    zone_name           varchar,
    email_receiver      varchar,
    submission_instant  timestamp with time zone default now()::timestamp with time zone,
    zone_detection_job_id  varchar,
    file_key varchar,
    zone_detection_job_type    detection_type);