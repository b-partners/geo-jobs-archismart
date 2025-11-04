DO
$$
    begin
        if not exists (select from pg_type where typname = 'surface_unit') then
            create type surface_unit as ENUM ('SQUARE_DEGREE', 'SQUARE_KILOMETER', 'SQUARE_METER', 'ARE', 'HECTARE');
        end if;
    end
$$;
