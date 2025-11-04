create table if not exists "zone_tiling_task"(
  id varchar primary key default uuid_generate_v4(),
  submission_instant timestamp without time zone not null default now()::timestamp without time zone,
  geometry jsonb,
  job_id varchar references zone_tiling_job("id") not null
);