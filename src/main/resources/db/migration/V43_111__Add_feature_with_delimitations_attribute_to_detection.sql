alter table "detection"
    add column if not exists feature_with_delimitations jsonb;