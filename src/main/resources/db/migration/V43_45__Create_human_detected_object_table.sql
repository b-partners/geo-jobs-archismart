create table if not exists "human_detected_object" (
   id varchar primary key,
   feature jsonb,
   human_detected_tile_id varchar references "human_detected_tile"(id),
   label jsonb,
   confidence varchar
);
