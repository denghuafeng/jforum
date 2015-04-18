--
-- Table structure for table 'jforum_spam'
--
DROP TABLE IF EXISTS jforum_spam;
CREATE TABLE jforum_spam (
  pattern VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- more characters for topic title
ALTER TABLE jforum_topics MODIFY topic_title VARCHAR(120);
ALTER TABLE jforum_posts_text MODIFY post_subject VARCHAR(130);

-- more characters for more secure hash
ALTER TABLE jforum_users MODIFY user_password VARCHAR(128);
-- more characters for external URL
ALTER TABLE jforum_users MODIFY user_avatar VARCHAR(255);