ALTER TABLE jforum_users ADD COLUMN user_twitter VARCHAR2(50) DEFAULT NULL;

ALTER TABLE jforum_moderation_log ALTER COLUMN post_id SET DEFAULT 0;
ALTER TABLE jforum_moderation_log ALTER COLUMN topic_id SET DEFAULT 0;
ALTER TABLE jforum_moderation_log ALTER COLUMN post_user_id SET DEFAULT 0;

ALTER TABLE jforum_mail_integration MODIFY pop_port NUMBER(5);