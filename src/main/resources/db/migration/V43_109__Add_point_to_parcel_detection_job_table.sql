alter table "parcel_detection_job"
    add column if not exists point jsonb;