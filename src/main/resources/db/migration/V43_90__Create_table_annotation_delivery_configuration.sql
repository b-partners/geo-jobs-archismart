create table if not exists annotation_delivery_configuration
(
    id                              varchar primary key                  default uuid_generate_v4(),
    minimum_confidence_for_delivery numeric                     not null check (minimum_confidence_for_delivery >= 0 and
                                                                                minimum_confidence_for_delivery <= 1),
    creation_datetime               timestamp without time zone not null default now()::timestamp without time zone);

delete
from annotation_delivery_configuration
where id = '4acd34c5-a934-481c-9745-582c762818dd';

insert into annotation_delivery_configuration (id, minimum_confidence_for_delivery)
values ('4acd34c5-a934-481c-9745-582c762818dd', 1.0);