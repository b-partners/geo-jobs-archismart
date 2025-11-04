alter table if exists detection
    add column if not exists bp_zan_model jsonb;