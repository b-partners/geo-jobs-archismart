DO
$$
    begin
        if not exists (select from pg_type where typname = 'city_json_delimitation_object_type') then
            create type city_json_delimitation_object_type as ENUM ('BUILDING_ROOF', 'BUILDING_ROOF_SEGMENT_FACE');
        end if;
    end
$$;

alter table city_json_request
    add column if not exists delimitation_object_type city_json_delimitation_object_type;
