create table if not exists cached_area_picture_map_layer
(
    id                    varchar primary key,
    layer_name            varchar not null,
    precision_level_in_cm integer not null,
    cached_at             timestamp without time zone default current_timestamp
);
