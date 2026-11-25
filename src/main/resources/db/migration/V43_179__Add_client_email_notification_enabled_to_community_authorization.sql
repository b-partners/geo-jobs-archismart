alter table if exists "community_authorization"
    add column if not exists client_email_notification_enabled boolean not null default false;
