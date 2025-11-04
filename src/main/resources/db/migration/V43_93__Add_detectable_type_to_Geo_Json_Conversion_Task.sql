alter table if exists "geo_json_conversion_task"
    add column if not exists detectable_type detectable_type;