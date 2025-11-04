drop function if exists get_detectable_object_configuration(varchar);

create or replace function get_detectable_object_configuration(zdj_id varchar)
    returns setof detectable_object_configuration
    strict
    language SQL as
$$
select *
from detectable_object_configuration
where detection_job_id = zdj_id;
$$;
