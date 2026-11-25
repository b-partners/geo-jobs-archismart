alter table "detection"
    add column if not exists zone_min_lon double precision,
    add column if not exists zone_min_lat double precision,
    add column if not exists zone_max_lon double precision,
    add column if not exists zone_max_lat double precision;

create index if not exists detection_reuse_nearby_zone_idx
    on "detection" (community_owner_id, inference_signature, zone_min_lon, zone_min_lat)
    where zone_min_lon is not null and geojson_s3_file_key is not null;
