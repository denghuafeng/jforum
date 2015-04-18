ALTER TABLE jforum_users ADD COLUMN user_twitter VARCHAR(50) DEFAULT NULL;

ALTER TABLE jforum_banner ALTER COLUMN banner_description VARCHAR(250);
ALTER TABLE jforum_banner ALTER COLUMN banner_url VARCHAR(250);
ALTER TABLE jforum_banner ALTER COLUMN banner_comment VARCHAR(250);
ALTER TABLE jforum_banner ALTER COLUMN banner_placement SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_clicks SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_views SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_active SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_type SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_width SET DEFAULT 0;
ALTER TABLE jforum_banner ALTER COLUMN banner_height SET DEFAULT 0;

ALTER TABLE jforum_moderation_log ALTER COLUMN post_id SET DEFAULT 0;
ALTER TABLE jforum_moderation_log ALTER COLUMN topic_id SET DEFAULT 0;
ALTER TABLE jforum_moderation_log ALTER COLUMN post_user_id SET DEFAULT 0;