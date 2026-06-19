alter table if exists detection
    add column if not exists debug_mode boolean default false;