create table if not exists "model_card_version"
(
    id                varchar primary key,
    model_card_id     varchar                     not null,
    version           integer                     not null,
    creation_datetime timestamp without time zone not null default current_timestamp,
    unique (model_card_id, version)
);
