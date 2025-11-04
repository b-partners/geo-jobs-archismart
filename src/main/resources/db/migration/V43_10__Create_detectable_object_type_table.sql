DO
$$
    begin
        if not exists (select from pg_type where typname = 'detectable_type') then
            create type detectable_type as ENUM ('ROOF', 'SOLAR_PANEL', 'PATHWAY', 'POOL', 'TREE');
        end if;
    end
$$;

create table if not exists "detectable_object_type"
(
    id varchar primary key default uuid_generate_v4(),
    detectable_type detectable_type not null,
    object_id varchar references detected_object("id")
);
