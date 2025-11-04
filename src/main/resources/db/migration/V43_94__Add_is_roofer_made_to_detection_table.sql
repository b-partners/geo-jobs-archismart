alter table if exists "detection"
    add column if not exists is_roofer_made boolean default false;