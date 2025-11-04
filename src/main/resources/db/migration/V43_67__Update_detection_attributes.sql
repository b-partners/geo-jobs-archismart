alter table if exists full_detection
    add column if not exists email_receiver varchar,
    add column if not exists zone_name      varchar;

alter table if exists full_detection
    drop column if exists detection_overall_configuration;

alter table if exists full_detection
    add column if not exists geo_server_properties jsonb;