alter table if exists full_detection
    add column if not exists shape_file_key varchar;