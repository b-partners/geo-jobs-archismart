alter table if exists "detection"
    add column if not exists detectable_object_model jsonb;