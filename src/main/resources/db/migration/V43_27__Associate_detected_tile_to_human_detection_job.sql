alter table "detected_tile"
    add column if not exists human_detection_job_id varchar;

create index if not exists detected_tile_zone_human_detection_job_id_index on "detected_tile" (human_detection_job_id);
