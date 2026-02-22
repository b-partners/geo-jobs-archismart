alter table "detection"
    add column if not exists detectable_object_model_list jsonb;