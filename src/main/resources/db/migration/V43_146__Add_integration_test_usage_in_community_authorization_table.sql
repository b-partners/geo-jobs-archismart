alter table if exists "community_authorization"
    add integration_test_usage boolean not null default false;

create index if not exists community_authorization_integration_test_usage_idx on "community_authorization" (integration_test_usage);