alter table if exists "zone_tiling_job"
    add column if not exists is_roofer_made boolean default false;