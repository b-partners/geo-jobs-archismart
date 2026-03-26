alter table if exists "detection"
    add integration_test boolean not null default false;

create index if not exists detection_integration_test_idx on "detection" (integration_test);

