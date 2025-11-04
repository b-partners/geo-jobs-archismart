alter table "parcel_detection_task"
drop constraint if exists id_detection_task_pdt_fk,
add constraint id_detection_task_pdt_fk foreign key (id_detection_task) references "detection_task" (id);