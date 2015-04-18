--
-- Table structure for table 'jforum_spam'
--
CREATE TABLE jforum_spam (
  pattern nvarchar(100) NOT NULL
);

-- more characters for topic title
ALTER TABLE jforum_topics ALTER COLUMN topic_title nvarchar(120);
ALTER TABLE jforum_posts_text ALTER COLUMN post_subject nvarchar(130);

-- more characters for more secure hash
ALTER TABLE jforum_users ALTER COLUMN user_password VARCHAR(128);
-- more characters for external URL
ALTER TABLE jforum_users ALTER COLUMN user_avatar VARCHAR(255);