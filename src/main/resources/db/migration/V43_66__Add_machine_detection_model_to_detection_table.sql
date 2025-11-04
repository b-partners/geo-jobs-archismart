alter table if exists full_detection
    add column if not exists bp_toiture_model jsonb,
    add column if not exists bp_lom_model     jsonb;