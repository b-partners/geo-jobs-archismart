alter table if exists "detection_task"
    add column if not exists parent_task_id varchar;
alter table if exists "tiling_task"
    add column if not exists parent_task_id varchar;
alter table if exists "tile_detection_task"
    add column if not exists parent_task_id varchar;