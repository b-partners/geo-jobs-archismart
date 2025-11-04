create table if not exists "human_detection_job"
(
    id                    varchar primary key default uuid_generate_v4(),
    annotation_job_id     varchar,
    zone_detection_job_id varchar,
    constraint fk_detection_job_human foreign key (zone_detection_job_id) references "zone_detection_job" (id)
);
create index if not exists human_detection_job_annotation_id_index on "human_detection_job" (annotation_job_id);
create index if not exists human_detection_job_zone_detection_job_id_index on "human_detection_job" (zone_detection_job_id);
