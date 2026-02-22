alter table city_json_request
    add column if not exists feature_with_delimitation jsonb;