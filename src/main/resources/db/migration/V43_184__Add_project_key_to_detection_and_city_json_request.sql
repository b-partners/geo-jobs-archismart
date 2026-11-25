alter table "detection"
    add column if not exists project_key        varchar,
    add column if not exists external_reference varchar;

alter table "city_json_request"
    add column if not exists project_key        varchar,
    add column if not exists external_reference varchar;
