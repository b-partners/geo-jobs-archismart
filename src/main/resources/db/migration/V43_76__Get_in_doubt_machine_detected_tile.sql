create or replace function get_detectable_object_configuration(zdj_id varchar)
    returns detectable_object_configuration
    language SQL
    immutable returns null on null input as
$$
    select * from detectable_object_configuration
        where detection_job_id = zdj_id;
$$;


create or replace function get_tiles_without_detected_object(zdj_id varchar)
    returns setof detected_tile
    language plpgsql as
$$
    begin
        return query
            select dtt.*
                from detected_tile dtt
                    where dtt.zdj_job_id = zdj_id
                        and dtt.id not in (select dto.detected_tile_id from detected_object dto);
    end;
$$;

create or replace function get_in_doubt_detected_tiles(zdj_id varchar, min_confidence_for_delivery double precision, is_greater boolean)
    returns setof detected_tile
    language plpgsql as
$$
    declare
        detectable_object_configuration record;
    begin
        drop table if exists temp_results;
        create temporary table temp_results as
            select dtt.* from detected_tile dtt where false;

            for detectable_object_configuration in
                select min_confidence_for_detection as reference_confidence, object_type as detected_object_type
                    from get_detectable_object_configuration(zdj_id)
                loop
                    insert into temp_results
                        select dtt.*
                            from detected_tile dtt
                                inner join detected_object dto on dto.detected_tile_id = dtt.id
                                inner join detectable_object_type dtobt on dtobt.id = dto.detected_object_type_id
                                    where dtt.zdj_job_id = zdj_id
                                        and dtobt.detectable_type = detectable_object_configuration.detected_object_type
                                        and detectable_object_configuration.reference_confidence >= dto.computed_confidence;
            end loop;

            return query select tmp.* from temp_results as tmp inner join detected_object dto on dto.detected_tile_id = tmp.id
                inner join detectable_object_type dtobt on dtobt.id = dto.detected_object_type_id
                    where (is_greater and dto.computed_confidence > min_confidence_for_delivery) or
                        (not is_greater and min_confidence_for_delivery >= dto.computed_confidence);
    end;
$$;