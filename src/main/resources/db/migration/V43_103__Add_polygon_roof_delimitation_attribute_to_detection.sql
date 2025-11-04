alter table if exists "detection"
    add column if not exists polygon_roof_delimitation jsonb;