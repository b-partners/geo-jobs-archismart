create table if not exists "geo_json_conversion_task" (
   id varchar primary key,
   job_id varchar,
   as_job_id varchar,
   submission_instant timestamp with time zone default now()::timestamp with time zone,
   geo_json_url varchar
);
