create table community_authorization (
    id varchar primary key default uuid_generate_v4(),
    name varchar not null,
    api_key varchar unique not null,
    max_surface double precision check (max_surface >= 0) not null
);
