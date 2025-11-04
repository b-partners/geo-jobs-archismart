DO
$$
    begin
        if not exists (select from pg_type where typname = 'detection_type') then
             create type detection_type as ENUM ('MACHINE', 'HUMAN');
        end if;
    end
$$;

alter table "zone_detection_job"
    add column if not exists type detection_type;