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
 * Created on Mar 02, 2007
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.Stats;
import net.jforum.view.forum.common.TopicsCommon;
import net.jforum.view.forum.common.ViewCommon;

/**
 * Display a list of hottest Topics
 * 
 * @author James Yong
 * @author Rafael Steil
 * @author Andowson Chang 
 * @version $Id$
 */
public class HottestTopicsAction extends Command 
{
	private transient List<Forum> forums;

	public void list()
	{
		final int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

		this.setTemplateName(TemplateKeys.HOTTEST_LIST);
		
		this.context.put("postsPerPage", Integer.valueOf(postsPerPage));
		this.context.put("topics", this.topics());
		this.context.put("forums", this.forums);
		this.context.put("pageTitle", I18n.getMessage("ForumBase.hottestTopics"));

		TopicsCommon.topicListingBase();
		this.request.removeAttribute("template");
	}
	
	private List<Topic> topics()
	{
        Stats.record("Hot topics page", request.getRequestURL());

		final int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		final List<Topic> tmpTopics = TopicRepository.getHottestTopics();
		
		this.forums = new ArrayList<Forum>(postsPerPage);

		for (final Iterator<Topic> iter = tmpTopics.iterator(); iter.hasNext(); ) {
			final Topic topic = (Topic)iter.next();
			
			if (TopicsCommon.isTopicAccessible(topic.getForumId())) {
				// Get name of forum that the topic refers to
				final Forum forum = ForumRepository.getForum(topic.getForumId());
				forums.add(forum);
			}
			else {
				iter.remove();
			}
		}
		
		JForumExecutionContext.getRequest().removeAttribute("template");
		
		return TopicsCommon.prepareTopics(tmpTopics);
	}

	public void showTopicsByUser() 
	{
		final DataAccessDriver dad = DataAccessDriver.getInstance();
		
		final UserDAO udao = dad.newUserDAO();
		final User user = udao.selectById(this.request.getIntParameter("user_id"));
		
		if (user.getId() == 0) {
			this.context.put("message", I18n.getMessage("User.notFound"));
			this.setTemplateName(TemplateKeys.USER_NOT_FOUND);
			return;
		} 
			
		TopicsCommon.topicListingBase();
		
		final int start = ViewCommon.getStartPage();
		final int topicsPerPage = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);
		final int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		
		this.setTemplateName(TemplateKeys.HOTTEST_USER_TOPICS_SHOW);
		
		int totalTopics = dad.newTopicDAO().countUserTopics(user.getId());
		
		this.context.put("u", user);
		this.context.put("pageTitle", I18n.getMessage("ForumListing.userTopics") + " " + user.getUsername());
		
		this.context.put("postsPerPage", Integer.valueOf(postsPerPage));
		
		final List<Topic> topics = dad.newTopicDAO().selectByUserByLimit(user.getId(), start, topicsPerPage);
		
		final List<Topic> list = TopicsCommon.prepareTopics(topics);
		final Map<Integer, Forum> forums = new HashMap<Integer, Forum>();
		
		for (final Iterator<Topic> iter = list.iterator(); iter.hasNext(); ) {
			final Topic topic = (Topic)iter.next();
			
			final Forum forum = ForumRepository.getForum(topic.getForumId());
			
			if (forum == null) {
				iter.remove();
				totalTopics--;
				continue;
			}
			
			forums.put(Integer.valueOf(topic.getForumId()), forum);
		}
		
		this.context.put("topics", list);
		this.context.put("forums", forums);
		
		ViewCommon.contextToPagination(start, totalTopics, topicsPerPage);
	}
}
