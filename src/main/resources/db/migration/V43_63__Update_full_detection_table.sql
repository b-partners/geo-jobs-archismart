alter table if exists full_detection
    drop column if exists detectable_object_configuration;

alter table if exists full_detection
    add column if not exists detectable_object_configurations jsonb,
    add column if not exists detection_overall_configuration  jsonb,
    add column if not exists geo_json_zone                    jsonb;