ALTER TABLE tickets
    ADD COLUMN participants LONGTEXT NULL DEFAULT NULL
        AFTER creator;
