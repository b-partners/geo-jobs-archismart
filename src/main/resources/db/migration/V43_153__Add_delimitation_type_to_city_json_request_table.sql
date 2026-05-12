alter type geo_json_delimitation_type add value 'USER_DEFINED_DELIMITATION';
alter type geo_json_delimitation_type add value 'PARCEL_FREE_DELIMITATION';
alter type geo_json_delimitation_type add value 'PARCEL_CONSTRAINED_DELIMITATION';

alter table "city_json_request"
    add column if not exists "delimitation_type" geo_json_delimitation_type;