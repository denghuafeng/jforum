# #############
# GenericModel
# #############
GenericModel.selectByLimit = SELECT TOP

# #############
# CategoryModel
# #############
CategoryModel.lastGeneratedCategoryId = SELECT IDENT_CURRENT('jforum_categories') AS categories_id 

# #############
# UserModel
# #############
UserModel.selectById = SELECT u.*, \
    (SELECT COUNT(1) FROM jforum_privmsgs pm \
    WHERE pm.privmsgs_to_userid = u.user_id \
    AND pm.privmsgs_type = 1) AS private_messages \
    FROM jforum_users u \
    WHERE u.user_id = ?
								
UserModel.lastUserRegistered = SELECT TOP 1 user_id, username FROM jforum_users ORDER BY user_regdate DESC
UserModel.lastGeneratedUserId = SELECT IDENT_CURRENT('jforum_users') AS user_id
UserModel.selectAllByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY user_id ASC) - 1 AS rownumber, \
	user_email, user_id, user_posts, user_regdate, username, deleted, user_karma, user_from, user_website, user_viewemail \
	FROM jforum_users ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?

UserModel.selectAllByGroup = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY u.user_id ASC) - 1 AS rownumber, \
	user_email, u.user_id, user_posts, user_regdate, username, deleted, user_karma, user_from, \
	user_website, user_viewemail \
	FROM jforum_users u, jforum_user_groups ug \
	WHERE u.user_id = ug.user_id \
	AND ug.group_id = ? ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?

UserModel.findByEmail = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY user_id ASC) - 1 AS rownumber, \
  * FROM jforum_users WHERE LOWER(user_email) = LOWER(?) ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?
 
UserModel.findByIp = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY user_id ASC) - 1 AS rownumber, \
  DISTINCT u.* \
  FROM jforum_users u LEFT JOIN jforum_posts p ON (u.user_id = p.user_id) \
  WHERE p.poster_ip LIKE ?  ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?
  		
# #############
# PostModel
# #############
PostModel.selectLatestByForumForRSS = SELECT * FROM ( \
	SELECT ROW_NUMBER() OVER (ORDER BY t.topic_last_post_id DESC) - 1 AS rownumber, \
	p.topic_id, p.post_id, p.forum_id, pt.post_subject AS subject, \
	pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0 \
	AND t.forum_id = ? ) AS tmp \
	WHERE rownumber < ?

PostModel.selectLatestForRSS = SELECT * FROM ( \
	SELECT ROW_NUMBER() OVER (ORDER BY t.topic_last_post_id DESC) - 1 AS rownumber, \
	t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0 \
	) AS tmp \
	WHERE rownumber < ?
	
PostModel.selectHotForRSS = SELECT * FROM ( \
	SELECT ROW_NUMBER() OVER (ORDER BY t.topic_views DESC) - 1 AS rownumber, \
	t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0 \
	) AS tmp \
	WHERE rownumber < ? 

PostModel.lastGeneratedPostId = SELECT IDENT_CURRENT('jforum_posts') AS post_id

PostModel.selectAllByTopicByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY p.post_time ASC) - 1 AS rownumber, \
	p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND topic_id = ? \
	AND p.user_id = u.user_id \
	AND p.need_moderate = 0 ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?

PostModel.selectByUserByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY p.post_id DESC) - 1 AS rownumber, \
	p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND p.user_id = u.user_id \
	AND p.user_id = ? \
	AND p.need_moderate = 0 \
	AND forum_id IN(:fids:) ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?
	
# #############
# ForumModel
# #############
ForumModel.lastGeneratedForumId = SELECT IDENT_CURRENT('jforum_forums') AS forum_id

# ##########
# PollModel
# ##########
PollModel.lastGeneratedPollId = SELECT IDENT_CURRENT('jforum_vote_desc') AS vote_desc_id

# #############
# TopicModel
# #############
TopicModel.selectAllByForumByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY t.topic_type DESC, t.topic_last_post_id DESC) - 1 AS rownumber, \
	t.*, p.user_id AS last_user_id, p.post_time, p.attach AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE (t.forum_id = ? OR t.topic_moved_id = ?) \
	AND p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0 ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?
	
TopicModel.selectRecentTopicsByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY t.topic_last_post_id DESC) - 1 AS rownumber, \
	t.*, p.user_id AS last_user_id, p.post_time, p.attach AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0 ) AS tmp \
	WHERE rownumber < ?

TopicModel.selectHottestTopicsByLimit = SELECT * \
    FROM ( SELECT ROW_NUMBER() OVER (ORDER BY t.topic_views DESC) - 1 AS rownumber, \
    t.*, p.user_id AS last_user_id, p.post_time, p.attach AS attach \
    FROM jforum_topics t, jforum_posts p \
    WHERE p.post_id = t.topic_last_post_id \
    AND p.need_moderate = 0 ) AS tmp \
    WHERE rownumber < ?
    
TopicModel.selectByUserByLimit = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY t.topic_last_post_id DESC) - 1 AS rownumber, \
	t.*, p.user_id AS last_user_id, p.post_time, p.attach AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND t.user_id = ? \
	AND p.need_moderate = 0 \
	AND t.forum_id IN(:fids:) ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?
	
TopicModel.lastGeneratedTopicId = SELECT IDENT_CURRENT('jforum_topics') AS topic_id 

# #############
# PrivateMessagesModel
# #############
PrivateMessagesModel.lastGeneratedPmId = SELECT IDENT_CURRENT('jforum_privmsgs') AS privmsgs_id 

# #############
# SmiliesModel
# #############
SmiliesModel.lastGeneratedSmilieId = SELECT IDENT_CURRENT('jforum_smilies') AS smilie_id 

# #############
# PermissionControl
# #############
PermissionControl.lastGeneratedRoleId = SELECT IDENT_CURRENT('jforum_roles') AS role_id 

# #############
# KarmaModel
# #############
KarmaModel.getMostRatedUserByPeriod = u.user_id, u.username, SUM(points) AS total, \
	COUNT(post_user_id) AS votes_received, user_karma, \
	(SELECT COUNT(from_user_id) AS votes_given \
		FROM jforum_karma as k2 \
		WHERE k2.from_user_id = u.user_id) AS votes_given \
	FROM jforum_users u, jforum_karma k \
	WHERE u.user_id = k.post_user_id \
	AND k.rate_date BETWEEN ? AND ? \
	GROUP BY u.user_id, u.username, user_karma								

# ################
# AttachmentModel
# ################
AttachmentModel.lastGeneratedAttachmentId = SELECT IDENT_CURRENT('jforum_attach') AS attach_id

AttachmentModel.selectTopDownloadsByLimit = SELECT TOP (?) f.forum_id, f.forum_name, t.topic_id, t.topic_title, ad.attach_id, ad.real_filename, ad.filesize, ad.download_count \
    FROM jforum_forums f, jforum_posts p, jforum_topics t, jforum_attach a, jforum_attach_desc ad \
    WHERE p.topic_id = t.topic_id AND p.forum_id = f.forum_id and p.post_id = a.post_id \
    AND a.attach_id = ad.attach_id AND a.privmsgs_id = 0 AND ad.download_count > 0 \
    ORDER BY ad.download_count DESC
    
# ###############
# BanlistModel
# ###############
BanlistModel.lastGeneratedBanlistId = SELECT IDENT_CURRENT('jforum_banlist') AS banlist_id

# ################
# ModerationLog
# ################
ModerationLog.lastGeneratedModerationLogId = SELECT IDENT_CURRENT('jforum_moderation_log') AS log_id
ModerationLog.selectAll = SELECT * \
	FROM ( SELECT ROW_NUMBER() OVER (ORDER BY l.log_id DESC) - 1 AS rownumber, \
	l.*, u.username, u2.username AS poster_username \
	FROM jforum_moderation_log l \
	LEFT JOIN jforum_users u2 ON u2.user_id = l.post_user_id \
	LEFT JOIN jforum_users u ON l.user_id = u.user_id ) AS tmp \
	WHERE rownumber >= ? and rownumber < ?