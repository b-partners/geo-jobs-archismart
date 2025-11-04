create table if not exists "task_statistic"
(
    id          varchar primary key         default uuid_generate_v4(),
    job_id      varchar,
    job_type    job_type,
    updated_at  timestamp without time zone default now()::timestamp without time zone,
    tiles_count numeric
);

create table if not exists "task_status_statistic"
(
    id                varchar primary key default uuid_generate_v4(),
    task_statistic_id varchar,
    progression       progression_status,
    constraint task_status_statistic_fk foreign key (task_statistic_id) references task_statistic (id)
);

create table if not exists "health_status_statistic"
(
    id                       varchar primary key default uuid_generate_v4(),
    task_status_statistic_id varchar,
    health_status            health_status,
    count                    numeric,
    constraint health_status_statistic_fk foreign key (task_status_statistic_id) references task_status_statistic (id)
);