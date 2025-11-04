DO
$$
    begin
        if not exists (select from pg_type where typname = 'job_type') then
                    create type job_type as ENUM ('TILING', 'DETECTION');
        end if;
    end
$$;
create table if not exists "zone_job_status"
(
    id varchar primary key default uuid_generate_v4(),
    progression progression_status not null,
    health health_status not null,
    job_type job_type,
    creation_datetime timestamp without time zone not null default now()::timestamp without time zone,
    job_id varchar,
    message varchar
);
