create table if not exists "parcel_tiling_task"
(
    id             varchar primary key default uuid_generate_v4(),
    id_parcel      varchar,
    id_tiling_task varchar,
    constraint id_parcel_ptt_fk foreign key (id_parcel) references "parcel" (id),
    constraint id_tiling_task_ptt_fk foreign key (id_tiling_task) references "tiling_task" (id)
);
create index if not exists id_parcel_ptt_idx on "parcel_tiling_task" (id_parcel);
create index if not exists id_tiling_task_ptt_idx on "parcel_tiling_task" (id_tiling_task);
