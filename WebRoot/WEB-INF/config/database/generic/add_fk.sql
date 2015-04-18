ALTER TABLE jforum_forums ADD FOREIGN KEY (categories_id) REFERENCES jforum_categories(categories_id);

ALTER TABLE jforum_forums_watch ADD FOREIGN KEY (forum_id) REFERENCES jforum_forums(forum_id);
ALTER TABLE jforum_forums_watch ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_user_groups ADD FOREIGN KEY (group_id) REFERENCES jforum_groups(group_id);
ALTER TABLE jforum_user_groups ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_roles ADD FOREIGN KEY (group_id) REFERENCES jforum_groups(group_id);

ALTER TABLE jforum_role_values ADD FOREIGN KEY (role_id) REFERENCES jforum_roles(role_id);

ALTER TABLE jforum_topics ADD FOREIGN KEY (forum_id) REFERENCES jforum_forums(forum_id);
ALTER TABLE jforum_topics ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_posts ADD FOREIGN KEY (topic_id) REFERENCES jforum_topics(topic_id);
ALTER TABLE jforum_posts ADD FOREIGN KEY (forum_id) REFERENCES jforum_forums(forum_id);
ALTER TABLE jforum_posts ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_posts_text ADD FOREIGN KEY (post_id) REFERENCES jforum_posts(post_id) ON DELETE CASCADE;

ALTER TABLE jforum_privmsgs ADD FOREIGN KEY (privmsgs_from_userid) REFERENCES jforum_users(user_id);
ALTER TABLE jforum_privmsgs ADD FOREIGN KEY (privmsgs_to_userid) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_privmsgs_text ADD FOREIGN KEY (privmsgs_id) REFERENCES jforum_privmsgs(privmsgs_id) ON DELETE CASCADE;

ALTER TABLE jforum_sessions ADD FOREIGN KEY (session_user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_topics_watch ADD FOREIGN KEY (topic_id) REFERENCES jforum_topics(topic_id);
ALTER TABLE jforum_topics_watch ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_vote_desc ADD FOREIGN KEY (topic_id) REFERENCES jforum_topics(topic_id);

ALTER TABLE jforum_vote_results ADD FOREIGN KEY (vote_id) REFERENCES jforum_vote_desc(vote_id) ON DELETE CASCADE;

ALTER TABLE jforum_vote_voters ADD FOREIGN KEY (vote_id) REFERENCES jforum_vote_desc(vote_id) ON DELETE CASCADE;
ALTER TABLE jforum_vote_voters ADD FOREIGN KEY (vote_user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_karma ADD FOREIGN KEY (post_id) REFERENCES jforum_posts(post_id);
ALTER TABLE jforum_karma ADD FOREIGN KEY (topic_id) REFERENCES jforum_topics(topic_id);
ALTER TABLE jforum_karma ADD FOREIGN KEY (post_user_id) REFERENCES jforum_users(user_id);
ALTER TABLE jforum_karma ADD FOREIGN KEY (from_user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_bookmarks ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_extensions ADD FOREIGN KEY (extension_group_id) REFERENCES jforum_extension_groups(extension_group_id);

ALTER TABLE jforum_attach ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_attach_desc ADD FOREIGN KEY (attach_id) REFERENCES jforum_attach(attach_id);

ALTER TABLE jforum_attach_quota ADD FOREIGN KEY (quota_limit_id) REFERENCES jforum_quota_limit(quota_limit_id);

ALTER TABLE jforum_moderation_log ADD FOREIGN KEY (user_id) REFERENCES jforum_users(user_id);
ALTER TABLE jforum_moderation_log ADD FOREIGN KEY (post_user_id) REFERENCES jforum_users(user_id);

ALTER TABLE jforum_mail_integration ADD FOREIGN KEY (forum_id) REFERENCES jforum_forums(forum_id);