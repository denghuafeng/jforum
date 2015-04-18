--
-- Table structure for table 'jforum_banlist'
--
CREATE SEQUENCE jforum_banlist_seq;
CREATE TABLE jforum_banlist (
  banlist_id INT NOT NULL DEFAULT nextval('jforum_banlist_seq'),
  user_id INT DEFAULT 0,
  banlist_ip VARCHAR(15),
  banlist_email VARCHAR(255),
  PRIMARY KEY(banlist_id)
);
CREATE INDEX idx_banlist_user ON jforum_banlist(user_id);
CREATE INDEX idx_banlist_ip ON jforum_banlist(banlist_ip);
CREATE INDEX idx_banlist_email ON jforum_banlist(banlist_email);

--
-- Table structure for table 'jforum_categories'
--
CREATE SEQUENCE jforum_categories_seq;
CREATE SEQUENCE jforum_categories_order_seq;
CREATE TABLE jforum_categories (
  categories_id INT NOT NULL PRIMARY KEY DEFAULT nextval('jforum_categories_seq'),
  title VARCHAR(100) NOT NULL DEFAULT '',
  display_order INT NOT NULL DEFAULT 0,
  moderated INT DEFAULT 0
);

--
-- Table structure for table 'jforum_config'
--
CREATE SEQUENCE jforum_config_seq;
CREATE TABLE jforum_config (
  config_name VARCHAR(255) NOT NULL DEFAULT '',
  config_value VARCHAR(255) NOT NULL DEFAULT '',
  config_id int NOT NULL PRIMARY KEY DEFAULT nextval('jforum_config_seq')
);

--
-- Table structure for table 'jforum_forums'
--
CREATE SEQUENCE jforum_forums_seq;
CREATE TABLE jforum_forums (
  forum_id INT NOT NULL DEFAULT nextval('jforum_forums_seq'),
  categories_id INT NOT NULL DEFAULT 1,
  forum_name VARCHAR(150) NOT NULL DEFAULT '',
  forum_desc VARCHAR(255) DEFAULT NULL,
  forum_order INT DEFAULT 1,
  forum_topics INT NOT NULL DEFAULT 0,
  forum_last_post_id INT NOT NULL DEFAULT 0,
  moderated INT DEFAULT 0,
  PRIMARY KEY(forum_id)
);
CREATE INDEX idx_forums_categories_id ON jforum_forums(categories_id);

--
-- Table structure for table 'jforum_forums_watch'
--
CREATE TABLE jforum_forums_watch (
  forum_id INT NOT NULL,
  user_id INT NOT NULL,
  is_read INT DEFAULT 1
);
CREATE INDEX idx_fw_forum ON jforum_forums_watch(forum_id);
CREATE INDEX idx_fw_user ON jforum_forums_watch(user_id);

--
-- Table structure for table 'jforum_groups'
--
CREATE SEQUENCE jforum_groups_seq;
CREATE TABLE jforum_groups (
  group_id INT NOT NULL DEFAULT nextval('jforum_groups_seq'),
  group_name VARCHAR(40) NOT NULL DEFAULT '',
  group_description VARCHAR(255) DEFAULT NULL,
  parent_id INT DEFAULT 0,
  PRIMARY KEY(group_id)
);

--
-- Table structure for table 'jforum_user_groups'
--
CREATE TABLE jforum_user_groups (
  group_id INT NOT NULL,
  user_id INT NOT NULL
);
CREATE INDEX idx_ug_group ON jforum_user_groups(group_id);
CREATE INDEX idx_ug_user ON jforum_user_groups(user_id);

--
-- Table structure for table 'jforum_roles'
--
CREATE SEQUENCE jforum_roles_seq;
CREATE TABLE jforum_roles (
  role_id INT NOT NULL PRIMARY KEY DEFAULT nextval('jforum_roles_seq'),
  group_id INT DEFAULT 0,
  name VARCHAR(255) NOT NULL
);
CREATE INDEX idx_roles_group ON jforum_roles(group_id);
CREATE INDEX idx_roles_name ON jforum_roles(name);

--
-- Table structure for table 'jforum_role_values'
--
CREATE TABLE jforum_role_values (
  role_id INT NOT NULL,
  role_value VARCHAR(255)
);
CREATE INDEX idx_rv_role ON jforum_role_values(role_id);

--
-- Table structure for table 'jforum_posts'
--
CREATE SEQUENCE jforum_posts_seq;
CREATE TABLE jforum_posts (
  post_id INT NOT NULL DEFAULT nextval('jforum_posts_seq'),
  topic_id INT NOT NULL DEFAULT 0,
  forum_id INT NOT NULL DEFAULT 0,
  user_id INT NOT NULL DEFAULT 0,
  post_time TIMESTAMP WITH TIME ZONE DEFAULT NULL,
  poster_ip VARCHAR(15) DEFAULT NULL,
  enable_bbcode INT NOT NULL DEFAULT 1,
  enable_html INT NOT NULL DEFAULT 1,
  enable_smilies INT NOT NULL DEFAULT 1,
  enable_sig INT NOT NULL DEFAULT 1,
  post_edit_time TIMESTAMP WITH TIME ZONE DEFAULT NULL,
  post_edit_count INT NOT NULL DEFAULT 0,
  status INT DEFAULT 1,
  attach INT DEFAULT 0,
  need_moderate INT DEFAULT 0,
  PRIMARY KEY(post_id)
);
CREATE INDEX idx_posts_user ON jforum_posts(user_id);
CREATE INDEX idx_posts_topic ON jforum_posts(topic_id);
CREATE INDEX idx_posts_forum ON jforum_posts(forum_id);
CREATE INDEX idx_posts_time ON jforum_posts(post_time);
CREATE INDEX idx_posts_moderate ON jforum_posts(need_moderate);

--
-- Table structure for table 'jforum_posts_text'
--
CREATE TABLE jforum_posts_text (
  post_id INT NOT NULL,
  post_text TEXT,
  post_subject VARCHAR(130) DEFAULT NULL,
  PRIMARY KEY ( post_id )
);

--
-- Table structure for table 'jforum_privmsgs'
--
CREATE SEQUENCE jforum_privmsgs_seq;
CREATE TABLE jforum_privmsgs (
  privmsgs_id INT NOT NULL DEFAULT nextval('jforum_privmsgs_seq'),
  privmsgs_type INT NOT NULL DEFAULT 0,
  privmsgs_subject VARCHAR(255) NOT NULL DEFAULT '',
  privmsgs_from_userid INT NOT NULL DEFAULT 0,
  privmsgs_to_userid INT NOT NULL DEFAULT 0,
  privmsgs_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  privmsgs_ip VARCHAR(15) NOT NULL DEFAULT '',
  privmsgs_enable_bbcode INT NOT NULL DEFAULT 1,
  privmsgs_enable_html INT NOT NULL DEFAULT 0,
  privmsgs_enable_smilies INT NOT NULL DEFAULT 1,
  privmsgs_attach_sig INT NOT NULL DEFAULT 1,
  PRIMARY KEY(privmsgs_id)
);

--
-- Table structure for table 'jforum_privmsgs_text'
--
CREATE TABLE jforum_privmsgs_text (
  privmsgs_id INT NOT NULL,
  privmsgs_text TEXT
);
CREATE INDEX idx_pm_text_id ON jforum_privmsgs_text (privmsgs_id);

--
-- Table structure for table 'jforum_ranks'
--
CREATE SEQUENCE jforum_ranks_seq;
CREATE TABLE jforum_ranks (
  rank_id INT NOT NULL DEFAULT nextval('jforum_ranks_seq'),
  rank_title VARCHAR(50) NOT NULL DEFAULT '',
  rank_min INT NOT NULL DEFAULT 0,
  rank_special INT DEFAULT NULL,
  rank_image VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(rank_id)
);

--
-- Table structure for table 'jforum_sessions'
--
CREATE TABLE jforum_sessions (
  session_id VARCHAR(150) NOT NULL DEFAULT '',
  session_user_id INT NOT NULL DEFAULT 0,
  session_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  session_time INT NOT NULL DEFAULT 0,
  session_ip VARCHAR(15) NOT NULL DEFAULT '',
  session_page INT NOT NULL DEFAULT 0,
  session_logged_int INT DEFAULT NULL
);
CREATE INDEX idx_sess_user ON jforum_sessions(session_user_id);

--
-- Table structure for table 'jforum_smilies'
--
CREATE SEQUENCE jforum_smilies_seq;
CREATE TABLE jforum_smilies (
  smilie_id INT NOT NULL DEFAULT nextval('jforum_smilies_seq'),
  code VARCHAR(50) NOT NULL DEFAULT '',
  url VARCHAR(100) DEFAULT NULL,
  disk_name VARCHAR(255),
  PRIMARY KEY(smilie_id)
);

--
-- Table structure for table 'jforum_themes'
--
CREATE SEQUENCE jforum_themes_seq;
CREATE TABLE jforum_themes (
  themes_id INT NOT NULL DEFAULT nextval('jforum_themes_seq'),
  template_name VARCHAR(30) NOT NULL DEFAULT '',
  style_name VARCHAR(30) NOT NULL DEFAULT '',
  PRIMARY KEY(themes_id)
);

--
-- Table structure for table 'jforum_topics'
--
CREATE SEQUENCE jforum_topics_seq;
CREATE TABLE jforum_topics (
  topic_id INT NOT NULL DEFAULT nextval('jforum_topics_seq'),
  forum_id INT NOT NULL DEFAULT 0,
  topic_title VARCHAR(120) NOT NULL DEFAULT '',
  user_id INT NOT NULL DEFAULT 0,
  topic_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  topic_views INT DEFAULT 0,
  topic_replies INT DEFAULT 0,
  topic_status INT DEFAULT 0,
  topic_vote_id INT DEFAULT 0,
  topic_type INT DEFAULT 0,
  topic_first_post_id INT DEFAULT 0,
  topic_last_post_id INT NOT NULL DEFAULT 0,
  moderated INT DEFAULT 0,
  topic_moved_id INT DEFAULT 0,
  PRIMARY KEY(topic_id)
);
CREATE INDEX idx_topics_forum ON jforum_topics(forum_id);
CREATE INDEX idx_topics_user ON jforum_topics(user_id);
CREATE INDEX idx_topics_fp ON jforum_topics(topic_first_post_id);
CREATE INDEX idx_topics_lp ON jforum_topics(topic_last_post_id);
CREATE INDEX idx_topics_time ON jforum_topics(topic_time);
CREATE INDEX idx_topics_type ON jforum_topics(topic_type);
CREATE INDEX idx_topics_moved ON jforum_topics(topic_moved_id);
    
--
-- Table structure for table 'jforum_topics_watch'
--
CREATE TABLE jforum_topics_watch (
  topic_id INT NOT NULL DEFAULT 0,
  user_id INT NOT NULL DEFAULT 0,
  is_read INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_tw_topic ON jforum_topics_watch(topic_id);
CREATE INDEX idx_tw_user ON jforum_topics_watch(user_id);

--
-- Table structure for table 'jforum_users'
--
CREATE SEQUENCE jforum_users_seq;
CREATE TABLE jforum_users (
  user_id INT NOT NULL DEFAULT nextval('jforum_users_seq'),
  user_active INT DEFAULT NULL,
  username VARCHAR(50) NOT NULL DEFAULT '',
  user_password VARCHAR(128) NOT NULL DEFAULT '',
  user_session_time INT NOT NULL DEFAULT 0,
  user_session_page INT NOT NULL DEFAULT 0,
  user_lastvisit TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_regdate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_level INT DEFAULT NULL,
  user_posts INT NOT NULL DEFAULT 0,
  user_timezone VARCHAR(5) NOT NULL DEFAULT '',
  user_style INT DEFAULT NULL,
  user_lang VARCHAR(255) DEFAULT NULL,
  user_dateformat VARCHAR(20) NOT NULL DEFAULT '%d/%M/%Y %H:%i',
  user_new_privmsg INT NOT NULL DEFAULT 0,
  user_unread_privmsg INT NOT NULL DEFAULT 0,
  user_last_privmsg TIMESTAMP NULL,
  user_emailtime TIMESTAMP NULL,
  user_viewemail INT DEFAULT 0,
  user_attachsig INT DEFAULT 1,
  user_allowhtml INT DEFAULT 0,
  user_allowbbcode INT DEFAULT 1,
  user_allowsmilies INT DEFAULT 1,
  user_allowavatar INT DEFAULT 1,
  user_allow_pm INT DEFAULT 1,
  user_allow_viewonline INT DEFAULT 1,
  user_notify INT DEFAULT 1,
  user_notify_always INT DEFAULT 0,
  user_notify_text INT DEFAULT 0,  
  user_notify_pm INT DEFAULT 1,
  user_popup_pm INT DEFAULT 1,
  rank_id INT DEFAULT 0,
  user_avatar VARCHAR(255) DEFAULT NULL,
  user_avatar_type INT NOT NULL DEFAULT 0,
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
  deleted INT DEFAULT NULL,
  user_viewonline INT DEFAULT 1,
  security_hash VARCHAR(32),
  user_karma NUMERIC(10,2),
  user_authhash VARCHAR(32),
  user_twitter VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY(user_id)
);

--
-- Table structure for table 'jforum_vote_desc'
--
CREATE SEQUENCE jforum_vote_desc_seq;
CREATE TABLE jforum_vote_desc (
  vote_id INT NOT NULL DEFAULT nextval('jforum_vote_desc_seq'),
  topic_id INT NOT NULL DEFAULT 0,
  vote_text VARCHAR(255) NOT NULL DEFAULT '',
  vote_start TIMESTAMP NOT NULL,
  vote_length INT NOT NULL DEFAULT 0,
  PRIMARY KEY(vote_id)
);
CREATE INDEX idx_vd_topic ON jforum_vote_desc(topic_id);

--
-- Table structure for table 'jforum_vote_results'
--
CREATE TABLE jforum_vote_results (
  vote_id INT NOT NULL DEFAULT 0,
  vote_option_id INT NOT NULL DEFAULT 0,
  vote_option_text VARCHAR(255) NOT NULL DEFAULT '',
  vote_result INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vr_id ON jforum_vote_results(vote_id);

--
-- Table structure for table 'jforum_vote_voters'
--
CREATE TABLE jforum_vote_voters (
  vote_id INT NOT NULL DEFAULT 0,
  vote_user_id INT NOT NULL DEFAULT 0,
  vote_user_ip VARCHAR(15) NOT NULL DEFAULT ''
);
CREATE INDEX idx_vv_id ON jforum_vote_voters(vote_id);
CREATE INDEX idx_vv_user ON jforum_vote_voters(vote_user_id);

--
-- Table structure for table 'jforum_words'
--
CREATE SEQUENCE jforum_words_seq;
CREATE TABLE jforum_words (
  word_id INT NOT NULL DEFAULT nextval('jforum_words_seq'),
  word VARCHAR(100) NOT NULL DEFAULT '',
  replacement VARCHAR(100) NOT NULL DEFAULT '',
  PRIMARY KEY(word_id)
);

--
-- Table structure for table 'jforum_karma'
--
CREATE SEQUENCE jforum_karma_seq;
CREATE TABLE jforum_karma (
  karma_id INT NOT NULL DEFAULT nextval('jforum_karma_seq'),
  post_id INT NOT NULL,
  topic_id INT NOT NULL,
  post_user_id INT NOT NULL,
  from_user_id INT NOT NULL,
  points INT NOT NULL,
  rate_date TIMESTAMP DEFAULT NULL,
  PRIMARY KEY(karma_id)
);
CREATE INDEX idx_krm_post ON jforum_karma(post_id);
CREATE INDEX idx_krm_topic ON jforum_karma(topic_id);
CREATE INDEX idx_krm_user ON jforum_karma(post_user_id);
CREATE INDEX idx_krm_from ON jforum_karma(from_user_id);

--
-- Table structure for table 'jforum_bookmark'
--
CREATE SEQUENCE jforum_bookmarks_seq;
CREATE TABLE jforum_bookmarks (
  bookmark_id INT NOT NULL DEFAULT nextval('jforum_bookmarks_seq'),
  user_id INT NOT NULL,
  relation_id INT NOT NULL,
  relation_type INT NOT NULL,
  public_visible INT DEFAULT 1,
  title VARCHAR(255),
  description VARCHAR(255),
  PRIMARY KEY(bookmark_id)
);
CREATE INDEX idx_bok_user ON jforum_bookmarks(user_id);
CREATE INDEX idx_bok_rel ON jforum_bookmarks(relation_id);

-- 
-- Table structure for table 'jforum_quota_limit'
--
CREATE SEQUENCE jforum_quota_limit_seq;
CREATE TABLE jforum_quota_limit (
  quota_limit_id INT NOT NULL DEFAULT nextval('jforum_quota_limit_seq'),
  quota_desc VARCHAR(50) NOT NULL,
  quota_limit INT NOT NULL,
  quota_type INT DEFAULT 1,
  PRIMARY KEY(quota_limit_id)
);

--
-- Table structure for table 'jforum_extension_groups'
--
CREATE SEQUENCE jforum_extension_groups_seq;
CREATE TABLE jforum_extension_groups (
  extension_group_id INT NOT NULL DEFAULT nextval('jforum_extension_groups_seq'),
  name VARCHAR(100) NOT NULL,
  allow INT DEFAULT 1, 
  upload_icon VARCHAR(100),
  download_mode INT DEFAULT 1,
  PRIMARY KEY(extension_group_id)
);

-- 
-- Table structure for table 'jforum_extensions'
--
CREATE SEQUENCE jforum_extensions_seq;
CREATE TABLE jforum_extensions (
  extension_id INT NOT NULL DEFAULT nextval('jforum_extensions_seq'),
  extension_group_id INT NOT NULL,
  description VARCHAR(100),
  upload_icon VARCHAR(100),
  extension VARCHAR(10),
  allow INT DEFAULT 1,
  PRIMARY KEY(extension_id)
);
CREATE INDEX idx_ext_group ON jforum_extensions(extension_group_id);
CREATE INDEX idx_ext_ext ON jforum_extensions(extension);

--
-- Table structure for table 'jforum_attach'
--
CREATE SEQUENCE jforum_attach_seq;
CREATE TABLE jforum_attach (
  attach_id INT NOT NULL DEFAULT nextval('jforum_attach_seq'),
  post_id INT,
  privmsgs_id INT,
  user_id INT NOT NULL,
  PRIMARY KEY(attach_id)
);
CREATE INDEX idx_att_post ON jforum_attach(post_id);
CREATE INDEX idx_att_priv ON jforum_attach(privmsgs_id);
CREATE INDEX idx_att_user ON jforum_attach(user_id);

-- 
-- Table structure for table 'jforum_attach_desc'
--

CREATE SEQUENCE jforum_attach_desc_seq;
CREATE TABLE jforum_attach_desc (
  attach_desc_id INT NOT NULL PRIMARY KEY DEFAULT nextval('jforum_attach_desc_seq'),
  attach_id INT NOT NULL,
  physical_filename VARCHAR(255) NOT NULL,
  real_filename VARCHAR(255) NOT NULL,
  download_count INT,
  description VARCHAR(255),
  mimetype VARCHAR(85),
  filesize NUMERIC(20),
  upload_time DATE,
  thumb INT DEFAULT 0,
  extension_id INTEGER
);
CREATE INDEX idx_att_d_att ON jforum_attach_desc(attach_id);
CREATE INDEX idx_att_d_ext ON jforum_attach_desc(extension_id);

--
-- Table structure for table 'jforum_attach_quota'
--
CREATE SEQUENCE jforum_attach_quota_seq;
CREATE TABLE jforum_attach_quota (
  attach_quota_id INT NOT NULL DEFAULT nextval('jforum_attach_quota_seq'),
  group_id INT NOT NULL,
  quota_limit_id INT NOT NULL,
  PRIMARY KEY(attach_quota_id)
);
CREATE INDEX idx_aq_group ON jforum_attach_quota(group_id);
CREATE INDEX idx_aq_ql ON jforum_attach_quota(quota_limit_id);

--
-- Table structure for table 'jforum_banner'
--
CREATE SEQUENCE jforum_banner_seq;
CREATE TABLE jforum_banner (
  banner_id INT NOT NULL DEFAULT nextval('jforum_banner_seq'),
  banner_name VARCHAR(90),
  banner_placement INT NOT NULL DEFAULT 0,
  banner_description VARCHAR(250),
  banner_clicks INT NOT NULL DEFAULT 0,
  banner_views INT NOT NULL DEFAULT 0,
  banner_url VARCHAR(250),
  banner_weight INT NOT NULL DEFAULT 50,
  banner_active INT NOT NULL DEFAULT 0,
  banner_comment VARCHAR(250),
  banner_type INT NOT NULL DEFAULT 0,
  banner_width INT NOT NULL DEFAULT 0,
  banner_height INT NOT NULL DEFAULT 0,
  PRIMARY KEY(banner_id)
);

--
-- Table structure for table 'jforum_moderation_log'
-- 
CREATE SEQUENCE jforum_moderation_log_seq;
CREATE TABLE jforum_moderation_log (
  log_id INT NOT NULL DEFAULT nextval('jforum_moderation_log_seq'),
  user_id INT NOT NULL,
  log_description TEXT NOT NULL,
  log_original_message TEXT,
  log_date TIMESTAMP NOT NULL,
  log_type INT DEFAULT 0,
  post_id INT DEFAULT 0,
  topic_id INT DEFAULT 0,
  post_user_id INT DEFAULT 0,
  PRIMARY KEY(log_id)
);
CREATE INDEX idx_ml_user ON jforum_moderation_log(user_id);
CREATE INDEX idx_ml_post_user ON jforum_moderation_log(post_user_id);

--
-- Table structure for table 'jforum_mail_integration'
--
CREATE TABLE jforum_mail_integration (
  forum_id INT NOT NULL,
  forum_email VARCHAR(100) NOT NULL,
  pop_username VARCHAR(100) NOT NULL,
  pop_password VARCHAR(100) NOT NULL,
  pop_host VARCHAR(100) NOT NULL,
  pop_port INT DEFAULT 110,
  pop_ssl INT DEFAULT 0
);

CREATE INDEX idx_mi_forum ON jforum_mail_integration(forum_id);

--
-- Table structure for table 'jforum_api'
--
CREATE SEQUENCE jforum_api_seq;
CREATE TABLE jforum_api (
  api_id INT NOT NULL DEFAULT nextval('jforum_api_seq'),
  api_key VARCHAR(32) NOT NULL,
  api_validity TIMESTAMP NOT NULL,
  PRIMARY KEY(api_id)
);

--
-- Table structure for table 'jforum_spam'
--
CREATE TABLE jforum_spam (
  pattern varchar(100) NOT NULL
);

