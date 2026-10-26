alter table if exists city_json_request
    add column if not exists message varchar;
