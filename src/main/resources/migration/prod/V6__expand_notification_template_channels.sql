ALTER TABLE notification_templates
    MODIFY channel ENUM ('PUSH', 'WEB', 'IN_APP', 'EMAIL', 'SMS') NOT NULL;
