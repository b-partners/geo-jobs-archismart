create table if not exists "detected_object"
(
    id varchar primary key default uuid_generate_v4(),
    feature jsonb not null,
    detected_tile_id varchar references detected_tile("id")
);
