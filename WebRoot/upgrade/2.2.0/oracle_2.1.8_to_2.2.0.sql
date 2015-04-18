ALTER TABLE jforum_topics MODIFY (topic_views DEFAULT 0);

--
-- Table structure for table 'jforum_mail_integration'
--
CREATE TABLE jforum_mail_integration (
	forum_id NUMBER(10) NOT NULL,
	forum_email VARCHAR2(100) NOT NULL,
	pop_username VARCHAR2(100) NOT NULL,
	pop_password VARCHAR2(100) NOT NULL,
	pop_host VARCHAR2(100) NOT NULL,
	pop_port NUMBER(10) DEFAULT 110,
	pop_ssl NUMBER(1) DEFAULT 0
);

CREATE INDEX idx_mi_forum ON jforum_mail_integration(forum_id);

--
-- Table structure for table 'jforum_api'
--
CREATE SEQUENCE jforum_api_seq
	INCREMENT BY 1
    START WITH 1 MAXVALUE 2.0E9 MINVALUE 1 NOCYCLE
    CACHE 200 ORDER;
    
CREATE TABLE jforum_api (
	api_id NUMBER(10) NOT NULL,
	api_key VARCHAR2(32) NOT NULL,
	api_validity DATE NOT NULL,
	PRIMARY KEY(api_id)
);