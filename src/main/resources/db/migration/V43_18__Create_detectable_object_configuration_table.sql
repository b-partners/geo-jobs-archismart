create table if not exists "detectable_object_configuration"
(
    id               varchar primary key default uuid_generate_v4(),
    detection_job_id varchar,
    object_type      detectable_type,
    confidence       numeric,
    constraint detectable_object_configuration_job_fk foreign key (detection_job_id)
        references "zone_detection_job" (id)
);