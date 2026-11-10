ALTER TABLE companies
    ADD COLUMN country_code integer,
    ADD COLUMN founded_date date,
    ADD COLUMN founded_date_format bigint,
    ADD COLUMN logo_image_id text;
