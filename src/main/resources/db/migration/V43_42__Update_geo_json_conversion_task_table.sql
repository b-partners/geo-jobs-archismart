alter table "geo_json_conversion_task" drop column if exists geo_json_url;
alter table "geo_json_conversion_task" add column if not exists file_key varchar;