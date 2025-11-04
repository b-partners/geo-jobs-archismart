create table if not exists "parcel_detection_task"
(
    id             varchar primary key default uuid_generate_v4(),
    id_parcel      varchar,
    id_detection_task varchar,
    constraint id_parcel_pdt_fk foreign key (id_parcel) references "parcel" (id),
    constraint id_detection_task_pdt_fk foreign key (id_parcel) references "detection_task" (id)
);
create index if not exists id_parcel_pdt_idx on "parcel_detection_task" (id_parcel);
create index if not exists id_detection_task_pdt_idx on "parcel_detection_task" (id_detection_task);
