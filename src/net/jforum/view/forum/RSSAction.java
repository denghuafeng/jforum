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
 * Created on 13/10/2004 23:47:06
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.*;
import net.jforum.entities.*;
import net.jforum.repository.ForumRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.util.rss.*;
import net.jforum.view.forum.common.Stats;
import net.jforum.view.forum.common.TopicsCommon;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class RSSAction extends Command 
{
	private static final String RSS_CONTENTS = "rssContents";
	
	/**
	 * RSS for all N first topics for some given forum
	 */
	public void forumTopics()
	{
		final int forumId = this.request.getIntParameter("forum_id");		
		final Forum forum = ForumRepository.getForum(forumId);
		
		// Handle if forum doesn't  exist
		if (forum == null) {
			this.context.put(RSS_CONTENTS, "<!-- The requested forum does not exist-->");
			return;
		}
		
		if (!TopicsCommon.isTopicAccessible(forumId)) {
			JForumExecutionContext.requestBasicAuthentication();
            return;
		}
		
		final List<Post> posts = DataAccessDriver.getInstance().newPostDAO().selectLatestByForumForRSS(
			forumId, SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE));
				
		final String[] param = { forum.getName() };
		
		final RSSAware rss = new TopicRSS(I18n.getMessage("RSS.ForumTopics.title", param),
			I18n.getMessage("RSS.ForumTopics.description", param),
			forumId, 
			posts);
		
		this.context.put(RSS_CONTENTS, rss.createRSS());
        Stats.record("RSS forum", request.getRequestURL());
	}
	
	/**
	 * RSS for all N first posts for some given topic
	 */
	public void topicPosts()
	{
		final int topicId = this.request.getIntParameter("topic_id");

		final TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		
		final Topic topic = topicDao.selectById(topicId);
		
		// Handle if topic doesn't  exist
		if (topic.getId() == 0) {
			this.context.put(RSS_CONTENTS, "<!-- The requested topic does not exist-->");
			return;
		}
		
		if (!TopicsCommon.isTopicAccessible(topic.getForumId())) {
			JForumExecutionContext.requestBasicAuthentication(); 
            return;
		}
		
		topicDao.incrementTotalViews(topic.getId());
		
		final PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		final List<Post> posts = postDao.selectAllByTopic(topicId);
		
		final String[] param = { topic.getTitle() };
		
		final String title = I18n.getMessage("RSS.TopicPosts.title", param);
		final String description = I18n.getMessage("RSS.TopicPosts.description", param);

		final RSSAware rss = new TopicPostsRSS(title, description, topic.getForumId(), posts);
		this.context.put(RSS_CONTENTS, rss.createRSS());
        Stats.record("RSS single topic", request.getRequestURL());
	}
	
	public void recentTopics()
	{
		final String title = I18n.getMessage("RSS.RecentTopics.title", 
			new Object[] { SystemGlobals.getValue(ConfigKeys.FORUM_NAME) });
		final String description = I18n.getMessage("RSS.RecentTopics.description");
		
		final List<Post> posts = DataAccessDriver.getInstance().newPostDAO().selectLatestForRSS(
			SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS));

		final List<Post> authPosts = new ArrayList<Post>();  
		final Iterator<Post> iter = posts.iterator();  
		while ( iter.hasNext() ) {  
		     Post post = iter.next();  
		     if ( TopicsCommon.isTopicAccessible(post.getForumId(), false) ) {  
		         authPosts.add(post);  
		     }  
		 }  
		RSSAware rss = new RecentTopicsRSS(title, description, authPosts);
		this.context.put(RSS_CONTENTS, rss.createRSS());
        Stats.record("RSS recent topics", request.getRequestURL());
	}

	public void hottestTopics()
	{
		String title = I18n.getMessage("RSS.HottestTopics.title", 
			new Object[] { SystemGlobals.getValue(ConfigKeys.FORUM_NAME) });
		String description = I18n.getMessage("RSS.HottestTopics.description");
		
		List<Post> posts = DataAccessDriver.getInstance().newPostDAO().selectHotForRSS(
			SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS));

		List<Post> authPosts = new ArrayList<Post>();  
		Iterator<Post> iter = posts.iterator();  
		while ( iter.hasNext() ) {  
		     Post post = iter.next();  
		     if ( TopicsCommon.isTopicAccessible(post.getForumId(), false) ) {  
		         authPosts.add(post);  
		     }  
		 }  
		RSSAware rss = new HottestTopicsRSS(title, description, authPosts);
		this.context.put(RSS_CONTENTS, rss.createRSS());
        Stats.record("RSS hot topics", request.getRequestURL());
	}

    public void userPosts() {
        int userId = this.request.getIntParameter("user_id");
        UserDAO userDAO = DataAccessDriver.getInstance().newUserDAO();
        User user = userDAO.selectById(userId);

        String title = I18n.getMessage("RSS.UserPosts.title", 
			new Object[] { SystemGlobals.getValue(ConfigKeys.FORUM_NAME), user.getUsername()});
        String description = I18n.getMessage("RSS.UserPosts.description", 
			new Object[] { user.getName() });

        PostDAO postDAO = DataAccessDriver.getInstance().newPostDAO();
        int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
        List<Post> posts = postDAO.selectByUserByLimit(userId, 0, postsPerPage);

		// Remove topics that the user should not see (like MO topics for non-noderators)
		removeUnauthorizedPosts(posts);

        RSSAware rss = new UserPostsRSS(title, description, userId, posts);
        this.context.put("rssContents", rss.createRSS());
        Stats.record("RSS user posts", request.getRequestURL());
    }

    /**
    * Remove topics that the user should not see (like MO topics for non-noderators)
    * @param posts
    */
    private void removeUnauthorizedPosts(List<Post> posts) {
        for (Iterator<Post> iter = posts.iterator(); iter.hasNext(); ) {
            Post p = (Post) iter.next();
            Forum f = ForumRepository.getForum(p.getForumId());
            if ((f == null)
					|| !ForumRepository.isCategoryAccessible(f.getCategoryId())
		     		|| !TopicsCommon.isTopicAccessible(p.getForumId(), false))
                iter.remove();
        }
    }

	/**
	 * Empty method, do nothing
	 *  
	 * @see net.jforum.Command#list()
	 */
	public void list()
	{
		// Empty method
	}
	
	/** 
	 * @see net.jforum.Command#process(net.jforum.context.RequestContext, net.jforum.context.ResponseContext, freemarker.template.SimpleHash) 
	 */
	public Template process(final RequestContext request,
			final ResponseContext response,
			final SimpleHash context)
	{
        if (!SessionFacade.isLogged() && UserAction.hasBasicAuthentication(request)) {
            new UserAction().validateLogin(request);
            JForumExecutionContext.setRedirect(null);
        }

        JForumExecutionContext.setContentType("text/xml");
		super.setTemplateName(TemplateKeys.RSS);

		return super.process(request, response, context);
	}

}
