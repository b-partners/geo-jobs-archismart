delete from cached_area_picture_map_layer;

alter table cached_area_picture_map_layer
    add column if not exists layer_year integer,
    add column if not exists layer_source varchar;
