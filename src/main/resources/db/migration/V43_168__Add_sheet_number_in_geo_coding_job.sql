alter table if exists geo_coding_job
    add column if not exists sheet_index int;