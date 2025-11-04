alter table if exists "detection"
    add column if not exists converted_addresses jsonb;