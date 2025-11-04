drop table if exists "human_detected_object";

alter table if exists "detected_object" add column if not exists detected_object_type_id varchar;

alter table "detected_object" drop constraint if exists detected_object_detected_tile_id_fkey;

alter table if exists "detected_object" add column if not exists type detection_type;
