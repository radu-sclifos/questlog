ALTER TABLE companies
    ALTER COLUMN founded_date_format TYPE text USING founded_date_format::text;
