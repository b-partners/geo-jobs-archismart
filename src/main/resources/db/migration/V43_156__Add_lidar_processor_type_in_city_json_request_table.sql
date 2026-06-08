do
$$
    begin
        if not exists (select from pg_type where typname = 'lidar_processor_type') then
            create type lidar_processor_type as ENUM ('DEFAULT', 'THREE_D_BAG_ROOFER');
        end if;
    end
$$;

alter table city_json_request
    add column if not exists lidar_processor_type lidar_processor_type;