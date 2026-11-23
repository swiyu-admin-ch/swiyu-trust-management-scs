-- A statement tombstone carries no partner data any more, so its details column has to accept NULL.
ALTER TABLE trust_statement
    ALTER COLUMN details DROP NOT NULL;
