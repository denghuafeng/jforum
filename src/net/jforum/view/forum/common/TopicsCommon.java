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
 * Created on 17/10/2004 23:54:47
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.PostRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.I18n;
import net.jforum.util.concurrent.Executor;
import net.jforum.util.mail.EmailSenderTask;
import net.jforum.util.mail.TopicReplySpammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.view.forum.ModerationHelper;
import freemarker.template.SimpleHash;

/**
 * General utilities methods for topic manipulation.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class TopicsCommon 
{
	private static final Object MUTEXT = new Object();
	
	/**
	 * List all first 'n' topics of a given forum.
	 * This method returns no more than <code>ConfigKeys.TOPICS_PER_PAGE</code>
	 * topics for the forum. 
	 * 
	 * @param forumId The forum id to which the topics belongs to
	 * @param start The start fetching index
	 * @return <code>java.util.List</code> containing the topics found.
	 */
	public static List<Topic> topicsByForum(int forumId, int start)
	{
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		int topicsPerPage = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);
		List<Topic> topics;
		
		// Try to get the first's page of topics from the cache
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			int topicCacheSize = SystemGlobals.getIntValue(ConfigKeys.TOPIC_CACHE_SIZE);
			topics = TopicRepository.getTopics(forumId);

			if (topics.isEmpty() || !TopicRepository.isLoaded(forumId) || start+topicsPerPage >= topicCacheSize) {
				synchronized (MUTEXT) {
					if (topics.isEmpty() || !TopicRepository.isLoaded(forumId) || start+topicsPerPage >= topicCacheSize) {
						topics = tm.selectAllByForum(forumId);
						TopicRepository.addAll(forumId, topics);
					}
				}
			}
		}
		else {
			topics = tm.selectAllByForumByLimit(forumId, start, topicsPerPage);
		}
		
		int size = topics.size();
		
		while (size < start) {
			start -= topicsPerPage;
		}
		if (start < 0) {
			start = 0;
		}
		
		return topics.subList(start, (size < start + topicsPerPage) ? size : start + topicsPerPage);
	}
	
	/**
	 * Prepare the topics for listing.
	 * This method does some preparation for a set ot <code>net.jforum.entities.Topic</code>
	 * instances for the current user, like verification if the user already
	 * read the topic, if pagination is a need and so on.
	 * 
	 * @param topics The topics to process
	 * @return The post-processed topics.
	 */
	public static List<Topic> prepareTopics(List<Topic> topics)
	{
		UserSession userSession = SessionFacade.getUserSession();

		long lastVisit = userSession.getLastVisit().getTime();
		int hotBegin = SystemGlobals.getIntValue(ConfigKeys.HOT_TOPIC_BEGIN);
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		
		List<Topic> newTopics = new ArrayList<Topic>(topics.size());
		Map<Integer, Long> topicsReadTime = SessionFacade.getTopicsReadTime();
		Map<Integer, Long> topicReadTimeByForum = SessionFacade.getTopicsReadTimeByForum();
		
		boolean checkUnread = (userSession.getUserId() 
			!= SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
		
		for (Iterator<Topic> iter = topics.iterator(); iter.hasNext(); ) {
			Topic topic = (Topic)iter.next();
			
			boolean read = false;
			boolean isReadByForum = false;
			long lastPostTime = topic.getLastPostDate().getTime();
			
			if (topicReadTimeByForum != null) {
				Long currentForumTime = topicReadTimeByForum.get(Integer.valueOf(topic.getForumId()));
				isReadByForum = currentForumTime != null && lastPostTime < currentForumTime.longValue();
			}
			
			boolean isTopicTimeOlder = !isReadByForum && lastPostTime <= lastVisit;
			
			if (!checkUnread || isReadByForum || isTopicTimeOlder) {
				read = true;
			}
			else {
				Integer topicId = Integer.valueOf(topic.getId());
				Long currentTopicTime = topicsReadTime.get(topicId);
				
				if (currentTopicTime != null) {
					read = currentTopicTime.longValue() > lastPostTime;
				}
			}

			if (topic.getTotalReplies() + 1 > postsPerPage) {
				topic.setPaginate(true);
				topic.setTotalPages(Double.valueOf(Math.floor(topic.getTotalReplies() / (double)postsPerPage)));
			}
			else {
				topic.setPaginate(false);
				topic.setTotalPages(Double.valueOf(0));
			}
			
			// Check if this is a hot topic
			topic.setHot(topic.getTotalReplies() >= hotBegin);
			
			topic.setRead(read);
			newTopics.add(topic);
		}
		
		return newTopics;
	}

	/**
	 * Common properties to be used when showing topic data
	 */
	public static void topicListingBase()
	{
		SimpleHash context = JForumExecutionContext.getTemplateContext();
		
		// Topic Types
		context.put("TOPIC_ANNOUNCE", Integer.valueOf(Topic.TYPE_ANNOUNCE));
		context.put("TOPIC_STICKY", Integer.valueOf(Topic.TYPE_STICKY));
		context.put("TOPIC_NORMAL", Integer.valueOf(Topic.TYPE_NORMAL));
	
		// Topic Status
		context.put("STATUS_LOCKED", Integer.valueOf(Topic.STATUS_LOCKED));
		context.put("STATUS_UNLOCKED", Integer.valueOf(Topic.STATUS_UNLOCKED));
		
		// Moderation
		PermissionControl pc = SecurityRepository.get(SessionFacade.getUserSession().getUserId());
		
		context.put("moderator", pc.canAccess(SecurityConstants.PERM_MODERATION));
		context.put("can_remove_posts", pc.canAccess(SecurityConstants.PERM_MODERATION_POST_REMOVE));
		context.put("can_move_topics", pc.canAccess(SecurityConstants.PERM_MODERATION_TOPIC_MOVE));
		context.put("can_lockUnlock_topics", pc.canAccess(SecurityConstants.PERM_MODERATION_TOPIC_LOCK_UNLOCK));
		context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
	}
	
	/**
	 * Checks if the user is allowed to view the topic.
	 * If the currently logged user does not have access
	 * to the forum, the template context will be set to show
	 * an error message to the user, by calling
	 * <blockquote>new ModerationHelper().denied(I18n.getMessage("PostShow.denied"))</blockquote>
	 * @param forumId The forum id to which the topics belongs to
	 * @return <code>true</code> if the topic is accessible, <code>false</code> otherwise
	 */
	public static boolean isTopicAccessible(int forumId)
	{
		return isTopicAccessible(forumId, true);
	}

	/**
	 * Checks if the user is allowed to view the topic.
	 * If the currently logged user does not have access
	 * to the forum, the template context will be set to show
	 * an error message to the user, by calling
	 * <blockquote>new ModerationHelper().denied(I18n.getMessage("PostShow.denied"))</blockquote>
	 * @param forumId The forum id to which the topics belongs to
	 * @param showError should show an error message or not, <code>true</code> will show, <code>false</code> otherwise
	 * @return <code>true</code> if the topic is accessible, <code>false</code> otherwise
	 */
	public static boolean isTopicAccessible(int forumId, boolean showError)
	{
		Forum forum = ForumRepository.getForum(forumId);
		
		if (forum == null || !ForumRepository.isCategoryAccessible(forum.getCategoryId())) {
			if (showError) {
				new ModerationHelper().denied(I18n.getMessage("PostShow.denied"));
			}
			return false;
		}

		return true;
	}
	
	/**
	 * Sends a "new post" notification message to all users watching the topic.
	 * 
	 * @param topic The changed topic
	 * @param post The new message
	 */
	public static void notifyUsers(Topic topic, Post post)
	{
		if (SystemGlobals.getBoolValue(ConfigKeys.MAIL_NOTIFY_ANSWERS)) {
			TopicDAO dao = DataAccessDriver.getInstance().newTopicDAO();
			List<User> usersToNotify = dao.notifyUsers(topic);

			// We only have to send an email if there are users
			// subscribed to the topic
			if (usersToNotify != null && !usersToNotify.isEmpty()) {
				Executor.execute(new EmailSenderTask(new TopicReplySpammer(topic, post, usersToNotify)));
			}
		}
	}
	
	/**
	 * Updates the board status after a new post is inserted.
	 * This method is used in conjunct with moderation manipulation. 
	 * It will increase by 1 the number of replies of the tpoic, set the
	 * last post id for the topic and the forum and refresh the cache. 
	 * 
	 * @param topic Topic The topic to update
	 * @param lastPostId int The id of the last post
	 * @param topicDao TopicDAO A TopicModel instance
	 * @param forumDao ForumDAO A ForumModel instance
     * @param firstPost boolean
	 */
	public static synchronized void updateBoardStatus(Topic topic, int lastPostId, boolean firstPost, 
		TopicDAO topicDao, ForumDAO forumDao)
	{
		topic.setLastPostId(lastPostId);
		topicDao.update(topic);
		
		forumDao.setLastPost(topic.getForumId(), lastPostId);
		
		if (firstPost) {
			forumDao.incrementTotalTopics(topic.getForumId(), 1);
		}
		else {
			topicDao.incrementTotalReplies(topic.getId());
		}
		
		TopicRepository.addTopic(topic);
		TopicRepository.pushTopic(topic);
		
		ForumRepository.incrementTotalMessages();
	}
	
	/**
	 * Deletes a topic.
	 * This method will remove the topic from the database,
	 * clear the entry from the cache and update the last 
	 * post info for the associated forum.
	 * @param topicId The topic id to remove
	 * @param fromModeration boolean 
     * @param forumId int
	 */
	public static synchronized void deleteTopic(int topicId, int forumId, boolean fromModeration)
	{
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		
		Topic topic = new Topic();
		topic.setId(topicId);
		topic.setForumId(forumId);

		topicDao.delete(topic, fromModeration);

		if (!fromModeration) {
			// Updates the Recent Topics if it contains this topic
			TopicRepository.loadMostRecentTopics();
			
            // Updates the Hottest Topics if it contains this topic
			TopicRepository.loadHottestTopics();
			TopicRepository.clearCache(forumId);
			PostRepository.clearCache(topicId);
			topicDao.removeSubscriptionByTopic(topicId);
		}
	}
}
