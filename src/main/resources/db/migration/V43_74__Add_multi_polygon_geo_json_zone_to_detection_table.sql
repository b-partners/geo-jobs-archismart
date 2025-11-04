alter table if exists detection
    add column if not exists multi_polygon_geo_json_zone jsonb;