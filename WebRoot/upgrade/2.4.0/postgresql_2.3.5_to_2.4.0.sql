--
-- Table structure for table 'jforum_spam'
--
CREATE TABLE jforum_spam (
  pattern VARCHAR(100) NOT NULL
);

-- more characters for topic title
ALTER TABLE jforum_topics ALTER COLUMN topic_title TYPE VARCHAR(120);
ALTER TABLE jforum_posts_text ALTER COLUMN post_subject TYPE VARCHAR(130);

-- more characters for more secure hash
ALTER TABLE jforum_users ALTER COLUMN user_password TYPE VARCHAR(128);
-- more characters for external URL
ALTER TABLE jforum_users ALTER COLUMN user_avatar TYPE VARCHAR(255);