alter table if exists "detection"
    add column if not exists dashboard_registration_datetime timestamp without time zone;