ALTER TABLE jforum_topics CHANGE topic_views topic_views INT DEFAULT 0;

--
-- Table structure for table 'jforum_mail_integration'
--
DROP TABLE IF EXISTS jforum_mail_integration;
CREATE TABLE jforum_mail_integration (
	forum_id INT NOT NULL,
	forum_email VARCHAR(100) NOT NULL,
	pop_username VARCHAR(100) NOT NULL,
	pop_password VARCHAR(100) NOT NULL,
	pop_host VARCHAR(100) NOT NULL,
	pop_port INT DEFAULT 110,
	pop_ssl TINYINT DEFAULT 0,
	KEY (forum_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_api'
--
DROP TABLE IF EXISTS jforum_api;
CREATE TABLE jforum_api (
	api_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	api_key VARCHAR(32) NOT NULL,
	api_validity DATETIME NOT NULL
) ENGINE=InnoDB;