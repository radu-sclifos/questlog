ALTER TABLE entries ADD COLUMN platform_id BIGINT;
ALTER TABLE entries ADD CONSTRAINT entries_platform_fk FOREIGN KEY (platform_id) REFERENCES platforms(id);
