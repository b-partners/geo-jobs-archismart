DO
$$
    begin
        if not exists (select from pg_type where typname = 'detection_feature_type') then
            create type detection_feature_type as ENUM ('PROVIDED_FEATURE');
        end if;
    end
$$;

create table if not exists "detection_feature"
(
    id                     varchar primary key         default uuid_generate_v4(),
    id_detection           varchar not null,
    id_feature             varchar not null,
    feature                jsonb,
    detection_feature_type detection_feature_type,
    creation_datetime      timestamp without time zone default current_timestamp,
    foreign key (id_detection) references detection (id)
);
create index detection_feature_idx on "detection_feature" (id_detection);
create index detection_feature_property_idx on "detection_feature" (id_feature);
create index detection_feature_type_idx on "detection_feature" (detection_feature_type);