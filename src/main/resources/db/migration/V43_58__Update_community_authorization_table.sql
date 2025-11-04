alter table if exists "community_authorization" add column if not exists max_surface_unit surface_unit not null default 'SQUARE_DEGREE';
