ALTER TABLE jforum_users ADD COLUMN user_twitter VARCHAR(50) DEFAULT NULL;

ALTER TABLE jforum_vote_voters ALTER COLUMN vote_user_ip varchar(15);

ALTER TABLE jforum_words ALTER COLUMN word SET DEFAULT ('');