alter table if exists "full_detection"
    add column if not exists "community_owner_id" varchar,
    add constraint full_detection_community_owner_id_fk foreign key (community_owner_id) references "community_authorization"(id);