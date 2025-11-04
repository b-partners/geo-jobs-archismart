create table community_authorized_zone
(
    id   varchar primary key default uuid_generate_v4(),
    name varchar not null,
    multi_polygon jsonb not null,
    community_authorization_id varchar not null,
    constraint community_authorization_id_fk foreign key (community_authorization_id) references "community_authorization" (id)
);
