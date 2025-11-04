alter table if exists detectable_object_configuration
    add column if not exists detection_id varchar;