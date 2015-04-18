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
 * Created on Sept 17, 2010
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Forum;
import net.jforum.entities.TopDownloadInfo;
import net.jforum.entities.Topic;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.TopicsCommon;

/**
 * @author Andowson
 *
 */
public class TopDownloadsAction extends Command 
{
	private transient List<Forum> forums;
	private transient List<Topic> topics;
	
	/* (non-Javadoc)
	 * @see net.jforum.Command#list()
	 */
	@Override
	public void list() {
		final int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);

		this.setTemplateName(TemplateKeys.TOP_DOWNLOADS_LIST);
		
		this.context.put("postsPerPage", Integer.valueOf(postsPerPage));
		this.context.put("topDownloads", this.topDownloads());
		this.context.put("forums", this.forums);
		this.context.put("topics", this.topics);
		this.context.put("pageTitle", I18n.getMessage("ForumBase.topDownloads"));

		this.request.removeAttribute("template");
	}
	
	private List<TopDownloadInfo> topDownloads()
	{
		final int limit = SystemGlobals.getIntValue(ConfigKeys.TOP_DOWNLOADS);
		final AttachmentDAO dao = DataAccessDriver.getInstance().newAttachmentDAO();
		final List<TopDownloadInfo> tmpTopDownloads = dao.selectTopDownloads(limit);
		
		this.forums = new ArrayList<Forum>(limit);
		this.topics = new ArrayList<Topic>(limit);

		for (final Iterator<TopDownloadInfo> iter = tmpTopDownloads.iterator(); iter.hasNext(); ) {
			final TopDownloadInfo tdi = iter.next();
			
			if (TopicsCommon.isTopicAccessible(tdi.getForumId())) {
				// Get name of forum that the topic refers to
				final Forum forum = ForumRepository.getForum(tdi.getForumId());				
				Topic topic = new Topic();
				topic.setForumId(tdi.getForumId());
				topic.setId(tdi.getTopicId());
				topic =	TopicRepository.getTopic(topic);
				forums.add(forum);
				topics.add(topic);
			}
			else {
				iter.remove();
			}
		}
		
		JForumExecutionContext.getRequest().removeAttribute("template");
		
		return tmpTopDownloads;
	}	
}
