alter table "detected_tile"
    add column if not exists "job_id" varchar,
    add constraint detected_tile_job_id_fk foreign key (job_id) references "zone_detection_job" (id),
    add column if not exists "parcel_id" varchar,
    add constraint detected_tile_parcel_fk foreign key (parcel_id) references "parcel" (id);