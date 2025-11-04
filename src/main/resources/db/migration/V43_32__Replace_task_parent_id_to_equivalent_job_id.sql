alter table if exists "detection_task"
    drop column if exists parent_task_id;
alter table if exists "tiling_task"
    drop column if exists parent_task_id;
alter table if exists "tile_detection_task"
    drop column if exists parent_task_id;
alter table if exists "detection_task"
    add column if not exists equivalent_job_id varchar;
alter table if exists "tiling_task"
    add column if not exists equivalent_job_id varchar;
alter table if exists "tile_detection_task"
    add column if not exists equivalent_job_id varchar;
create index if not exists id_tile_detection_equivalent_job_idx on "tile_detection_task" (equivalent_job_id);
create index if not exists id_detection_equivalent_job_idx on "detection_task" (equivalent_job_id);
create index if not exists id_tiling_equivalent_job_idx on "tiling_task" (equivalent_job_id);
