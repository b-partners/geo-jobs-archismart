create table if not exists revoked_api_key(
    id varchar primary key default uuid_generate_v4(),
    revoked_api_key_value varchar unique not null,
    revoked_at timestamp without time zone not null default now()::timestamp without time zone,
    community_owner_id varchar not null,
    constraint community_owner_id_fk foreign key (community_owner_id) references "community_authorization" (id)
);