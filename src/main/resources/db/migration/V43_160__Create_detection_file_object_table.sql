do
$$
    begin
        if not exists (select from pg_type where typname = 'detection_file_object_type') then
            create type detection_file_object_type as ENUM ('TILE_IMAGE', 'TILE_MASK', 'ASSEMBLE_VGG', 'ASSEMBLE_IMAGE', 'GEOJSON');
        end if;
    end
$$;

create table if not exists detection_file_object
(
    id                varchar primary key         default uuid_generate_v4(),
    id_detection      varchar not null,
    file_name         varchar not null,
    file_type         detection_file_object_type,
    bucket_key        varchar not null,
    creation_datetime timestamp without time zone default current_timestamp,
    constraint fk_detection_file_object_detection foreign key (id_detection) references detection (id)
);

create index if not exists detection_file_object_id_detection_idx on detection_file_object (id_detection);