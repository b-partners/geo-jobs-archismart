DO
$$
    begin
        if not exists (select from pg_type where typname = 'city_json_request_step') then
            create type city_json_request_step as ENUM ('REQUEST_ACCEPTED', 'POINTS_CLOUD_PRE_PROCESSING', 'GEOMETRY_CONSTRUCTION', 'POST_PROCESSING');
        end if;
    end
$$;
alter table city_json_request
    add column if not exists step city_json_request_step;