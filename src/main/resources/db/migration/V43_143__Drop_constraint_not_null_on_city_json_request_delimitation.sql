DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'city_json_request'
              AND column_name = 'delimitations'
              AND is_nullable = 'NO'
        ) THEN
            ALTER TABLE city_json_request
                ALTER COLUMN delimitations DROP NOT NULL;
        END IF;
    END
$$;