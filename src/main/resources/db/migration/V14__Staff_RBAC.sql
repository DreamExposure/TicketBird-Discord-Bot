ALTER TABLE projects
    ADD COLUMN staff_users LONGTEXT NULL
    AFTER project_prefix;

ALTER TABLE projects
    ADD COLUMN staff_roles LONGTEXT NULL
    AFTER staff_users;
