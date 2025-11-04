alter table if exists "parcel_detection_task_entity"
    drop column if exists equivalent_job_id;
alter table if exists "tiling_task"
    drop column if exists equivalent_job_id;
alter table if exists "tile_detection_task"
    drop column if exists equivalent_job_id;
alter table if exists "parcel_detection_task_entity"
    add column if not exists as_job_id varchar;
alter table if exists "tiling_task"
    add column if not exists as_job_id varchar;
alter table if exists "tile_detection_task"
    add column if not exists as_job_id varchar;