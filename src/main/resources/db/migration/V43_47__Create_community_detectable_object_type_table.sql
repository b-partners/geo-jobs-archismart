create table community_detectable_object_type(
    id varchar primary key default uuid_generate_v4(),
    type detectable_type not null,
    community_authorization_id varchar not null,
    constraint community_authorization_id_fk foreign key (community_authorization_id) references "community_authorization" (id)
);