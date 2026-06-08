alter table city_json_texture
    add column if not exists tile_x int;

alter table city_json_texture
    add column if not exists tile_y int;

alter table city_json_texture
    add column if not exists tile_image_size_px int;

alter table city_json_texture
    add column if not exists zoom int;
