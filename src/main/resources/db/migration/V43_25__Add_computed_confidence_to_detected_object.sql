alter table "detected_object"
    add column if not exists computed_confidence numeric;