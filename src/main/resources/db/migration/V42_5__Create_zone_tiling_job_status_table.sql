DO
$$
    begin
        if not exists (select from pg_type where typname = 'progression_status') then
            create type progression_status as ENUM ('PENDING', 'PROCESSING', 'FINISHED');
        end if;
        if not exists (select from pg_type where typname = 'health_status') then
            create type health_status as ENUM ('UNKNOWN', 'SUCCEEDED', 'FAILED');
        end if;
    end
$$;
create table if not exists "zone_tiling_job_status"
(
    id varchar primary key default uuid_generate_v4(),
    progression progression_status not null,
    health health_status not null,
    creation_datetime timestamp without time zone not null default now()::timestamp without time zone,
    job_id varchar references zone_tiling_job("id"),
    message varchar
);
