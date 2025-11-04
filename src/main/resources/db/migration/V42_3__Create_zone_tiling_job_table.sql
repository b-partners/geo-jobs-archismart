create extension if not exists "uuid-ossp";
create table if not exists "zone_tiling_job"(
  id varchar primary key default uuid_generate_v4(),
  submission_instant timestamp without time zone not null default now()::timestamp without time zone,
  email_receiver varchar not null,
  zone_name varchar not null
);