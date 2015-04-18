--
-- Table structure for table 'jforum_banlist'
--
DROP TABLE IF EXISTS jforum_banlist;
CREATE TABLE jforum_banlist (
  banlist_id INT NOT NULL AUTO_INCREMENT,
  user_id INT,
  banlist_ip VARCHAR(15),
  banlist_email VARCHAR(255),
  PRIMARY KEY (banlist_id),
  INDEX idx_user (user_id),
  INDEX (banlist_ip),
  INDEX (banlist_email)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_categories'
--
DROP TABLE IF EXISTS jforum_categories;
CREATE TABLE jforum_categories (
  categories_id INT NOT NULL AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL DEFAULT '',
  display_order INT NOT NULL DEFAULT 0,
  moderated TINYINT(1) DEFAULT 0,
  PRIMARY KEY (categories_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_config'
--
DROP TABLE IF EXISTS jforum_config;
CREATE TABLE jforum_config (
  config_name VARCHAR(255) NOT NULL DEFAULT '',
  config_value VARCHAR(255) NOT NULL DEFAULT '',
  config_id INT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (config_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_forums'
--
DROP TABLE IF EXISTS jforum_forums;
CREATE TABLE jforum_forums (
  forum_id INT NOT NULL AUTO_INCREMENT,
  categories_id INT NOT NULL DEFAULT 1,
  forum_name VARCHAR(150) NOT NULL DEFAULT '',
  forum_desc VARCHAR(255) DEFAULT NULL,
  forum_order INT DEFAULT 1,
  forum_topics INT NOT NULL DEFAULT 0,
  forum_last_post_id INT NOT NULL DEFAULT 0,
  moderated TINYINT(1) DEFAULT 0,
  PRIMARY KEY (forum_id),
  KEY (categories_id),
  INDEX idx_forums_cats (categories_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_forums_watch'
--
DROP TABLE IF EXISTS jforum_forums_watch;
CREATE TABLE jforum_forums_watch (
  forum_id INT NOT NULL,
  user_id INT NOT NULL,
  INDEX idx_fw_forum (forum_id),
  INDEX idx_fw_user (user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_groups'
--
DROP TABLE IF EXISTS jforum_groups;
CREATE TABLE jforum_groups (
  group_id INT NOT NULL AUTO_INCREMENT,
  group_name VARCHAR(40) NOT NULL DEFAULT '',
  group_description VARCHAR(255) DEFAULT NULL,
  parent_id INT DEFAULT 0,
  PRIMARY KEY (group_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_user_groups'
--
DROP TABLE IF EXISTS jforum_user_groups;
CREATE TABLE jforum_user_groups (
  group_id INT NOT NULL,
  user_id INT NOT NULL,
  INDEX idx_group (group_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_roles'
--
DROP TABLE IF EXISTS jforum_roles;
CREATE TABLE jforum_roles (
  role_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  group_id INT DEFAULT 0,
  name VARCHAR(255) NOT NULL,
  INDEX idx_group (group_id),
  INDEX idx_name (name)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_role_values'
--
DROP TABLE IF EXISTS jforum_role_values;
CREATE TABLE jforum_role_values (
  role_id INT NOT NULL,
  role_value VARCHAR(255),
  INDEX idx_role (role_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_posts'
--
DROP TABLE IF EXISTS jforum_posts;
CREATE TABLE jforum_posts (
  post_id INT NOT NULL AUTO_INCREMENT,
  topic_id INT NOT NULL DEFAULT 0,
  forum_id INT NOT NULL DEFAULT 0,
  user_id INT NOT NULL DEFAULT 0,
  post_time DATETIME DEFAULT NULL,
  poster_ip VARCHAR(15) DEFAULT NULL,
  enable_bbcode TINYINT(1) NOT NULL DEFAULT 1,
  enable_html TINYINT(1) NOT NULL DEFAULT 1,
  enable_smilies TINYINT(1) NOT NULL DEFAULT 1,
  enable_sig TINYINT(1) NOT NULL DEFAULT 1,
  post_edit_time DATETIME DEFAULT NULL,
  post_edit_count INT NOT NULL DEFAULT 0,
  status TINYINT(1) DEFAULT 1,
  attach TINYINT(1) DEFAULT 0,
  need_moderate TINYINT(1) DEFAULT 0,
  PRIMARY KEY (post_id),
  KEY (user_id),
  KEY (topic_id),
  KEY (forum_id),
  KEY (post_time),
  INDEX (need_moderate)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_posts_text'
--
DROP TABLE IF EXISTS jforum_posts_text;
CREATE TABLE jforum_posts_text (
  post_id INT NOT NULL PRIMARY KEY,
  post_text TEXT,
  post_subject VARCHAR(130)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_privmsgs'
--
DROP TABLE IF EXISTS jforum_privmsgs;
CREATE TABLE jforum_privmsgs (
  privmsgs_id INT NOT NULL AUTO_INCREMENT,
  privmsgs_type TINYINT(4) NOT NULL DEFAULT 0,
  privmsgs_subject VARCHAR(255) NOT NULL DEFAULT '',
  privmsgs_from_userid INT NOT NULL DEFAULT 0,
  privmsgs_to_userid INT NOT NULL DEFAULT 0,
  privmsgs_date DATETIME DEFAULT NULL,
  privmsgs_ip VARCHAR(15) NOT NULL DEFAULT '',
  privmsgs_enable_bbcode TINYINT(1) NOT NULL DEFAULT 1,
  privmsgs_enable_html TINYINT(1) NOT NULL DEFAULT 0,
  privmsgs_enable_smilies TINYINT(1) NOT NULL DEFAULT 1,
  privmsgs_attach_sig TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (privmsgs_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_privmsgs_text'
--
DROP TABLE IF EXISTS jforum_privmsgs_text;
CREATE TABLE jforum_privmsgs_text (
  privmsgs_id INT NOT NULL,
  privmsgs_text TEXT,
  PRIMARY KEY (privmsgs_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_ranks'
--
DROP TABLE IF EXISTS jforum_ranks;
CREATE TABLE jforum_ranks (
  rank_id INT NOT NULL AUTO_INCREMENT,
  rank_title VARCHAR(50) NOT NULL DEFAULT '',
  rank_min INT NOT NULL DEFAULT 0,
  rank_special TINYINT(1) DEFAULT NULL,
  rank_image VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (rank_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_sessions'
--
DROP TABLE IF EXISTS jforum_sessions;
CREATE TABLE jforum_sessions (
  session_id VARCHAR(150) NOT NULL DEFAULT '',
  session_user_id INT NOT NULL DEFAULT 0,
  session_start DATETIME DEFAULT NULL,
  session_time BIGINT DEFAULT 0,
  session_ip VARCHAR(15) NOT NULL DEFAULT '',
  session_page INT(11) NOT NULL DEFAULT 0,
  session_logged_int TINYINT(1) DEFAULT NULL,
  INDEX idx_sessions_users (session_user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_smilies'
--
DROP TABLE IF EXISTS jforum_smilies;
CREATE TABLE jforum_smilies (
  smilie_id INT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL DEFAULT '',
  url VARCHAR(100) DEFAULT NULL,
  disk_name VARCHAR(255),
  PRIMARY KEY (smilie_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_themes'
--
DROP TABLE IF EXISTS jforum_themes;
CREATE TABLE jforum_themes (
  themes_id INT NOT NULL AUTO_INCREMENT,
  template_name VARCHAR(30) NOT NULL DEFAULT '',
  style_name VARCHAR(30) NOT NULL DEFAULT '',
  PRIMARY KEY (themes_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_topics'
--
DROP TABLE IF EXISTS jforum_topics;
CREATE TABLE jforum_topics (
  topic_id INT NOT NULL AUTO_INCREMENT,
  forum_id INT NOT NULL DEFAULT 0,
  topic_title VARCHAR(120) NOT NULL DEFAULT '',
  user_id INT NOT NULL DEFAULT 0,
  topic_time DATETIME DEFAULT NULL,
  topic_views INT DEFAULT 0,
  topic_replies INT DEFAULT 0,
  topic_status TINYINT(3) DEFAULT 0,
  topic_vote_id INT NOT NULL DEFAULT 0,
  topic_type TINYINT(3) DEFAULT 0,
  topic_first_post_id INT DEFAULT 0,
  topic_last_post_id INT NOT NULL DEFAULT 0,
  topic_moved_id INT DEFAULT 0,
  moderated TINYINT(1) DEFAULT 0,
  PRIMARY KEY (topic_id),
  KEY (forum_id),
  KEY (user_id),
  KEY (topic_first_post_id),
  KEY (topic_last_post_id),
  KEY (topic_moved_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_topics_watch'
--
DROP TABLE IF EXISTS jforum_topics_watch;
CREATE TABLE jforum_topics_watch (
  topic_id INT NOT NULL,
  user_id INT NOT NULL,
  is_read TINYINT(1) DEFAULT 1,
  INDEX idx_topic (topic_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_users'
--
DROP TABLE IF EXISTS jforum_users;
CREATE TABLE jforum_users (
  user_id INT NOT NULL AUTO_INCREMENT,
  user_active TINYINT(1) DEFAULT NULL,
  username VARCHAR(50) NOT NULL DEFAULT '',
  user_password VARCHAR(128) NOT NULL DEFAULT '',
  user_session_time BIGINT DEFAULT 0,
  user_session_page INT NOT NULL DEFAULT 0,
  user_lastvisit DATETIME DEFAULT NULL,
  user_regdate DATETIME DEFAULT NULL,
  user_level TINYINT(4) DEFAULT NULL,
  user_posts INT NOT NULL DEFAULT 0,
  user_timezone VARCHAR(5) NOT NULL DEFAULT '',
  user_style TINYINT(4) DEFAULT NULL,
  user_lang VARCHAR(255) NOT NULL DEFAULT '',
  user_dateformat VARCHAR(20) NOT NULL DEFAULT '%d/%M/%Y %H:%i',
  user_new_privmsg INT NOT NULL DEFAULT 0,
  user_unread_privmsg INT NOT NULL DEFAULT 0,
  user_last_privmsg DATETIME NULL,
  user_emailtime DATETIME DEFAULT NULL,
  user_viewemail TINYINT(1) DEFAULT 0,
  user_attachsig TINYINT(1) DEFAULT 1,
  user_allowhtml TINYINT(1) DEFAULT 0,
  user_allowbbcode TINYINT(1) DEFAULT 1,
  user_allowsmilies TINYINT(1) DEFAULT 1,
  user_allowavatar TINYINT(1) DEFAULT 1,
  user_allow_pm TINYINT(1) DEFAULT 1,
  user_allow_viewonline TINYINT(1) DEFAULT 1,
  user_notify TINYINT(1) DEFAULT 1,
  user_notify_always TINYINT(1) DEFAULT 0,
  user_notify_text TINYINT(1) DEFAULT 0,
  user_notify_pm TINYINT(1) DEFAULT 1,
  user_popup_pm TINYINT(1) DEFAULT 1,
  rank_id INT DEFAULT 0,
  user_avatar VARCHAR(255) DEFAULT NULL,
  user_avatar_type TINYINT(4) NOT NULL DEFAULT 0,
  user_email VARCHAR(255) NOT NULL DEFAULT '',
  user_icq VARCHAR(15) DEFAULT NULL,
  user_website VARCHAR(255) DEFAULT NULL,
  user_from VARCHAR(100) DEFAULT NULL,
  user_sig TEXT,
  user_sig_bbcode_uid VARCHAR(10) DEFAULT NULL,
  user_aim VARCHAR(255) DEFAULT NULL,
  user_yim VARCHAR(255) DEFAULT NULL,
  user_msnm VARCHAR(255) DEFAULT NULL,
  user_occ VARCHAR(100) DEFAULT NULL,
  user_interests VARCHAR(255) DEFAULT NULL,
  user_biography TEXT DEFAULT NULL,
  user_actkey VARCHAR(32) DEFAULT NULL,
  gender CHAR(1) DEFAULT NULL,
  themes_id INT DEFAULT NULL,
  deleted TINYINT(1) DEFAULT NULL,
  user_viewonline TINYINT(1) DEFAULT 1,
  security_hash VARCHAR(32),
  user_karma DOUBLE,
  user_authhash VARCHAR(32),
  user_twitter VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_vote_desc'
--
DROP TABLE IF EXISTS jforum_vote_desc;
CREATE TABLE jforum_vote_desc (
  vote_id INT NOT NULL AUTO_INCREMENT,
  topic_id INT NOT NULL DEFAULT 0,
  vote_text VARCHAR(255) NOT NULL DEFAULT '',
  vote_start DATETIME NOT NULL,
  vote_length INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (vote_id),
  INDEX (topic_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_vote_results'
--
DROP TABLE IF EXISTS jforum_vote_results;
CREATE TABLE jforum_vote_results (
  vote_id INT NOT NULL DEFAULT 0,
  vote_option_id TINYINT(4) NOT NULL DEFAULT 0,
  vote_option_text VARCHAR(255) NOT NULL DEFAULT '',
  vote_result INT(11) NOT NULL DEFAULT 0,
  INDEX (vote_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_vote_voters'
--
DROP TABLE IF EXISTS jforum_vote_voters;
CREATE TABLE jforum_vote_voters (
  vote_id INT NOT NULL DEFAULT 0,
  vote_user_id INT NOT NULL DEFAULT 0,
  vote_user_ip VARCHAR(15) NOT NULL DEFAULT '',
  INDEX (vote_id),
  INDEX (vote_user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_words'
--
DROP TABLE IF EXISTS jforum_words;
CREATE TABLE jforum_words (
  word_id INT NOT NULL AUTO_INCREMENT,
  word VARCHAR(100) NOT NULL DEFAULT '',
  replacement VARCHAR(100) NOT NULL DEFAULT '',
  PRIMARY KEY (word_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_karma'
--
DROP TABLE IF EXISTS jforum_karma;
CREATE TABLE jforum_karma (
  karma_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  post_id INT NOT NULL,
  topic_id INT NOT NULL,
  post_user_id INT NOT NULL,
  from_user_id INT NOT NULL,
  points INT NOT NULL,
  rate_date DATETIME NULL,
  KEY (post_id),
  KEY (topic_id),
  KEY (post_user_id),
  KEY (from_user_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_bookmark'
--
DROP TABLE IF EXISTS jforum_bookmarks;
CREATE TABLE jforum_bookmarks (
  bookmark_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  relation_id INT NOT NULL,
  relation_type INT NOT NULL,
  public_visible INT DEFAULT 1,
  title VARCHAR(255),
  description VARCHAR(255),
  INDEX book_idx_relation (relation_id),
  KEY (user_id)
) ENGINE=InnoDB;
-- 
-- Table structure for table 'jforum_quota_limit'
--
DROP TABLE IF EXISTS jforum_quota_limit;
CREATE TABLE jforum_quota_limit (
  quota_limit_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  quota_desc VARCHAR(50) NOT NULL,
  quota_limit INT NOT NULL,
  quota_type TINYINT(1) DEFAULT 1
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_extension_groups'
--
DROP TABLE IF EXISTS jforum_extension_groups;
CREATE TABLE jforum_extension_groups (
  extension_group_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  allow TINYINT(1) DEFAULT 1,
  upload_icon VARCHAR(100),
  download_mode TINYINT(1) DEFAULT 1
) ENGINE=InnoDB;

-- 
-- Table structure for table 'jforum_extensions'
--
DROP TABLE IF EXISTS jforum_extensions;
CREATE TABLE jforum_extensions (
  extension_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  extension_group_id INT NOT NULL,
  description VARCHAR(100),
  upload_icon VARCHAR(100),
  extension VARCHAR(10),
  allow TINYINT(1) DEFAULT 1,
  KEY (extension_group_id),
  INDEX (extension)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_attach'
--
DROP TABLE IF EXISTS jforum_attach;
CREATE TABLE jforum_attach (
  attach_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  post_id INT,
  privmsgs_id INT,
  user_id INT NOT NULL,
  INDEX idx_att_post (post_id),
  INDEX idx_att_priv (privmsgs_id),
  INDEX idx_att_user (user_id)
) ENGINE=InnoDB;

-- 
-- Table structure for table 'jforum_attach_desc'
--
DROP TABLE IF EXISTS jforum_attach_desc;
CREATE TABLE jforum_attach_desc (
  attach_desc_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  attach_id INT NOT NULL,
  physical_filename VARCHAR(255) NOT NULL,
  real_filename VARCHAR(255) NOT NULL,
  download_count INT,
  description VARCHAR(255),
  mimetype VARCHAR(85),
  filesize INT,
  upload_time DATETIME,
  thumb TINYINT(1) DEFAULT 0,
  extension_id INT,
  INDEX idx_att_d_att (attach_id),
  INDEX idx_att_d_ext (extension_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_attach_quota'
--
DROP TABLE IF EXISTS jforum_attach_quota;
CREATE TABLE jforum_attach_quota (
  attach_quota_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  group_id INT NOT NULL,
  quota_limit_id INT NOT NULL,
  KEY (group_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_banner'
--
DROP TABLE IF EXISTS jforum_banner;
CREATE TABLE jforum_banner (
  banner_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  banner_name VARCHAR(90),
  banner_placement INT NOT NULL DEFAULT 0,
  banner_description VARCHAR(250),
  banner_clicks INT NOT NULL DEFAULT 0,
  banner_views INT NOT NULL DEFAULT 0,
  banner_url VARCHAR(250),
  banner_weight TINYINT(1) NOT NULL DEFAULT 50,
  banner_active TINYINT(1) NOT NULL DEFAULT 0,
  banner_comment VARCHAR(250),
  banner_type INT NOT NULL DEFAULT 0,
  banner_width INT NOT NULL DEFAULT 0,
  banner_height INT NOT NULL DEFAULT 0,
  KEY (banner_id)
) ENGINE=InnoDB;

--
-- Table structure for table 'jforum_moderation_log'
-- 
DROP TABLE IF EXISTS jforum_moderation_log;
CREATE TABLE jforum_moderation_log (
  log_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  log_description TEXT NOT NULL,
  log_original_message TEXT,
  log_date DATETIME NOT NULL,
  log_type TINYINT DEFAULT 0,
  post_id INT DEFAULT 0,
  topic_id INT DEFAULT 0,
  post_user_id INT DEFAULT 0,
  KEY (user_id),
  KEY (post_user_id)
) ENGINE=InnoDB;

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

--
-- Table structure for table 'jforum_spam'
--
DROP TABLE IF EXISTS jforum_spam;
CREATE TABLE jforum_spam (
  pattern VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

