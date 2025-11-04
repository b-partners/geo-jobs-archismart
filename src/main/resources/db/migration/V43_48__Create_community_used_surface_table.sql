create table community_used_surface(
    id varchar primary key default uuid_generate_v4(),
    used_surface double precision check (used_surface >= 0),
    usage_datetime timestamp without time zone not null default now()::timestamp without time zone,
    community_authorization_id varchar not null,
    constraint community_authorization_id_fk foreign key (community_authorization_id) references "community_authorization" (id)
);
