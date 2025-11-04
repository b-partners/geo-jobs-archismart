alter table "detected_tile"
    rename column "job_id" to "zdj_job_id";
alter table "detected_tile"
    add column if not exists "parcel_job_id" varchar references "parcel_detection_job" (id);
