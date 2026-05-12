create table if not exists "city_json_texture"
(
    id                   varchar primary key,
    city_json_request_id varchar references city_json_request (id),
    top_left_longitude   double precision,
    top_left_latitude    double precision,
    pixel_width          double precision,
    pixel_height         double precision,
    shear_x              double precision,
    shear_y              double precision,
    image_width          integer,
    image_height         integer,
    image_uri            varchar
);