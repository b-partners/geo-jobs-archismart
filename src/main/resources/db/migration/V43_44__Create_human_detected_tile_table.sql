create table if not exists "human_detected_tile" (
   id varchar primary key,
   job_id varchar,
   annotation_job_id varchar,
   machine_detected_tile_id varchar,
   image_size integer,
   tile jsonb
);
