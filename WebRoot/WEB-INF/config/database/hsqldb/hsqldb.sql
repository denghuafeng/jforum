# ##########
# UserModel
# ##########
UserModel.lastUserRegistered = SELECT TOP 1 user_id, username FROM jforum_users ORDER BY user_regdate DESC 

UserModel.selectAllByLimit = SELECT LIMIT ? ? user_email, user_id, user_posts, user_regdate, username, deleted, user_karma, \
	user_from, user_website, user_viewemail FROM jforum_users ORDER BY user_id ASC

UserModel.lastGeneratedUserId = SELECT MAX(user_id) FROM jforum_users

UserModel.selectAllByGroup = SELECT LIMIT ? ? user_email, u.user_id, user_posts, user_regdate, username, deleted, user_karma, \
	user_from, user_website, user_viewemail \
	FROM jforum_users u, jforum_user_groups ug \
	WHERE u.user_id = ug.user_id \
	AND ug.group_id = ? \
	ORDER BY username

UserModel.findByEmail = SELECT LIMIT ?, ? * FROM jforum_users WHERE LOWER(user_email) = LOWER(?)

UserModel.findByIp = SELECT LIMIT ?, ? DISTINCT users.* \
    FROM jforum_users users LEFT JOIN jforum_posts posts ON (users.user_id = posts.user_id) \
    WHERE posts.poster_ip LIKE ?
    	
UserModel.selectById = SELECT u.*, \
	(SELECT COUNT(1) FROM jforum_privmsgs pm \
	WHERE pm.privmsgs_to_userid = u.user_id \
	AND pm.privmsgs_type = 1) AS private_messages \
	FROM jforum_users u \
	WHERE u.user_id = ?

UserModel.isUsernameRegistered = SELECT COUNT(1) as registered FROM jforum_users WHERE LCASE(username) = LCASE(?)
UserModel.login = SELECT user_id FROM jforum_users WHERE LCASE(username) = LCASE(?) AND user_password = ?

# #############
# PostModel
# #############
PostModel.lastGeneratedPostId = SELECT MAX(post_id) FROM jforum_posts
	
PostModel.selectLatestByForumForRSS = SELECT LIMIT 0 ? p.topic_id, p.topic_id, p.post_id, p.forum_id, pt.post_subject AS subject, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0 \
	AND t.forum_id = ? \
	ORDER BY t.topic_last_post_id DESC

PostModel.selectLatestForRSS = SELECT LIMIT 0 ? t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0  \
	ORDER BY topic_last_post_id DESC
	
PostModel.selectHotForRSS = SELECT LIMIT 0 ? t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0  \
	ORDER BY topic_views DESC

PostModel.selectAllByTopicByLimit = SELECT LIMIT ? ? p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND topic_id = ? \
	AND p.user_id = u.user_id \
	AND p.need_moderate = 0 \
	ORDER BY post_time ASC 

PostModel.selectByUserByLimit = SELECT LIMIT ? ? p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND p.user_id = u.user_id \
	AND p.user_id = ? \
	AND p.need_moderate = 0 \
	AND forum_id IN(:fids:) \
	ORDER BY p.post_id DESC

# ##########
# PollModel
# ##########
PollModel.lastGeneratedPollId = SELECT MAX(vote_id) FROM jforum_vote_desc

# #############
# ForumModel
# #############
ForumModel.lastGeneratedForumId = SELECT MAX(forum_id) FROM jforum_forums

# #############
# TopicModel
# #############
TopicModel.selectAllByForumByLimit = SELECT LIMIT ? ? t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE (t.forum_id = ? OR t.topic_moved_id = ?) \
	AND p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0 \
	ORDER BY t.topic_type DESC, t.topic_last_post_id DESC

TopicModel.selectRecentTopicsByLimit = SELECT LIMIT 0 ? t.*, p.user_id AS last_user_id, p.post_time, p.attach AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0 \
	ORDER BY t.topic_last_post_id DESC
	
TopicModel.lastGeneratedTopicId = SELECT MAX(topic_id) FROM jforum_topics

TopicModel.selectByUserByLimit = SELECT LIMIT ? ? t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND t.user_id = ? \
	AND p.need_moderate = 0 \
	AND t.forum_id IN(:fids:) \
	ORDER BY t.topic_last_post_id DESC

# #####################
# PrivateMessagesModel
# #####################
PrivateMessagesModel.lastGeneratedPmId = SELECT MAX(privmsgs_id) FROM jforum_privmsgs

# #############
# SmiliesModel
# #############
SmiliesModel.lastGeneratedSmilieId = SELECT MAX(smilie_id) FROM jforum_smilies

# ##################
# PermissionControl
# ##################
PermissionControl.lastGeneratedRoleId = SELECT MAX(role_id) FROM jforum_roles

# ##############
# CategoryModel
# ##############
CategoryModel.lastGeneratedCategoryId = SELECT MAX(categories_id) FROM jforum_categories

# ################
# AttachmentModel
# ################
AttachmentModel.lastGeneratedAttachmentId = SELECT MAX(attach_id) FROM jforum_attach

# ###############
# BanlistModel
# ###############
BanlistModel.lastGeneratedBanlistId = SELECT MAX(banlist_id) FROM jforum_banlist

# ################
# ModerationLog
# ################
ModerationLog.lastGeneratedModerationLogId = SELECT MAX(log_id) FROM jforum_moderation_log

ModerationLog.selectAll = SELECT LIMIT ? ? l.*, u.username, u2.username AS poster_username \
	FROM jforum_moderation_log l \
	LEFT JOIN jforum_users u2 ON u2.user_id = l.post_user_id \
	LEFT JOIN jforum_users u ON l.user_id = u.user_id \
	ORDER BY log_id DESC 