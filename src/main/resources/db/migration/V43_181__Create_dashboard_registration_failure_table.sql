create table if not exists "dashboard_registration_failure"
(
    id                    varchar primary key,
    type                  varchar not null,
    subject_id            varchar not null,
    community_owner_id    varchar,
    project_key           varchar,
    payload               text,
    error_message         text,
    attempts              integer not null default 1,
    status                varchar not null,
    creation_datetime     timestamp without time zone default current_timestamp,
    last_attempt_datetime timestamp without time zone,
    reprocessed_datetime  timestamp without time zone
);

create index if not exists dashboard_registration_failure_status_idx
    on "dashboard_registration_failure" (status);

create index if not exists dashboard_registration_failure_type_subject_idx
    on "dashboard_registration_failure" (type, subject_id);
