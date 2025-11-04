alter table if exists "community_authorization"
    add column if not exists email varchar,
    add constraint community_authorization_email_unique unique (email),
    add column if not exists is_api_key_revoked boolean not null default false;
