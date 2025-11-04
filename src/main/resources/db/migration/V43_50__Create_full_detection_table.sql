create table full_detection (
    id varchar primary key default uuid_generate_v4(),
    end_to_end_id varchar not null,
    ztj_id varchar references zone_tiling_job("id"),
    zdj_id varchar references zone_detection_job("id"),
    detectable_object_configuration jsonb,
    geojson_s3_file_key varchar
);
