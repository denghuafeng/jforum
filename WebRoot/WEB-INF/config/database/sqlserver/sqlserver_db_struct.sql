IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_banlist') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_banlist;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_categories') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_categories;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_config') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_config;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_forums') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_forums;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_forums_watch') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_forums_watch;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_groups') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_groups;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_posts') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_posts;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_posts_text') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_posts_text;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_privmsgs') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_privmsgs;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_privmsgs_text') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_privmsgs_text;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_ranks') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_ranks;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_role_values') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_role_values;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_roles') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_roles;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_sessions') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_sessions;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_smilies') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_smilies;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_themes') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_themes;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_topics') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_topics;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_topics_watch') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_topics_watch;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_user_groups') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_user_groups;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_users') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_users;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_vote_desc') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_vote_desc;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_vote_results') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_vote_results;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_vote_voters') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_vote_voters;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_words') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_words;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_karma') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_karma;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_bookmarks') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_bookmarks;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_quota_limit') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_quota_limit;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_extension_groups') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_extension_groups;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_extensions') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_extensions;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_attach') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_attach;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_attach_desc') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_attach_desc;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_attach_quota') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_attach_quota;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_banner') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_banner;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_moderation_log') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_moderation_log;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_mail_integration') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_mail_integration;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_api') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_api;

IF EXISTS (SELECT * FROM dbo.sysobjects WHERE id = object_id(N'jforum_spam') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
DROP TABLE jforum_spam;

--
-- Table structure for table 'jforum_banlist'
--
CREATE TABLE jforum_banlist (
  banlist_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  user_id bigint DEFAULT (0),
  banlist_ip varchar(15),
  banlist_email varchar(255) 
);
CREATE INDEX idx_banlist_user ON jforum_banlist(user_id);
CREATE INDEX idx_banlist_ip ON jforum_banlist(banlist_ip);
CREATE INDEX idx_banlist_email ON jforum_banlist(banlist_email);

--
-- Table structure for table 'jforum_categories'
--
CREATE TABLE jforum_categories (
  categories_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  title nvarchar(100) DEFAULT ('') NOT NULL,
  display_order bigint DEFAULT (0) NOT NULL,
  moderated tinyint DEFAULT (0) 
);

--
-- Table structure for table 'jforum_config'
--
CREATE TABLE jforum_config (
  config_name varchar(255) DEFAULT ('') NOT NULL,
  config_value varchar(255) NOT NULL,
  config_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL 
);

--
-- Table structure for table 'jforum_forums'
--
CREATE TABLE jforum_forums (
  forum_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  categories_id bigint DEFAULT (1) NOT NULL,
  forum_name nvarchar(150) DEFAULT ('') NOT NULL,
  forum_desc nvarchar(255) DEFAULT NULL,
  forum_order bigint DEFAULT (1) NULL,
  forum_topics bigint DEFAULT (0) NOT NULL,
  forum_last_post_id bigint DEFAULT (0) NOT NULL,
  moderated tinyint DEFAULT (0) NULL 
);
CREATE INDEX idx_forums_categories_id ON jforum_forums(categories_id);

--
-- Table structure for table 'jforum_forums_watch'
--
CREATE TABLE jforum_forums_watch (
  forum_id int NOT NULL,
  user_id int NOT NULL,
  is_read int DEFAULT (1) NULL,
);
CREATE INDEX idx_fw_forum ON jforum_forums_watch(forum_id);
CREATE INDEX idx_fw_user ON jforum_forums_watch(user_id);

--
-- Table structure for table 'jforum_groups'
--
CREATE TABLE jforum_groups (
  group_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  group_name nvarchar(40) DEFAULT ('') NOT NULL,
  group_description nvarchar(255) DEFAULT NULL,
  parent_id bigint DEFAULT (0) NULL 
);

--
-- Table structure for table 'jforum_user_groups'
--
CREATE TABLE jforum_user_groups (
  group_id bigint NOT NULL,
  user_id bigint NOT NULL 
);
CREATE INDEX idx_ug_group ON jforum_user_groups(group_id);
CREATE INDEX idx_ug_user ON jforum_user_groups(user_id);

--
-- Table structure for table 'jforum_roles'
--
CREATE TABLE jforum_roles (
  role_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  group_id bigint DEFAULT (0) NULL,
  name nvarchar(255) NOT NULL  
);
CREATE INDEX idx_roles_group ON jforum_roles(group_id);
CREATE INDEX idx_roles_name ON jforum_roles(name);

--
-- Table structure for table 'jforum_role_values'
--
CREATE TABLE jforum_role_values (
  role_id bigint NOT NULL,
  role_value nvarchar(255) NULL 
);
CREATE INDEX idx_rv_role ON jforum_role_values(role_id);

--
-- Table structure for table 'jforum_posts'
--
CREATE TABLE jforum_posts (
  post_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  topic_id bigint DEFAULT (0) NOT NULL,
  forum_id bigint DEFAULT (0) NOT NULL,
  user_id bigint DEFAULT (0) NOT NULL,
  post_time datetime DEFAULT NULL,
  poster_ip varchar(15) DEFAULT NULL,
  enable_bbcode tinyint DEFAULT (1) NOT NULL,
  enable_html tinyint DEFAULT (1)  NOT NULL,
  enable_smilies tinyint DEFAULT (1) NOT NULL,
  enable_sig tinyint DEFAULT (1) NOT NULL,
  post_edit_time datetime DEFAULT NULL,
  post_edit_count bigint DEFAULT (0) NOT NULL,
  status tinyint DEFAULT (1),
  attach tinyint DEFAULT (0),
  need_moderate tinyint DEFAULT (0)
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
  post_id bigint PRIMARY KEY NOT NULL,
  post_text ntext,
  post_subject nvarchar(130) NULL 
);

--
-- Table structure for table 'jforum_privmsgs'
--
CREATE TABLE jforum_privmsgs (
  privmsgs_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  privmsgs_type tinyint DEFAULT (0) NOT NULL,
  privmsgs_subject nvarchar(255) DEFAULT ('') NOT NULL,
  privmsgs_from_userid bigint DEFAULT (0) NOT NULL,
  privmsgs_to_userid bigint DEFAULT (0) NOT NULL,
  privmsgs_date datetime DEFAULT NULL,
  privmsgs_ip varchar(15) DEFAULT ('') NOT NULL,
  privmsgs_enable_bbcode tinyint DEFAULT (1) NOT NULL,
  privmsgs_enable_html tinyint DEFAULT (0) NOT NULL,
  privmsgs_enable_smilies tinyint DEFAULT (1) NOT NULL,
  privmsgs_attach_sig tinyint DEFAULT (1) NOT NULL 
);

--
-- Table structure for table 'jforum_privmsgs_text'
--
CREATE TABLE jforum_privmsgs_text (
  privmsgs_id bigint PRIMARY KEY NOT NULL,
  privmsgs_text ntext 
);
CREATE INDEX idx_pm_text_id ON jforum_privmsgs_text (privmsgs_id);

--
-- Table structure for table 'jforum_ranks'
--
CREATE TABLE jforum_ranks (
  rank_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  rank_title nvarchar(50) DEFAULT ('') NOT NULL,
  rank_min bigint DEFAULT (0) NOT NULL,
  rank_special tinyint DEFAULT NULL,
  rank_image varchar(255) NULL 
);

--
-- Table structure for table 'jforum_sessions'
--
CREATE TABLE jforum_sessions (
  session_id varchar(150) DEFAULT ('') NOT NULL,
  session_user_id bigint DEFAULT (0) NOT NULL,
  session_start datetime DEFAULT NULL,
  session_time bigint DEFAULT (0) NULL,
  session_ip varchar(15) DEFAULT ('') NOT NULL,
  session_page bigint DEFAULT (0) NOT NULL,
  session_logged_int tinyint DEFAULT NULL 
);
CREATE INDEX idx_sess_user ON jforum_sessions(session_user_id);  

--
-- Table structure for table 'jforum_smilies'
--
CREATE TABLE jforum_smilies (
  smilie_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  code varchar(50) NOT NULL,
  url varchar(100) DEFAULT NULL,
  disk_name varchar(255) NULL 
);

--
-- Table structure for table 'jforum_themes'
--
CREATE TABLE jforum_themes (
  themes_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  template_name varchar(30) DEFAULT ('') NOT NULL,
  style_name varchar(30) DEFAULT ('') NOT NULL 
);

--
-- Table structure for table 'jforum_topics'
--
CREATE TABLE jforum_topics (
  topic_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  forum_id int DEFAULT (0) NOT NULL,
  topic_title nvarchar(120) DEFAULT ('') NOT NULL,
  user_id bigint DEFAULT (0) NOT NULL,
  topic_time datetime DEFAULT NULL,
  topic_views bigint DEFAULT (0) NULL,
  topic_replies bigint DEFAULT (0) NULL,
  topic_status tinyint DEFAULT (0) NULL,
  topic_vote_id bigint DEFAULT (0) NULL,
  topic_type tinyint DEFAULT (0) NULL,
  topic_first_post_id bigint DEFAULT (0) NULL,
  topic_last_post_id bigint DEFAULT (0) NOT NULL,
  topic_moved_id bigint DEFAULT (0),
  moderated tinyint DEFAULT (0) NULL 
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
  topic_id bigint DEFAULT (0) NOT NULL,
  user_id bigint DEFAULT (0) NOT NULL,
  is_read tinyint DEFAULT (1) NOT NULL 
);
CREATE INDEX idx_tw_topic ON jforum_topics_watch(topic_id);
CREATE INDEX idx_tw_user ON jforum_topics_watch(user_id);

--
-- Table structure for table 'jforum_users'
--
CREATE TABLE jforum_users (
  user_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  user_active tinyint DEFAULT NULL,
  username nvarchar(50) DEFAULT ('') NOT NULL,
  user_password varchar(128) DEFAULT ('') NOT NULL,
  user_session_time bigint DEFAULT (0) NULL,
  user_session_page int DEFAULT (0) NOT NULL,
  user_lastvisit datetime DEFAULT NULL,
  user_regdate datetime DEFAULT NULL,
  user_level tinyint DEFAULT NULL,
  user_posts bigint DEFAULT (0) NOT NULL,
  user_timezone varchar(5) DEFAULT ('') NOT NULL,
  user_style tinyint DEFAULT NULL,
  user_lang varchar(255) DEFAULT ('') NOT NULL,
  user_dateformat varchar(20) DEFAULT ('%d/%M/%Y %H:%i') NOT NULL,
  user_new_privmsg int DEFAULT (0) NOT NULL,
  user_unread_privmsg int DEFAULT (0) NOT NULL,
  user_last_privmsg datetime NULL,
  user_emailtime datetime DEFAULT NULL,
  user_viewemail tinyint DEFAULT (0) NULL,
  user_attachsig tinyint DEFAULT (1) NULL,
  user_allowhtml tinyint DEFAULT (0) NULL,
  user_allowbbcode tinyint DEFAULT (1) NULL,
  user_allowsmilies tinyint DEFAULT (1) NULL,
  user_allowavatar tinyint DEFAULT (1) NULL,
  user_allow_pm tinyint DEFAULT (1) NULL,
  user_allow_viewonline tinyint DEFAULT (1) NULL,
  user_notify tinyint DEFAULT (1) NULL,
  user_notify_always tinyint DEFAULT (0) NULL,
  user_notify_text tinyint DEFAULT (0) NULL,
  user_notify_pm tinyint DEFAULT (1) NULL,
  user_popup_pm tinyint DEFAULT (1) NULL,
  rank_id int DEFAULT (0) NULL,
  user_avatar VARCHAR(255) DEFAULT NULL,
  user_avatar_type tinyint DEFAULT (0) NOT NULL,
  user_email varchar(255) DEFAULT ('') NOT NULL,
  user_icq varchar(15) DEFAULT NULL,
  user_website varchar(255) DEFAULT NULL,
  user_from nvarchar(100) DEFAULT NULL,
  user_sig ntext NULL,
  user_sig_bbcode_uid varchar(10) DEFAULT NULL,
  user_aim varchar(255) DEFAULT NULL,
  user_yim varchar(255) DEFAULT NULL,
  user_msnm varchar(255) DEFAULT NULL,
  user_occ nvarchar(100) DEFAULT NULL,
  user_interests nvarchar(255) DEFAULT NULL,
  user_biography ntext DEFAULT NULL,
  user_actkey varchar(32) DEFAULT NULL,
  gender char(1) DEFAULT NULL,
  themes_id bigint DEFAULT NULL,
  deleted tinyint NULL,
  user_viewonline tinyint DEFAULT (1) NULL,
  security_hash varchar(32) NULL,
  user_karma decimal(10,2),
  user_authhash varchar(32) NULL,
  user_twitter varchar(50) DEFAULT NULL
);

--
-- Table structure for table 'jforum_vote_desc'
--
CREATE TABLE jforum_vote_desc (
  vote_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  topic_id bigint DEFAULT (0) NOT NULL,
  vote_text ntext DEFAULT ('') NOT NULL,
  vote_start datetime NOT NULL,
  vote_length bigint DEFAULT (0) NOT NULL 
);
CREATE INDEX idx_vd_topic ON jforum_vote_desc(topic_id);

--
-- Table structure for table 'jforum_vote_results'
--
CREATE TABLE jforum_vote_results (
  vote_id bigint DEFAULT (0) NOT NULL,
  vote_option_id tinyint DEFAULT (0) NOT NULL,
  vote_option_text nvarchar(255) DEFAULT ('') NOT NULL,
  vote_result bigint DEFAULT (0) NOT NULL 
);
CREATE INDEX idx_vr_id ON jforum_vote_results(vote_id);

--
-- Table structure for table 'jforum_vote_voters'
--
CREATE TABLE jforum_vote_voters (
  vote_id bigint DEFAULT (0) NOT NULL,
  vote_user_id bigint DEFAULT (0) NOT NULL,
  vote_user_ip varchar(15) DEFAULT ('') NOT NULL 
);
CREATE INDEX idx_vv_id ON jforum_vote_voters(vote_id);
CREATE INDEX idx_vv_user ON jforum_vote_voters(vote_user_id);

--
-- Table structure for table 'jforum_words'
--
CREATE TABLE jforum_words (
  word_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  word nvarchar(100) DEFAULT ('') NOT NULL,
  replacement nvarchar(100) DEFAULT ('') NOT NULL 
);

--
-- Table structure for table 'jforum_karma'
--
CREATE TABLE jforum_karma (
  karma_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  post_id int NOT NULL,
  topic_id int NOT NULL,
  post_user_id int NOT NULL,
  from_user_id int NOT NULL,
  points int NOT NULL,
  rate_date datetime NULL
);
CREATE INDEX idx_krm_post ON jforum_karma(post_id);
CREATE INDEX idx_krm_topic ON jforum_karma(topic_id);
CREATE INDEX idx_krm_user ON jforum_karma(post_user_id);
CREATE INDEX idx_krm_from ON jforum_karma(from_user_id);

--
-- Table structure for table 'jforum_bookmark'
--
CREATE TABLE jforum_bookmarks (
  bookmark_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  user_id int NOT NULL,
  relation_id int NOT NULL,
  relation_type int NOT NULL,
  public_visible int DEFAULT (1),
  title nvarchar(255),
  description nvarchar(255)
);
CREATE INDEX idx_bok_user ON jforum_bookmarks(user_id);
CREATE INDEX idx_bok_rel ON jforum_bookmarks(relation_id);

-- 
-- Table structure for table 'jforum_quota_limit'
--
CREATE TABLE jforum_quota_limit (
  quota_limit_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  quota_desc nvarchar(50) NOT NULL,
  quota_limit int NOT NULL,
  quota_type tinyint DEFAULT (1)
);

--
-- Table structure for table 'jforum_extension_groups'
--
CREATE TABLE jforum_extension_groups (
  extension_group_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  name nvarchar(100) NOT NULL,
  allow tinyint DEFAULT (1),
  upload_icon varchar(100),
  download_mode tinyint DEFAULT (1)
);

-- 
-- Table structure for table 'jforum_extensions'
--
CREATE TABLE jforum_extensions (
  extension_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  extension_group_id int NOT NULL,
  description nvarchar(100),
  upload_icon varchar(100),
  extension varchar(10),
  allow tinyint DEFAULT (1)
);
CREATE INDEX idx_ext_group ON jforum_extensions(extension_group_id);
CREATE INDEX idx_ext_ext ON jforum_extensions(extension);

--
-- Table structure for table 'jforum_attach'
--
CREATE TABLE jforum_attach (
  attach_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  post_id int,
  privmsgs_id int,
  user_id int NOT NULL
);
CREATE INDEX idx_att_post ON jforum_attach(post_id);
CREATE INDEX idx_att_priv ON jforum_attach(privmsgs_id);
CREATE INDEX idx_att_user ON jforum_attach(user_id);

-- 
-- Table structure for table 'jforum_attach_desc'
--
CREATE TABLE jforum_attach_desc (
  attach_desc_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  attach_id int NOT NULL,
  physical_filename varchar(255) NOT NULL,
  real_filename nvarchar(255) NOT NULL,
  download_count int,
  description nvarchar(255),
  mimetype varchar(85),
  filesize int,
  upload_time datetime,
  thumb tinyint DEFAULT (0),
  extension_id int
);
CREATE INDEX idx_att_d_att ON jforum_attach_desc(attach_id);
CREATE INDEX idx_att_d_ext ON jforum_attach_desc(extension_id);

--
-- Table structure for table 'jforum_attach_quota'
--
CREATE TABLE jforum_attach_quota (
  attach_quota_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  group_id int NOT NULL,
  quota_limit_id int NOT NULL
);
CREATE INDEX idx_aq_group ON jforum_attach_quota(group_id);
CREATE INDEX idx_aq_ql ON jforum_attach_quota(quota_limit_id);

--
-- Table structure for table 'jforum_banner'
--
CREATE TABLE jforum_banner (
  banner_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  banner_name nvarchar(90),
  banner_placement int DEFAULT (0) NOT NULL,
  banner_description nvarchar(250),
  banner_clicks int DEFAULT (0) NOT NULL,
  banner_views int DEFAULT (0) NOT NULL,
  banner_url varchar(250),
  banner_weight tinyint DEFAULT (50) NOT NULL,
  banner_active tinyint DEFAULT (0) NOT NULL,
  banner_comment nvarchar(250),
  banner_type int DEFAULT (0) NOT NULL,
  banner_width int DEFAULT (0) NOT NULL,
  banner_height int DEFAULT (0) NOT NULL
);

--
-- Table structure for table 'jforum_moderation_log'
-- 
CREATE TABLE jforum_moderation_log (
  log_id bigint IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  user_id bigint NOT NULL,
  log_description ntext NOT NULL,
  log_original_message ntext,
  log_date datetime NOT NULL,
  log_type tinyint DEFAULT (0),
  post_id bigint DEFAULT (0),
  topic_id bigint DEFAULT (0),
  post_user_id bigint DEFAULT (0)
);
CREATE INDEX idx_ml_user ON jforum_moderation_log(user_id);
CREATE INDEX idx_ml_post_user ON jforum_moderation_log(post_user_id);

--
-- Table structure for table 'jforum_mail_integration'
--
CREATE TABLE jforum_mail_integration (
  forum_id int NOT NULL,
  forum_email varchar(100) NOT NULL,
  pop_username varchar(100) NOT NULL,
  pop_password varchar(100) NOT NULL,
  pop_host varchar(100) NOT NULL,
  pop_port int DEFAULT (110),
  pop_ssl tinyint DEFAULT (0)
);
CREATE INDEX idx_mi_forum ON jforum_mail_integration(forum_id);

--
-- Table structure for table 'jforum_api'
--
CREATE TABLE jforum_api (
  api_id int IDENTITY (1, 1) PRIMARY KEY NOT NULL,
  api_key varchar(32) NOT NULL,
  api_validity datetime NOT NULL
);

--
-- Table structure for table 'jforum_spam'
--
CREATE TABLE jforum_spam (
  pattern nvarchar(100) NOT NULL
);
