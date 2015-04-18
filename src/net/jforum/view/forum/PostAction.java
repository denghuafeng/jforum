/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on May 3, 2003 / 5:05:18 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.KarmaDAO;
import net.jforum.dao.PollDAO;
import net.jforum.dao.PostDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Attachment;
import net.jforum.entities.Forum;
import net.jforum.entities.KarmaStatus;
import net.jforum.entities.ModerationLog;
import net.jforum.entities.Poll;
import net.jforum.entities.PollChanges;
import net.jforum.entities.Post;
import net.jforum.entities.QuotaLimit;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.AttachmentException;
import net.jforum.exceptions.ForumException;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.PostRepository;
import net.jforum.repository.RankingRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.SmiliesRepository;
import net.jforum.repository.SpamRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.AttachmentCommon;
import net.jforum.view.forum.common.ForumCommon;
import net.jforum.view.forum.common.PollCommon;
import net.jforum.view.forum.common.PostCommon;
import net.jforum.view.forum.common.Stats;
import net.jforum.view.forum.common.TopicsCommon;
import net.jforum.view.forum.common.ViewCommon;

import org.apache.commons.lang3.StringUtils;

import alex.zhrenjie04.wordfilter.WordFilterUtil;
import alex.zhrenjie04.wordfilter.result.FilteredResult;
import freemarker.template.SimpleHash;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class PostAction extends Command 
{
	public PostAction() {
	}

	public PostAction(RequestContext request, SimpleHash templateContext) {
		super.context = templateContext;
		super.request = request;
	}

	public void list()
	{
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		PollDAO pollDao = DataAccessDriver.getInstance().newPollDAO();
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();

		UserSession us = SessionFacade.getUserSession();
		int anonymousUser = SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID);
		boolean logged = SessionFacade.isLogged();

		int topicId = this.request.getIntParameter("topic_id");

		Topic topic = TopicRepository.getTopic(new Topic(topicId));

		if (topic == null) {
			topic = topicDao.selectById(topicId);
		}

		// The topic exists?
		if (topic.getId() == 0) {
			this.topicNotFound();
			return;
		}

		// Shall we proceed?
		Forum forum = ForumRepository.getForum(topic.getForumId());

		if (!logged) {
			if (forum == null || !ForumRepository.isCategoryAccessible(forum.getCategoryId())) {
				this.setTemplateName(ViewCommon.contextToLogin());
				return;
			}
		}
		else if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
			return;
		}

		int count = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		int start = ViewCommon.getStartPage();

		PermissionControl pc = SecurityRepository.get(us.getUserId());

		boolean moderatorCanEdit = pc.canAccess(SecurityConstants.PERM_MODERATION_POST_EDIT);

		List<Post> helperList = PostCommon.topicPosts(postDao, moderatorCanEdit, us.getUserId(), topic.getId(), start, count);

		// Ugly assumption:
		// Is moderation pending for the topic?
		if (topic.isModerated() && helperList.isEmpty()) {
			this.notModeratedYet();
			return;
		}

		// Set the topic status as read
		if (logged) {
			topicDao.updateReadStatus(topic.getId(), us.getUserId(), true);
		}

		boolean canVoteOnPoll = logged && SecurityRepository.canAccess(SecurityConstants.PERM_VOTE);
		Poll poll = null;

		if (topic.isVote()) {
			// It has a poll associated with the topic
			poll = pollDao.selectById(topic.getVoteId());

			if (canVoteOnPoll) {
				canVoteOnPoll = !pollDao.hasUserVotedOnPoll(topic.getVoteId(), us.getUserId());
			}
		}

		topicDao.incrementTotalViews(topic.getId());
		topic.setTotalViews(topic.getTotalViews() + 1);

		if (us.getUserId() != anonymousUser) {
			SessionFacade.getTopicsReadTime().put(Integer.valueOf(topic.getId()),
				Long.valueOf(System.currentTimeMillis()));
		}

		boolean karmaEnabled = SecurityRepository.canAccess(SecurityConstants.PERM_KARMA_ENABLED);
		Map<Integer, Integer> userVotes = new HashMap<Integer, Integer>();

		if (logged && karmaEnabled) {
			userVotes = DataAccessDriver.getInstance().newKarmaDAO().getUserVotes(topic.getId(), us.getUserId());
		}

		this.setTemplateName(TemplateKeys.POSTS_LIST);
		this.context.put("attachmentsEnabled", pc.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(topic.getForumId())));
		this.context.put("canDownloadAttachments", pc.canAccess(SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD));
		this.context.put("thumbShowBox", SystemGlobals.getBoolValue(ConfigKeys.ATTACHMENTS_IMAGES_THUMB_BOX_SHOW));
		this.context.put("am", new AttachmentCommon(this.request, topic.getForumId()));
		this.context.put("karmaVotes", userVotes);
		this.context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
		this.context.put("socialEnabled", SystemGlobals.getBoolValue(ConfigKeys.SOCIAL_SHARING_ENABLED));
		this.context.put("canRemove", pc.canAccess(SecurityConstants.PERM_MODERATION_POST_REMOVE));
		this.context.put("moderatorCanEdit", moderatorCanEdit);
		this.context.put("editAfterReply", SystemGlobals.getBoolValue(ConfigKeys.POSTS_EDIT_AFTER_REPLY));
		this.context.put("allCategories", ForumCommon.getAllCategoriesAndForums(false));
		this.context.put("topic", topic);
		this.context.put("poll", poll);
		this.context.put("canVoteOnPoll", canVoteOnPoll);
		this.context.put("rank", new RankingRepository());
		this.context.put("posts", helperList);
		this.context.put("forum", forum);
		this.context.put("karmaMin", Integer.valueOf(SystemGlobals.getValue(ConfigKeys.KARMA_MIN_POINTS)));
		this.context.put("karmaMax", Integer.valueOf(SystemGlobals.getValue(ConfigKeys.KARMA_MAX_POINTS)));
		this.context.put("avatarAllowExternalUrl", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_ALLOW_EXTERNAL_URL));
		this.context.put("avatarPath", SystemGlobals.getValue(ConfigKeys.AVATAR_IMAGE_DIR));
		this.context.put("moderationLoggingEnabled", SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED));
		this.context.put("needCaptcha", SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS));

		this.context.put("showAvatar", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_SHOW));
		this.context.put("showKarma", SystemGlobals.getBoolValue(ConfigKeys.KARMA_SHOW));
		this.context.put("showIP", SystemGlobals.getBoolValue(ConfigKeys.IP_SHOW));
		this.context.put("showOnline", SystemGlobals.getBoolValue(ConfigKeys.ONLINE_SHOW));

		Map<Integer, User> topicPosters = topicDao.topicPosters(topic.getId());

		for (Iterator<User> iter = topicPosters.values().iterator(); iter.hasNext(); ) {
			ViewCommon.prepareUserSignature(iter.next());
		}

		this.context.put("users", topicPosters);
		this.context.put("anonymousPosts", pc.canAccess(SecurityConstants.PERM_ANONYMOUS_POST, Integer.toString(topic.getForumId())));
		this.context.put("watching", topicDao.isUserSubscribed(topicId, SessionFacade.getUserSession().getUserId()));
		this.context.put("pageTitle", topic.getTitle());
		this.context.put("isAdmin", pc.canAccess(SecurityConstants.PERM_ADMINISTRATION));
		this.context.put("readonly", !pc.canAccess(SecurityConstants.PERM_READ_ONLY_FORUMS,	Integer.toString(topic.getForumId())));
		this.context.put("replyOnly", !pc.canAccess(SecurityConstants.PERM_REPLY_ONLY, Integer.toString(topic.getForumId())));

		this.context.put("isModerator", us.isModerator(topic.getForumId()));

		ViewCommon.contextToPagination(start, topic.getTotalReplies() + 1, count);

		TopicsCommon.topicListingBase();
		TopicRepository.updateTopic(topic);

        Stats.record("View thread", request.getRequestURL());
	}

	/**
	 * Given a postId, sends the user to the right page
	 */
	public void preList()
	{
		int postId = this.request.getIntParameter("post_id");

		PostDAO dao = DataAccessDriver.getInstance().newPostDAO();

		int count = dao.countPreviousPosts(postId);
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

		int topicId = 0;

		if (this.request.getParameter("topic_id") != null) {
			topicId = this.request.getIntParameter("topic_id");
		}

		if (topicId == 0) {
			Post post = dao.selectById(postId);
			topicId = post.getTopicId();
		}

		String page = "";

		if (count > postsPerPage) {
			page = Integer.toString(postsPerPage * ((count - 1) / postsPerPage)) + "/";
		} 

		JForumExecutionContext.setRedirect(this.request.getContextPath() + "/posts/list/"
			+ page + topicId
			+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) 
			+ "#p" + postId);
	}

	/**
	 * Votes on a poll.
	 */
	public void vote()
	{
		int pollId = this.request.getIntParameter("poll_id");
		int topicId = this.request.getIntParameter("topic_id");

		if (SessionFacade.isLogged() && this.request.getParameter("poll_option") != null) {
			Topic topic = TopicRepository.getTopic(new Topic(topicId));

			if (topic == null) {
				topic = DataAccessDriver.getInstance().newTopicDAO().selectRaw(topicId);
			}

			if (topic.getStatus() == Topic.STATUS_LOCKED) {
				this.topicLocked();
				return;
			}

			// They voted, save the value
			int optionId = this.request.getIntParameter("poll_option");

			PollDAO dao = DataAccessDriver.getInstance().newPollDAO();

			//vote on the poll
			UserSession user = SessionFacade.getUserSession();
			dao.voteOnPoll(pollId, optionId, user.getUserId(), request.getRemoteAddr());
		}

		JForumExecutionContext.setRedirect(this.request.getContextPath() 
			+ "/posts/list/"
			+ topicId
			+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
	}

	public void listByUser()
	{
		PostDAO pm = DataAccessDriver.getInstance().newPostDAO();
		UserDAO um = DataAccessDriver.getInstance().newUserDAO();
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();

		User user = um.selectById(this.request.getIntParameter("user_id"));

		if (user.getId() == 0) {
			this.context.put("message", I18n.getMessage("User.notFound"));
			this.setTemplateName(TemplateKeys.USER_NOT_FOUND);
			return;
		} 

		int count = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		int start = ViewCommon.getStartPage();
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

		List<Post> posts = pm.selectByUserByLimit(user.getId(), start, postsPerPage);
		int totalMessages = pm.countUserPosts(user.getId());

		// get list of forums
		Map<Integer, Topic> topics = new HashMap<Integer, Topic>();
		Map<Integer, Forum> forums = new HashMap<Integer, Forum>();

		for (Iterator<Post> iter = posts.iterator(); iter.hasNext(); ) {
			Post post = iter.next();

			if (!topics.containsKey(Integer.valueOf(post.getTopicId()))) {
				Topic topic = TopicRepository.getTopic(new Topic(post.getTopicId()));

				if (topic == null) {
					topic = tm.selectRaw(post.getTopicId());
				}

				this.context.put("attachmentsEnabled", SecurityRepository.canAccess(
						SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(topic.getForumId())));
				this.context.put("am", new AttachmentCommon(this.request, topic.getForumId()));

				topics.put(Integer.valueOf(topic.getId()), topic);
			}

			if (!forums.containsKey(Integer.valueOf(post.getForumId()))) {
				Forum forum = ForumRepository.getForum(post.getForumId());

				if (forum == null) {
					// OK, probably the user does not have permission to see this forum
					iter.remove();
					totalMessages--;
					continue;
				}

				forums.put(Integer.valueOf(forum.getId()), forum);
			}

			PostCommon.preparePostForDisplay(post);
		}

		this.setTemplateName(TemplateKeys.POSTS_USER_POSTS_LIST);

		this.context.put("canDownloadAttachments", SecurityRepository.canAccess(
				SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD));
		this.context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
		this.context.put("toggleMessageBody", SystemGlobals.getBoolValue(ConfigKeys.USER_POSTS_TOGGLE));
		this.context.put("allCategories", ForumCommon.getAllCategoriesAndForums(false));
		this.context.put("posts", posts);
		this.context.put("topics", topics);
		this.context.put("forums", forums);
		this.context.put("u", user);
		this.context.put("pageTitle", I18n.getMessage("PostShow.userPosts") + " " + user.getUsername());
		this.context.put("karmaMin", Integer.valueOf(SystemGlobals.getValue(ConfigKeys.KARMA_MIN_POINTS)));
		this.context.put("karmaMax", Integer.valueOf(SystemGlobals.getValue(ConfigKeys.KARMA_MAX_POINTS)));

		ViewCommon.contextToPagination(start, totalMessages, count);
	}

	public void review()
	{
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();

		int userId = SessionFacade.getUserSession().getUserId();
		int topicId = this.request.getIntParameter("topic_id");

		Topic topic = TopicRepository.getTopic(new Topic(topicId));

		if (topic == null) {
			topic = topicDao.selectById(topicId);
		}

		if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
			return;
		}

		int count = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		int start = ViewCommon.getStartPage();

		Map<Integer, User> usersMap = topicDao.topicPosters(topic.getId());
		List<Post> helperList = PostCommon.topicPosts(postDao, false, userId, topic.getId(), start, count);
		Collections.reverse(helperList);

		this.setTemplateName(SystemGlobals.getValue(ConfigKeys.TEMPLATE_DIR) + "/empty.htm");

		this.setTemplateName(TemplateKeys.POSTS_REVIEW);
		this.context.put("posts", helperList);
		this.context.put("users", usersMap);
	}

	private void topicNotFound() {
		this.setTemplateName(TemplateKeys.POSTS_TOPIC_NOT_FOUND);
		this.context.put("message", I18n.getMessage("PostShow.TopicNotFound"));
	}

	private void postNotFound() {
		this.setTemplateName(TemplateKeys.POSTS_POST_NOT_FOUND);
		this.context.put("message", I18n.getMessage("PostShow.PostNotFound"));
	}

	private void replyOnly()
	{
		this.setTemplateName(TemplateKeys.POSTS_REPLY_ONLY);
		this.context.put("message", I18n.getMessage("PostShow.replyOnly"));
	}

	private boolean isReplyOnly(int forumId)
	{
		return !SecurityRepository.canAccess(SecurityConstants.PERM_REPLY_ONLY, 
				Integer.toString(forumId));
	}

	public void reply()
	{
		this.insert();
	}

	public void insert()
	{
		int forumId;

		// If we have a topic_id, then it should be a reply
		if (this.request.getParameter("topic_id") != null) {
			int topicId = this.request.getIntParameter("topic_id");

			Topic topic = TopicRepository.getTopic(new Topic(topicId));

			if (topic == null) {
				topic = DataAccessDriver.getInstance().newTopicDAO().selectRaw(topicId);

				if (topic.getId() == 0) {
					throw new ForumException("Could not find a topic with id #" + topicId);
				}
			}

			forumId = topic.getForumId();

			if (topic.getStatus() == Topic.STATUS_LOCKED) {
				this.topicLocked();
				return;
			}

			this.context.put("topic", topic);
			this.context.put("setType", false);
			this.context.put("pageTitle", I18n.getMessage("PostForm.reply")+" "+topic.getTitle());
		}
		else {
			forumId = this.request.getIntParameter("forum_id");

			if (this.isReplyOnly(forumId)) {
				this.replyOnly();
				return;
			}
			this.context.put("setType", true);
			this.context.put("pageTitle", I18n.getMessage("PostForm.title"));
		}

		Forum forum = ForumRepository.getForum(forumId);

		if (forum == null) {
			throw new ForumException("Could not find a forum with id #" + forumId);
		}

		if (!TopicsCommon.isTopicAccessible(forumId)) {
			return;
		}

		if (!this.anonymousPost(forumId)
				|| this.isForumReadonly(forumId, this.request.getParameter("topic_id") != null)) {
			return;
		}

		int userId = SessionFacade.getUserSession().getUserId();

		this.setTemplateName(TemplateKeys.POSTS_INSERT);

		// Attachments
		boolean attachmentsEnabled = SecurityRepository.canAccess(
			SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(forumId));

		if (attachmentsEnabled && !SessionFacade.isLogged() 
			&& !SystemGlobals.getBoolValue(ConfigKeys.ATTACHMENTS_ANONYMOUS)) {
			attachmentsEnabled = false;
		}

		this.context.put("attachmentsEnabled", attachmentsEnabled);

		if (attachmentsEnabled) {
			QuotaLimit ql = new AttachmentCommon(this.request, forumId).getQuotaLimit(userId);
			this.context.put("maxAttachmentsSize", Long.valueOf(ql != null ? ql.getSizeInBytes() : 1));
			this.context.put("maxAttachments", SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		}

		boolean needCaptcha = SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS);

		this.context.put("moderationLoggingEnabled", SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED));
		this.context.put("smilies", SmiliesRepository.getSmilies());
		this.context.put("forum", forum);
		this.context.put("action", "insertSave");
		this.context.put("start", this.request.getParameter("start"));
		this.context.put("isNewPost", true);
		this.context.put("needCaptcha", needCaptcha);
		this.context.put("htmlAllowed",
			SecurityRepository.canAccess(SecurityConstants.PERM_HTML_DISABLED, Integer.toString(forumId)));
		this.context.put("canCreateStickyOrAnnouncementTopics",
			SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_STICKY_ANNOUNCEMENT_TOPICS));
		this.context.put("canCreatePolls",
			SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_POLL));

		User user = DataAccessDriver.getInstance().newUserDAO().selectById(userId);

		ViewCommon.prepareUserSignature(user);

		if (this.request.getParameter("preview") != null) {
			user.setNotifyOnMessagesEnabled(this.request.getParameter("notify") != null);
		}

		this.context.put("user", user);
	}

	public void edit()  {
		this.edit(false, null);
	}

	private void edit(boolean preview, Post origPost)
	{
		Post post = origPost;
		int userId = SessionFacade.getUserSession().getUserId();

		if (!preview) {
			PostDAO pm = DataAccessDriver.getInstance().newPostDAO();
			post = pm.selectById(this.request.getIntParameter("post_id"));

			// The post exist?
			if (post.getId() == 0) {
				this.postNotFound();
				return;
			}
		}

		boolean isModerator = SessionFacade.getUserSession().isModerator(post.getForumId());
		boolean canEdit = PostCommon.canEditPost(post);

		if (!canEdit) {
			this.setTemplateName(TemplateKeys.POSTS_EDIT_CANNOTEDIT);
			this.context.put("message", I18n.getMessage("CannotEditPost"));
		}
		else {
			Topic topic = TopicRepository.getTopic(new Topic(post.getTopicId()));

			if (topic == null) {
				topic = DataAccessDriver.getInstance().newTopicDAO().selectRaw(post.getTopicId());
			}

			if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
				return;
			}

			if (topic.getStatus() == Topic.STATUS_LOCKED && !isModerator) {
				this.topicLocked();
				return;
			}

			if (preview && this.request.getParameter("topic_type") != null) {
				topic.setType(this.request.getIntParameter("topic_type"));
			}

			if (post.hasAttachments()) {
				this.context.put("attachments", 
						DataAccessDriver.getInstance().newAttachmentDAO().selectAttachments(post.getId()));
			}

			Poll poll = null;

			if (topic.isVote() && topic.getFirstPostId() == post.getId()) {
				// It has a poll associated with the topic
				PollDAO poolDao = DataAccessDriver.getInstance().newPollDAO();
				poll = poolDao.selectById(topic.getVoteId());
			}

			this.setTemplateName(TemplateKeys.POSTS_EDIT);

			this.context.put("attachmentsEnabled", SecurityRepository.canAccess(
					SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(post.getForumId())));

			this.context.put("moderationLoggingEnabled", SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED));

			QuotaLimit ql = new AttachmentCommon(this.request, post.getForumId()).getQuotaLimit(userId);
			this.context.put("maxAttachmentsSize", Long.valueOf(ql != null ? ql.getSizeInBytes() : 1));
			this.context.put("isEdit", true);
			this.context.put("maxAttachments", SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
			this.context.put("smilies", SmiliesRepository.getSmilies());
			this.context.put("forum", ForumRepository.getForum(post.getForumId()));
			this.context.put("action", "editSave");
			this.context.put("post", post);
			this.context.put("setType", post.getId() == topic.getFirstPostId());
			this.context.put("topic", topic);
			this.context.put("poll", poll);
			this.context.put("pageTitle", I18n.getMessage("PostShow.messageTitle") + " " + post.getSubject());
			this.context.put("isModerator", isModerator);
			this.context.put("start", this.request.getParameter("start"));
			this.context.put("htmlAllowed", SecurityRepository.canAccess(SecurityConstants.PERM_HTML_DISABLED, 
					Integer.toString(topic.getForumId())));
			this.context.put("canCreateStickyOrAnnouncementTopics",
					SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_STICKY_ANNOUNCEMENT_TOPICS));
			this.context.put("canCreatePolls",
					SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_POLL));
		}

		UserDAO udao = DataAccessDriver.getInstance().newUserDAO();
		User user = udao.selectById(userId);
		ViewCommon.prepareUserSignature(user);

		if (preview) {
			user.setNotifyOnMessagesEnabled(this.request.getParameter("notify") != null);

			if (user.getId() != post.getUserId()) {
				// Probably a moderator is editing the message
				User previewUser = udao.selectById(post.getUserId());
				ViewCommon.prepareUserSignature(previewUser);
				this.context.put("previewUser", previewUser);
			}
		}

		this.context.put("user", user);
	}

	public void quote()
	{
		PostDAO pm = DataAccessDriver.getInstance().newPostDAO();
		Post post = pm.selectById(this.request.getIntParameter("post_id"));

		if (post.getId() == 0) {
			this.postNotFound();
			return;
		}

		if (post.isModerationNeeded()) {
			this.notModeratedYet();
			return;
		}

		if (!this.anonymousPost(post.getForumId())) {
			return;
		}

		Topic topic = TopicRepository.getTopic(new Topic(post.getTopicId()));

		if (topic == null) {
			topic = DataAccessDriver.getInstance().newTopicDAO().selectRaw(post.getTopicId());
		}

		if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
			return;
		}

		if (topic.getStatus() == Topic.STATUS_LOCKED) {
			this.topicLocked();
			return;
		}

		this.setTemplateName(TemplateKeys.POSTS_QUOTE);

		this.context.put("forum", ForumRepository.getForum(post.getForumId()));
		this.context.put("action", "insertSave");
		this.context.put("post", post);

		UserDAO um = DataAccessDriver.getInstance().newUserDAO();
		User user = um.selectById(post.getUserId());

		int userId = SessionFacade.getUserSession().getUserId();

		this.context.put("attachmentsEnabled", SecurityRepository.canAccess(
			SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(topic.getForumId())));

		QuotaLimit ql = new AttachmentCommon(this.request, topic.getForumId()).getQuotaLimit(userId);
		this.context.put("maxAttachmentsSize", Long.valueOf(ql != null ? ql.getSizeInBytes() : 1));

		this.context.put("moderationLoggingEnabled", SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED));
		this.context.put("maxAttachments", SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_MAX_POST));
		this.context.put("isNewPost", true);
		this.context.put("topic", topic);
		this.context.put("quote", "true");
		this.context.put("quoteUser", user.getUsername());
		this.context.put("setType", false);
		this.context.put("htmlAllowed", SecurityRepository.canAccess(SecurityConstants.PERM_HTML_DISABLED, 
			Integer.toString(topic.getForumId())));
		this.context.put("start", this.request.getParameter("start"));
		this.context.put("user", DataAccessDriver.getInstance().newUserDAO().selectById(userId));
		this.context.put("pageTitle", I18n.getMessage("PostForm.reply") + " " + topic.getTitle());
		this.context.put("smilies", SmiliesRepository.getSmilies());

		boolean needCaptcha = SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS);

		this.context.put("needCaptcha", needCaptcha);
	}

	/* check for spam in subject or body, except for admins */
	private String validatePost (Post post) {
		String str = post.getSubject();
		String spam = SpamRepository.findSpam(str);
		if (spam != null)
			return I18n.getMessage("PostForm.spam");

		str = post.getText();
		spam = SpamRepository.findSpam(str);
		if (spam != null)
			return I18n.getMessage("PostForm.spam");

		return null;
	}

	public void editSave()
	{
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		PollDAO pollDao = DataAccessDriver.getInstance().newPollDAO();
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();

		Post post = postDao.selectById(this.request.getIntParameter("post_id"));

		if (!PostCommon.canEditPost(post)) {
			this.cannotEdit();
			return;
		}

		boolean isModerator = SessionFacade.getUserSession().isModerator(post.getForumId());

		String originalMessage = post.getText();

		post = PostCommon.fillPostFromRequest(post, true);
		// check for subject or body containing spam
		String error = validatePost(post);
        if ((error != null) && ! isModerator) {
            this.context.put("post", post);
            this.context.put("errorMessage", error);
            this.edit(false, post);
            return;
        }

		// The user wants to preview the message before posting it?
		if ("1".equals(this.request.getParameter("preview"))) {
			this.context.put("preview", true);

			Post postPreview = new Post(post);
			this.context.put("postPreview", PostCommon.preparePostForDisplay(postPreview));

			this.edit(true, post);
		}
		else {
			AttachmentCommon attachments = new AttachmentCommon(this.request, post.getForumId());

			try {
				attachments.preProcess();
			}
			catch (AttachmentException e) {
				JForumExecutionContext.enableRollback();
				post.setText(this.request.getParameter("message"));
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", post);
				this.edit(false, post);
				return;
			}

			Topic topic = TopicRepository.getTopic(new Topic(post.getTopicId()));

			if (topic == null) {
				topic = topicDao.selectById(post.getTopicId());
			}

			if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
				return;
			}

			if (topic.getStatus() == Topic.STATUS_LOCKED
					&& !SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_POST_EDIT)) {
				this.topicLocked();
				return;
			}

			// If the corresponding setting is turned on, don't allow editing of posts after replies are available
			// ... unless the user is a moderator, of course
			if ((topic.getLastPostId() > post.getId())
				&& ! SystemGlobals.getBoolValue(ConfigKeys.POSTS_EDIT_AFTER_REPLY)
				&& ! SessionFacade.getUserSession().isModerator(post.getForumId()))
			{
				this.cannotEdit();
				return;
			}

			postDao.update(post);

			// Attachments
			attachments.editAttachments(post.getId(), post.getForumId());
			attachments.insertAttachments(post);
			post.hasAttachments(attachments.getAttachments(post.getId(), post.getForumId()).size() > 0);

			// The first message (the one which originated the topic) was changed
			if (topic.getFirstPostId() == post.getId()) {
				topic.setTitle(post.getSubject());

				int newType = this.request.getIntParameter("topic_type");
				boolean changeType = SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_STICKY_ANNOUNCEMENT_TOPICS)
					&& newType != topic.getType();

				if (changeType) {
					topic.setType(newType);
				}

				// Poll
				Poll poll = PollCommon.fillPollFromRequest();

				if (poll != null && !topic.isVote()) {
					// They added a poll
					poll.setTopicId(topic.getId());

					if (!this.ensurePollMinimumOptions(post, poll)) {
						return;
					}

					pollDao.addNew(poll);
					topic.setVoteId(poll.getId());

				} 
				else if (poll != null) {
					if (!this.ensurePollMinimumOptions(post, poll)) {
						return;
					}

					// They edited the poll in the topic
					Poll existing = pollDao.selectById(topic.getVoteId());
					PollChanges changes = new PollChanges(existing, poll);

					if (changes.hasChanges()) {
						poll.setId(existing.getId());
						poll.setChanges(changes);
						pollDao.update(poll);
					}

				} 
				else if (topic.isVote()) {
					// They deleted the poll from the topic
					pollDao.delete(topic.getVoteId());
					topic.setVoteId(0);
				}

				topicDao.update(topic);
				topic = topicDao.selectById(post.getTopicId());

				if (changeType) {
					TopicRepository.addTopic(topic);
				}
				else {
					TopicRepository.updateTopic(topic);
				}
			} else {
				topicDao.update(topic);
				topic = topicDao.selectById(post.getTopicId());
				TopicRepository.updateTopic(topic);
			}

			// Update forum last post info 
			ForumRepository.reloadForum(post.getForumId());

			if (SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED)
					&& isModerator && post.getUserId() != SessionFacade.getUserSession().getUserId()) {
				ModerationHelper helper = new ModerationHelper();
				this.request.addParameter("log_original_message", originalMessage);
				ModerationLog log = helper.buildModerationLogFromRequest();
				log.getPosterUser().setId(post.getUserId());
				helper.saveModerationLog(log);
			}

			if (this.request.getParameter("notify") == null) {
				topicDao.removeSubscription(post.getTopicId(), SessionFacade.getUserSession().getUserId());
			}

			String path = this.request.getContextPath() + "/posts/list/";
			int start = ViewCommon.getStartPage();

			if (start > 0) {
				path = new StringBuilder(path).append(start).append('/').toString();
			}

			path = new StringBuilder(path).append(post.getTopicId())
			    .append(SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION))
			    .append("#p")
			    .append(post.getId())
			    .toString();
			JForumExecutionContext.setRedirect(path);

			if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
				PostRepository.update(post.getTopicId(), PostCommon.preparePostForDisplay(post));
			}
		}
	}

	private boolean ensurePollMinimumOptions(Post post, Poll poll)
	{
		if (poll.getOptions().size() < 2) {
			// It is not a valid poll, cancel the post
			JForumExecutionContext.enableRollback();
			post.setText(this.request.getParameter("message"));
			post.setId(0);
			this.context.put("errorMessage", I18n.getMessage("PostForm.needMorePollOptions"));
			this.context.put("post", post);
			this.context.put("poll", poll);
			this.edit();
			return false;
		}

		return true;
	}

	public void waitingModeration()
	{
		this.setTemplateName(TemplateKeys.POSTS_WAITING);

		int topicId = this.request.getIntParameter("topic_id");
		String path = this.request.getContextPath();

		if (topicId == 0) {
			path = new StringBuilder(path).append("/forums/show/").append(this.request.getParameter("forum_id")).toString();
		}
		else {
			path = new StringBuilder(path).append("/posts/list/").append(topicId).toString();
		}
		if(this.request.getParameter("filtered")!=null){ 
			this.context.put("message",I18n.getMessage("PostShow.waitingModeration.filtered",//"document.getElementById('_174_239_Open_Image').style.display='inline'; document.getElementById('_174_239_Open_Text').style.display='inline'; />"+
					new String[] {path +SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));
		}else{
		this.context.put("message", I18n.getMessage("PostShow.waitingModeration", 
				new String[] { path + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) }));
		}
	}

	private void notModeratedYet()
	{
		this.setTemplateName(TemplateKeys.POSTS_NOT_MODERATED);
		this.context.put("message", I18n.getMessage("PostShow.notModeratedYet"));
	}

	public void insertSave()
	{
		int forumId = this.request.getIntParameter("forum_id");
		boolean firstPost = false;

		boolean newTopic = (this.request.getParameter("topic_id") == null);

		if (!this.anonymousPost(forumId) || !TopicsCommon.isTopicAccessible(forumId)
				|| this.isForumReadonly(forumId, newTopic)) {
			return;
		}

		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		PollDAO pollDao = DataAccessDriver.getInstance().newPollDAO();
		ForumDAO forumDao = DataAccessDriver.getInstance().newForumDAO();
		
		Topic topic = new Topic(-1);
		if (!newTopic) {
			int topicId = this.request.getIntParameter("topic_id");

			topic = TopicRepository.getTopic(new Topic(topicId));
			if (topic == null) {
				topic = topicDao.selectById(topicId);
			}

			// Could not find the topic. The topicId sent was invalid
			if (topic == null || topic.getId() == 0) {
				newTopic = true;
			}
			else {
				// Cannot insert new messages on locked topics
				if (topic.getStatus() == Topic.STATUS_LOCKED) {
					this.topicLocked();
					return;
				}
			}
		}

		// We don't use "else if" here because there is a possibility of the
		// checking above set the newTopic var to true
		if (newTopic) {
			if (this.isReplyOnly(forumId)) {
				this.replyOnly();
				return;
			}

			if (this.request.getParameter("topic_type") != null) {
				topic.setType(this.request.getIntParameter("topic_type"));

				if (topic.getType() != Topic.TYPE_NORMAL 
						&& !SecurityRepository.canAccess(SecurityConstants.PERM_CREATE_STICKY_ANNOUNCEMENT_TOPICS)) {
					topic.setType(Topic.TYPE_NORMAL);
				}
			}
		}

		UserSession us = SessionFacade.getUserSession();
		User user = DataAccessDriver.getInstance().newUserDAO().selectById(us.getUserId());

		if ("1".equals(this.request.getParameter("quick")) && SessionFacade.isLogged()) {
			this.request.addParameter("notify", user.isNotifyOnMessagesEnabled() ? "1" : null);
			this.request.addParameter("attach_sig", user.isAttachSignatureEnabled() ? "1" : "0");
		}

		// Set the Post
		Post post = PostCommon.fillPostFromRequest();

		if (post.getText() == null || post.getText().trim().equals("")) {
			this.insert();
			return;
		}

		boolean isModerator = SessionFacade.getUserSession().isModerator(post.getForumId());

		// check for subject or body containing spam
		String error = validatePost(post);
        if ((error != null) && ! isModerator) {
            this.context.put("post", post);
            this.context.put("errorMessage", error);
            this.insert();
            return;
        }

		// Check the elapsed time since the last post from the user
		int delay = SystemGlobals.getIntValue(ConfigKeys.POSTS_NEW_DELAY);

		if (delay > 0) {
			Long lastPostTime = (Long)SessionFacade.getAttribute(ConfigKeys.LAST_POST_TIME);

			if ((lastPostTime != null) && (System.currentTimeMillis() < (lastPostTime.longValue() + delay))) {
				this.context.put("post", post);
				this.context.put("start", this.request.getParameter("start"));
				this.context.put("error", I18n.getMessage("PostForm.tooSoon"));
				this.insert();
				return;
			}
		}

		topic.setForumId(forumId);
		post.setForumId(forumId);

		if (StringUtils.isBlank(post.getSubject())) {
			post.setSubject(topic.getTitle());
		}

		boolean needCaptcha = SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS)
			&& request.getSessionContext().getAttribute(ConfigKeys.REQUEST_IGNORE_CAPTCHA) == null;

		if (needCaptcha && !us.validateCaptchaResponse(this.request.getParameter("captcha_anwser"))) {
			this.context.put("post", post);
			this.context.put("start", this.request.getParameter("start"));
			this.context.put("error", I18n.getMessage("CaptchaResponseFails"));
			this.insert();
			return;
		}

		boolean preview = "1".equals(this.request.getParameter("preview"));

		if (!preview) {
			AttachmentCommon attachments = new AttachmentCommon(this.request, forumId);

			try {
				attachments.preProcess();
			}
			catch (AttachmentException e) {
				JForumExecutionContext.enableRollback();
				post.setText(this.request.getParameter("message"));
				post.setId(0);
				this.context.put("errorMessage", e.getMessage());
				this.context.put("post", post);
				this.insert();
				return;
			}

			Forum forum = ForumRepository.getForum(forumId);
			PermissionControl pc = SecurityRepository.get(us.getUserId());

			// Moderators and admins don't need to have their messages moderated
			boolean moderate = (forum.isModerated() 
				&& !pc.canAccess(SecurityConstants.PERM_MODERATION)
				&& !pc.canAccess(SecurityConstants.PERM_ADMINISTRATION));
			String title=this.request.getParameter("subject");
			String text=this.request.getParameter("message");
			//设置是否被过滤，true说明发布的内容包含了关键字，放到审核列表中
			boolean filtered = false;
			// 如果该组启用了过滤
			if (!moderate && pc.canAccess(SecurityConstants.PERM_POST_FILTER)) {
				FilteredResult fr = WordFilterUtil.filterText(title, '*');// 过滤标题
				filtered = fr.getLevel() > 0;
				// 过滤帖子内容
				if (!filtered) {
					fr = WordFilterUtil.filterText(text, '*');// 过滤标题
					filtered = fr.getLevel() > 0;
				}
				moderate = moderate || filtered; // 更改审核状态
			}
		   /*  p.setModerate(moderate);
		     int postId = postDao.addNew(p);  */  
			if (newTopic) {
				topic.setTime(new Date());
				topic.setTitle(title);
				topic.setModerated(moderate);
				topic.setPostedBy(user);
				topic.setFirstPostTime(topic.getTime());

				int topicId = topicDao.addNew(topic);
				topic.setId(topicId);
				firstPost = true;
			}

			if (!firstPost && pc.canAccess(
					SecurityConstants.PERM_REPLY_WITHOUT_MODERATION, Integer.toString(topic.getForumId()))) {
				moderate = false;
			}

			// Topic watch
			if (this.request.getParameter("notify") != null) {
				this.watch(topicDao, topic.getId(), user.getId());
			}

			post.setTopicId(topic.getId());

			// add a poll
			Poll poll = PollCommon.fillPollFromRequest();

			if (poll != null && newTopic) {
				poll.setTopicId(topic.getId());

				if (poll.getOptions().size() < 2) {
					//it is not a valid poll, cancel the post
					JForumExecutionContext.enableRollback();
					post.setText(text);
					post.setId(0);
					this.context.put("errorMessage", I18n.getMessage("PostForm.needMorePollOptions"));
					this.context.put("post", post);
					this.context.put("poll", poll);
					this.insert();
					return;
				}

				pollDao.addNew(poll);
				topic.setVoteId(poll.getId());
			}

			// Save the remaining stuff
			post.setModerate(moderate);
			post.setKarma(new KarmaStatus());
			int postId = postDao.addNew(post);

			if (newTopic) {
				topic.setFirstPostId(postId);
			}

			if (!moderate) {
				topic.setLastPostId(postId);
				topic.setLastPostBy(user);
				topic.setLastPostDate(post.getTime());
				topic.setLastPostTime(post.getTime());
			}

			topicDao.update(topic);

			attachments.insertAttachments(post);
			post.hasAttachments(attachments.getAttachments(post.getId(), forumId).size() > 0);
			topic.setHasAttach(topic.hasAttach()||post.hasAttachments());

			postDao.index(post);

			if (!moderate) {
				StringBuilder path = new StringBuilder(512);
				path.append(this.request.getContextPath()).append("/posts/list/");

				int start = ViewCommon.getStartPage();

				path.append(this.startPage(topic, start)).append('/')
					.append(topic.getId()).append(SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION))
					.append("#p").append(postId);

				JForumExecutionContext.setRedirect(path.toString());

				if (newTopic) {
					// Notify "forum new topic" users
					ForumCommon.notifyUsers(forum, topic, post);
				}
				else {
					topic.setTotalReplies(topic.getTotalReplies() + 1);
					TopicsCommon.notifyUsers(topic, post);
				}

				// Update forum stats, cache and etc
				DataAccessDriver.getInstance().newUserDAO().incrementPosts(post.getUserId());

				TopicsCommon.updateBoardStatus(topic, postId, firstPost, topicDao, forumDao);
				ForumRepository.updateForumStats(topic, user, post);
				ForumRepository.reloadForum(post.getForumId());

				int anonymousUser = SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID);

				if (user.getId() != anonymousUser) {
					SessionFacade.getTopicsReadTime().put(Integer.valueOf(topic.getId()),
						Long.valueOf(post.getTime().getTime()));
				}

				if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
					PostRepository.append(post.getTopicId(), PostCommon.preparePostForDisplay(post));
				}
			}
			else if(filtered){
			    JForumExecutionContext.setRedirect(this.request.getContextPath()
			         + "/posts/waitingModeration/"
			         + (firstPost ? 0 : topic.getId())
			         + "/" + topic.getForumId()
			         + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION)+"?filtered=true");                       
			  }else{
				JForumExecutionContext.setRedirect(this.request.getContextPath() 
					+ "/posts/waitingModeration/" 
					+ (firstPost ? 0 : topic.getId())
					+ "/" + topic.getForumId()
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			}

			if (delay > 0) {
				SessionFacade.setAttribute(ConfigKeys.LAST_POST_TIME, Long.valueOf(System.currentTimeMillis()));
			}
		}
		else {
			this.context.put("preview", true);
			this.context.put("post", post);
			this.context.put("start", this.request.getParameter("start"));

			Post postPreview = new Post(post);
			this.context.put("postPreview", PostCommon.preparePostForDisplay(postPreview));

			this.insert();
		}
	}

	private int startPage(Topic topic, int currentStart) {
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

		int newStart = (topic.getTotalReplies() + 1) / postsPerPage * postsPerPage;

		return (newStart > currentStart) ? newStart : currentStart;
	}

	public void delete()
	{
		if (!SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_POST_REMOVE)) {
			this.setTemplateName(TemplateKeys.POSTS_CANNOT_DELETE);
			this.context.put("message", I18n.getMessage("CannotRemovePost"));

			return;
		}

		// Post
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		Post post = postDao.selectById(this.request.getIntParameter("post_id"));

		if (post.getId() == 0) {
			this.postNotFound();
			return;
		}

		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		Topic topic = topicDao.selectRaw(post.getTopicId());

		if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
			return;
		}

		postDao.delete(post);

		// Karma
		KarmaDAO karmaDao = DataAccessDriver.getInstance().newKarmaDAO();
		karmaDao.deletePostKarma(post.getId());
		karmaDao.updateUserKarma(post.getUserId());

		// Attachments
		new AttachmentCommon(this.request, post.getForumId()).deleteAttachments(post.getId(), post.getForumId());

		// It was the last remaining post in the topic?
		int totalPosts = topicDao.getTotalPosts(post.getTopicId());

		if (totalPosts > 0) {
			// Topic
			topicDao.decrementTotalReplies(post.getTopicId());

			int maxPostId = topicDao.getMaxPostId(post.getTopicId());
			if (maxPostId > -1) {
				topicDao.setLastPostId(post.getTopicId(), maxPostId);
			}

			int minPostId = topicDao.getMinPostId(post.getTopicId());
			if (minPostId > -1) {
			  topicDao.setFirstPostId(post.getTopicId(), minPostId);
			}
	        
			// Forum
			ForumDAO fm = DataAccessDriver.getInstance().newForumDAO();

			maxPostId = fm.getMaxPostId(post.getForumId());
			if (maxPostId > -1) {
				fm.setLastPost(post.getForumId(), maxPostId);
			}

			String returnPath = this.request.getContextPath() + "/posts/list/";

			int page = ViewCommon.getStartPage();

			if (page > 0) {
				int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

				if (totalPosts % postsPerPage == 0) {
					page -= postsPerPage;
				}

				returnPath = new StringBuilder(returnPath).append(page).append('/').toString();
			}

			JForumExecutionContext.setRedirect(returnPath 
				+ post.getTopicId() 
				+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));

			// Update the cache
			if (TopicRepository.isTopicCached(topic)) {
				topic = topicDao.selectById(topic.getId());
				TopicRepository.updateTopic(topic);
			}
		}
		else {
			// OK, all posts were removed. Time to say goodbye
			TopicsCommon.deleteTopic(post.getTopicId(), post.getForumId(), false);

			JForumExecutionContext.setRedirect(this.request.getContextPath() 
				+ "/forums/show/" 
				+ post.getForumId()
				+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
		}

		this.request.addOrReplaceParameter("log_original_message", post.getText());
		ModerationHelper moderationHelper = new ModerationHelper();
		ModerationLog moderationLog = moderationHelper.buildModerationLogFromRequest();
		moderationLog.getPosterUser().setId(post.getUserId());
		moderationHelper.saveModerationLog(moderationLog);

		PostRepository.remove(topic.getId(), post);
		TopicRepository.loadMostRecentTopics();
		TopicRepository.loadHottestTopics();
		ForumRepository.reloadForum(post.getForumId());
	}

	private void watch(TopicDAO tm, int topicId, int userId)  
	{
		if (!tm.isUserSubscribed(topicId, userId)) {
			tm.subscribeUser(topicId, userId);
		}
	}

	public void watch()  
	{
		int topicId = this.request.getIntParameter("topic_id");
		int userId = SessionFacade.getUserSession().getUserId();

		this.watch(DataAccessDriver.getInstance().newTopicDAO(), topicId, userId);
		this.list();
	}

	public void unwatch()  
	{
		if (!SessionFacade.isLogged()) {
			this.setTemplateName(ViewCommon.contextToLogin());
		}
		else {
			int topicId = this.request.getIntParameter("topic_id");
			int userId = SessionFacade.getUserSession().getUserId();
			int start = ViewCommon.getStartPage();

			DataAccessDriver.getInstance().newTopicDAO().removeSubscription(topicId, userId);

			String returnPath = this.request.getContextPath() + "/posts/list/";

			if (start > 0) {
				returnPath += start + "/";
			}

			returnPath += topicId + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);

			this.setTemplateName(TemplateKeys.POSTS_UNWATCH);
			this.context.put("pageTitle", I18n.getMessage("PostShow.unwatch"));
			this.context.put("message", I18n.getMessage("ForumBase.unwatched", new String[] { returnPath }));
		}
	}

	public void downloadAttach()
	{
		int id = this.request.getIntParameter("attach_id");

		if (!SessionFacade.isLogged() && !SystemGlobals.getBoolValue(ConfigKeys.ATTACHMENTS_ANONYMOUS)) {
			String referer = this.request.getHeader("Referer");

			if (referer != null) {
				this.setTemplateName(ViewCommon.contextToLogin(referer));
			}
			else {
				this.setTemplateName(ViewCommon.contextToLogin());
			}

			return;
		}

		AttachmentDAO am = DataAccessDriver.getInstance().newAttachmentDAO();
		Attachment a = am.selectAttachmentById(id);

		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		Post post = postDao.selectById(a.getPostId());

		String forumId = Integer.toString(post.getForumId());

		boolean attachmentsEnabled = SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, forumId);
		boolean attachmentsDownload = SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD, forumId);

		if (!attachmentsEnabled && !attachmentsDownload) {
			this.setTemplateName(TemplateKeys.POSTS_CANNOT_DOWNLOAD);
			this.context.put("message", I18n.getMessage("Attachments.featureDisabled"));
			return;
		}

		String filename = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR)
			+ "/"
			+ a.getInfo().getPhysicalFilename();

		if (!new File(filename).exists()) {
			this.setTemplateName(TemplateKeys.POSTS_ATTACH_NOTFOUND);
			this.context.put("message", I18n.getMessage("Attachments.notFound"));
			return;
		}

		FileInputStream fis = null;
		OutputStream os = null;

		try {
			a.getInfo().setDownloadCount(a.getInfo().getDownloadCount() + 1);
			am.updateAttachment(a);

			fis = new FileInputStream(filename);
			os = response.getOutputStream();

			if (am.isPhysicalDownloadMode(a.getInfo().getExtension().getExtensionGroupId())) {
				this.response.setContentType("application/octet-stream");
			}
			else {
				this.response.setContentType(a.getInfo().getMimetype());
			}

			if (this.request.getHeader("User-Agent").indexOf("Firefox") != -1) {
				this.response.setHeader("Content-Disposition", "attachment; filename=\""
					+ new String(a.getInfo().getRealFilename().getBytes(SystemGlobals.getValue(ConfigKeys.ENCODING)),
						SystemGlobals.getValue(ConfigKeys.DEFAULT_CONTAINER_ENCODING)) + "\";");
			}
			else {
				this.response.setHeader("Content-Disposition", "attachment; filename=\""
					+ ViewCommon.toUtf8String(a.getInfo().getRealFilename()) + "\";");
			}

			this.response.setContentLength((int)a.getInfo().getFilesize());

			int c;
			byte[] b = new byte[4096];
			while ((c = fis.read(b)) != -1) {
				os.write(b, 0, c);
			}


			JForumExecutionContext.enableCustomContent(true);
		}
		catch (IOException e) {
			throw new ForumException(e);
		}
		finally {
			if (fis != null) {
				try { fis.close(); }
				catch (Exception e) { e.printStackTrace(); }
			}

			if (os != null) {
				try { os.close(); }
				catch (Exception e) { e.printStackTrace(); }
			}
		}
	}

	private void cannotEdit()
	{
		this.setTemplateName(TemplateKeys.POSTS_EDIT_CANNOTEDIT);
		this.context.put("message", I18n.getMessage("CannotEditPost"));
	}

	private void topicLocked() 
	{
		this.setTemplateName(TemplateKeys.POSTS_TOPIC_LOCKED);
		this.context.put("message", I18n.getMessage("PostShow.topicLocked"));
	}

	public void listSmilies()
	{
		this.setTemplateName(SystemGlobals.getValue(ConfigKeys.TEMPLATE_DIR) + "/empty.htm");
		this.setTemplateName(TemplateKeys.POSTS_LIST_SMILIES);
		this.context.put("smilies", SmiliesRepository.getSmilies());
	}

	private boolean isForumReadonly(int forumId, boolean isReply) {
		if (!SecurityRepository.canAccess(SecurityConstants.PERM_READ_ONLY_FORUMS, Integer.toString(forumId))) {
			if (isReply) {
				this.list();
			}
			else {
				JForumExecutionContext.setRedirect(this.request.getContextPath() + "/forums/show/" + forumId
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			}

			return true;
		}

		return false;
	}

	private boolean anonymousPost(int forumId)  
	{
		// Check if anonymous posts are allowed
		if (!SessionFacade.isLogged()
				&& !SecurityRepository.canAccess(SecurityConstants.PERM_ANONYMOUS_POST, Integer.toString(forumId))) {
			this.setTemplateName(ViewCommon.contextToLogin());

			return false;
		}

		return true;
	}
}
