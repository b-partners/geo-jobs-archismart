alter table if exists "detection"
    add column if not exists image_file_key varchar;