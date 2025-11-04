DO
$$
    begin
        if not exists (select from pg_type where typname = 'role') then
            create type role as ENUM ('ROLE_ADMIN', 'ROLE_COMMUNITY', 'ROLE_INSURANCE');
        end if;
    end
$$;


alter table if exists "community_authorization" add column role role default 'ROLE_COMMUNITY';

update "community_authorization" set role='ROLE_COMMUNITY';