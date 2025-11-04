create table if not exists "parcel"
(
    id   varchar primary key default uuid_generate_v4(),
    parcel_content jsonb
);
