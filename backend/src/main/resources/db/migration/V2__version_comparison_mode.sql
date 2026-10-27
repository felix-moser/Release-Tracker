ALTER TABLE releases ADD COLUMN version_comparison TEXT NOT NULL DEFAULT 'ANY_DIFFERENT'
    CHECK (version_comparison IN ('ANY_DIFFERENT', 'HIGHER_ONLY'));
