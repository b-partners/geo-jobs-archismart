create table if not exists "feature_delimitation_computing"
(
    id                            varchar primary key         default uuid_generate_v4(),
    detection_identifier          varchar,
    feature_properties_identifier varchar,
    feature_with_delimitation     jsonb,
    creation_datetime             timestamp without time zone default current_timestamp,
    foreign key (detection_identifier) references detection (id)
);

create index if not exists delimitation_computing_detection_idx on "feature_delimitation_computing" (detection_identifier);
create index if not exists delimitation_computing_feature_properties_identifier_idx on "feature_delimitation_computing" (feature_properties_identifier);