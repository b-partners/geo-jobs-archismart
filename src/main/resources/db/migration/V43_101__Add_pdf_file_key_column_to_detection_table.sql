alter table if exists "detection"
    add column if not exists pdf_file_key varchar;