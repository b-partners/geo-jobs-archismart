create table parcelized_polygon (
   id varchar primary key default uuid_generate_v4(),
   feature jsonb not null
);
