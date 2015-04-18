ALTER TABLE jforum_ranks ADD UNIQUE(rank_title);
ALTER TABLE jforum_users ADD UNIQUE(username);
ALTER TABLE jforum_categories ADD UNIQUE(title);
ALTER TABLE jforum_forums ADD CONSTRAINT u_catid_forum_name UNIQUE(categories_id, forum_name);
ALTER TABLE jforum_groups ADD UNIQUE(group_name);
ALTER TABLE jforum_smilies ADD UNIQUE(code);
ALTER TABLE jforum_themes ADD UNIQUE(template_name);
ALTER TABLE jforum_extension_groups ADD UNIQUE(name);
