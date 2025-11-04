alter table if exists "detection"
    add column if not exists vgg_file_key varchar;