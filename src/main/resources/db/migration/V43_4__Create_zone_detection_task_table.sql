create table if not exists "zone_detection_task"(
  id varchar primary key default uuid_generate_v4(),
  submission_instant timestamp with time zone not null default now()::timestamp with time zone,
  tile jsonb,
  job_id varchar references zone_detection_job("id")
);