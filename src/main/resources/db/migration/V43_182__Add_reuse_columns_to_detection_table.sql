alter table "detection"
    add column if not exists zone_hash           varchar,
    add column if not exists inference_signature varchar,
    add column if not exists source_detection_id varchar;

create index if not exists detection_community_owner_id_zone_hash_idx
    on "detection" (community_owner_id, zone_hash)
    where zone_hash is not null;
