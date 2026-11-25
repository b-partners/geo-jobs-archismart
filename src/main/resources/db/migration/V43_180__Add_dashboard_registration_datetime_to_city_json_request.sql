alter table if exists "city_json_request"
    add column if not exists dashboard_registration_datetime timestamp without time zone;
