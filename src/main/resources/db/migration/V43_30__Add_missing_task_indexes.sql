create index if not exists id_tile_detection_task_idx on "tile_detection_task" (detection_task_id);
create index if not exists id_tile_detection_task_parcel_idx on "tile_detection_task" (parcel_id);
create index if not exists id_tile_detection_task_job_idx on "tile_detection_task" (job_id);
create index if not exists id_tile_detection_task_parent_task_idx on "tile_detection_task" (parent_task_id);
create index if not exists id_detection_task_parent_task_idx on "detection_task" (parent_task_id);
create index if not exists id_tiling_task_parent_task_idx on "tiling_task" (parent_task_id);

