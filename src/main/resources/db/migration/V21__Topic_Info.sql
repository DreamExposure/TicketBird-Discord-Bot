ALTER TABLE projects
    ADD COLUMN additional_info TEXT NULL DEFAULT NULL
    AFTER project_prefix;