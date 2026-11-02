ALTER TABLE entries ADD COLUMN achievements_unlocked INTEGER;
ALTER TABLE entries ADD COLUMN achievements_total INTEGER;
ALTER TABLE entries ADD CONSTRAINT entries_achievements_check CHECK (
    (achievements_unlocked IS NULL OR achievements_unlocked >= 0)
    AND (achievements_total IS NULL OR achievements_total >= 0)
    AND (achievements_unlocked IS NULL OR achievements_total IS NULL OR achievements_unlocked <= achievements_total)
);
