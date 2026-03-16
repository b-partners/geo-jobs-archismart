DO
$$
    begin
        if not exists (select from pg_type where typname = 'geo_coding_job_status') then
            create type geo_coding_job_status as ENUM ('PENDING','PROCESSING', 'SUCCEEDED','FAILED');
        end if;
    end
$$;


create table if not exists "geo_coding_job"
(
    id                 varchar primary key,
    end_to_end_id      varchar                     not null,
    community_owner_id varchar references community_authorization (id),
    file_key           varchar,
    geo_json_key       varchar,
    status             geo_coding_job_status,
    creation_datetime  timestamp without time zone not null
);

create index if not exists "geo_coding_job_end_to_end_id_idx" on "geo_coding_job" (end_to_end_id);
create index if not exists "geo_coding_job_community_owner_id_idx" on "geo_coding_job" (community_owner_id);