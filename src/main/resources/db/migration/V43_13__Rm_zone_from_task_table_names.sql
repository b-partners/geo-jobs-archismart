ALTER TABLE zone_job_status RENAME TO job_status;
ALTER TABLE zone_task_status RENAME TO task_status;

ALTER TABLE zone_tiling_task RENAME TO tiling_task;
ALTER TABLE zone_detection_task RENAME TO detection_task;

ALTER TABLE zone_tiling_task_status RENAME TO tiling_task_status;
ALTER TABLE zone_detection_task_status RENAME TO detection_task_status;
