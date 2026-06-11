alter table city_json_texture
drop column if exists top_left_longitude,
drop column if exists top_left_latitude,
drop column if exists pixel_width,
drop column if exists pixel_height,
drop column if exists shear_x,
drop column if exists shear_y;
