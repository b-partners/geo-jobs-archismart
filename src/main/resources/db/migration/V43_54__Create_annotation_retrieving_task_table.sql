create table if not exists "annotation_retrieving_task" (
   id varchar primary key,
   job_id varchar references "annotation_retrieving_job"(id),
   as_job_id varchar,
   submission_instant timestamp with time zone default now()::timestamp with time zone,
   annotation_task_id varchar
);
