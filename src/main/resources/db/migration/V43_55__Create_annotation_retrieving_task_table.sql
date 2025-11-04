alter table "annotation_retrieving_task"
    add column if not exists annotation_job_id           varchar,
    add column if not exists human_zone_detection_job_id varchar,
    add column if not exists x_tile                      int,
    add column if not exists y_tile                      int,
    add column if not exists zoom                        int;