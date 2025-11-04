create table if not exists "annotation_retrieving_job" (
   id varchar primary key,
   zone_name varchar,
   email_receiver varchar,
   submission_instant timestamp with time zone default now()::timestamp with time zone,
   annotation_job_id varchar,
   detection_job_id varchar
);
