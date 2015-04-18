# ####################################
# @author ??? (original coding)
# @author Dirk Rasmussen - d.rasmussen@bevis.de (modifies for MS SQL Server 2005)
# @author Andowson Chang - http://www.andowson.com (fix for MS SQL Server 2000)
# @version $Id$
# ####################################

# #############
# UserModel
# #############						
UserModel.selectAllByLimit = SELECT TOP %d \
	user_email, user_id, user_posts, user_regdate, username, deleted, user_karma, user_from, user_website, user_viewemail \
	FROM jforum_users \
	ORDER BY user_id ASC

UserModel.selectAllByGroup = SELECT TOP %d \	
	user_email, u.user_id, user_posts, user_regdate, username, deleted, user_karma, user_from, \
	user_website, user_viewemail \
	FROM jforum_users u, jforum_user_groups ug \
	WHERE u.user_id = ug.user_id \
	AND ug.group_id = ?
	
# #############
# PostModel
# #############
PostModel.selectLatestByForumForRSS = SELECT TOP %d \
    p.topic_id, p.topic_id, p.post_id, p.forum_id, pt.post_subject AS subject, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0 \
	AND t.forum_id = ? \
	ORDER BY t.topic_last_post_id DESC

PostModel.selectLatestForRSS = SELECT TOP %d \
    t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0  \
	ORDER BY topic_last_post_id DESC
	
PostModel.selectHotForRSS = SELECT TOP %d \
    t.topic_id, t.topic_title AS subject, p.post_id, t.forum_id, pt.post_text, p.post_time, p.user_id, u.username \
	FROM jforum_topics t, jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.topic_id = t.topic_id \
	AND p.user_id = u.user_id \
	AND p.post_id = pt.post_id \
	AND p.need_moderate = 0  \
	ORDER BY topic_views DESC

PostModel.selectAllByTopicByLimit = SELECT TOP %d \
	p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND topic_id = ? \
	AND p.user_id = u.user_id \
	AND p.need_moderate = 0 
	ORDER BY post_time ASC

PostModel.selectByUserByLimit = SELECT TOP %d \
    p.post_id, topic_id, forum_id, p.user_id, post_time, poster_ip, enable_bbcode, p.attach, \
	enable_html, enable_smilies, enable_sig, post_edit_time, post_edit_count, status, pt.post_subject, pt.post_text, username, p.need_moderate \
	FROM jforum_posts p, jforum_posts_text pt, jforum_users u \
	WHERE p.post_id = pt.post_id \
	AND p.user_id = u.user_id \
	AND p.user_id = ? \
	AND p.need_moderate = 0 \
	AND forum_id IN(:fids:) \
	ORDER BY p.post_id DESC

# #############
# TopicModel
# #############
TopicModel.selectAllByForumByLimit =  SELECT TOP %d \
    t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE (t.forum_id = ? OR t.topic_moved_id = ?) \
	AND p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0 \
	ORDER BY t.topic_type DESC, t.topic_last_post_id DESC

TopicModel.selectRecentTopicsByLimit = SELECT TOP %d \
	t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND p.need_moderate = 0  \
	ORDER BY t.topic_last_post_id DESC

TopicModel.selectHottestTopicsByLimit = SELECT TOP %d \
  t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
  FROM jforum_topics t, jforum_posts p \
  WHERE p.post_id = t.topic_last_post_id \
  AND p.need_moderate = 0 \
  ORDER BY topic_views DESC

TopicModel.selectByUserByLimit = SELECT TOP %d \
    t.*, p.user_id AS last_user_id, p.post_time, (SELECT SUM(p.attach) \
        FROM jforum_posts p \
        WHERE p.topic_id = t.topic_id \
        AND p.need_moderate = 0) AS attach \
	FROM jforum_topics t, jforum_posts p \
	WHERE p.post_id = t.topic_last_post_id \
	AND t.user_id = ? \
	AND p.need_moderate = 0 \
	AND t.forum_id IN(:fids:) \
	ORDER BY t.topic_last_post_id DESC

# ############
# SearchModel
# ############
SearchModel.firstPostIdByDate = SELECT TOP 1 post_id FROM jforum_posts WHERE post_time > ?
SearchModel.lastPostIdByDate = SELECT TOP 1 post_id FROM jforum_posts WHERE post_time < ? ORDER BY post_id DESC

# ################
# AttachmentModel
# ################
AttachmentModel.selectTopDownloadsByLimit = SELECT TOP (?) f.forum_id, f.forum_name, t.topic_id, t.topic_title, ad.attach_id, ad.real_filename, ad.filesize, ad.download_count \
    FROM jforum_forums f, jforum_posts p, jforum_topics t, jforum_attach a, jforum_attach_desc ad \
    WHERE p.topic_id = t.topic_id AND p.forum_id = f.forum_id and p.post_id = a.post_id \
    AND a.attach_id = ad.attach_id AND a.privmsgs_id = 0 AND ad.download_count > 0 \
    ORDER BY ad.download_count DESC 
    
# ################
# ModerationLog
# ################
ModerationLog.selectAll = SELECT TOP %d \
    l.*, u.username, u2.username AS poster_username FROM jforum_moderation_log l \
	LEFT JOIN jforum_users u2 ON u2.user_id = l.post_user_id \
	LEFT JOIN jforum_users u ON l.user_id = u.user_id \
	ORDER BY log_id DESC
