alter table if exists "detection_step"
    add column if not exists message varchar;
