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
 * Class created on Jul 15, 2005
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.summary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.SummaryDAO;
import net.jforum.entities.Post;
import net.jforum.entities.User;
import net.jforum.util.mail.Spammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.view.forum.common.ViewCommon;

import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;

/**
 * Manage the Summary sends.
 * 
 * @see net.jforum.summary.SummaryJob
 * @see net.jforum.summary.SummaryScheduler
 * 
 * @author Franklin S. Dattein (<a href="mailto:franklin@hp.com">franklin@hp.com</a>)
 * @version $Id$
 */
public class SummaryModel extends Spammer
{
	private static final Logger LOGGER = Logger.getLogger(SummaryModel.class);
	private final SummaryDAO dao;

	public SummaryModel()
	{
		super();
		this.dao = DataAccessDriver.getInstance().newSummaryDAO();
	}

	public void sendPostsSummary(final List<String> recipients)
	{
		if (recipients.size() > 0) { // make sure somebody want to receive it
			// Gets a Date seven days before now
			final int daysBefore = Integer.parseInt(SystemGlobals.getValue(ConfigKeys.SUMMARY_DAYS_BEFORE));

			// New date "X" days before now, where "X" is the number set on the variable daysBefore
			final long dateBefore = Calendar.getInstance().getTimeInMillis() - (1000L * 60 * 60 * 24 * daysBefore);

			final List<Post> posts = listPosts(new Date(dateBefore), new Date());
			if (posts.size() > 0) { // make sure there is at least one new post

				final String forumLink = ViewCommon.getForumLink();

				final SimpleHash params = JForumExecutionContext.newSimpleHash();
				params.put("posts", posts);
				params.put("url", forumLink);
				params.put("extension", SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));

				final String subject = SystemGlobals.getValue(ConfigKeys.MAIL_SUMMARY_SUBJECT);

				LOGGER.info("Sending Weekly summary...");

				this.setUsers(this.recipientsAsUsers(recipients));
				this.setTemplateParams(params);

				this.prepareMessage(subject, SystemGlobals.getValue(ConfigKeys.MAIL_SUMMARY_FILE));
				super.dispatchMessages();
			}
		}
	}

	private List<User> recipientsAsUsers(final List<String> recipients)
	{
		final List<User> list = new ArrayList<User>();

		for (String email : recipients) {
			LOGGER.debug("email="+email);
			final User user = new User();
			user.setEmail(email);

			list.add(user);
		}

		return list;
	}

	/**
	 * List all recipients able to receive posts summaries.
	 * 
	 * @return List of users
	 */
	public List<String> listRecipients()
	{
		return this.dao.listRecipients();
	}

	/**
	 * List last posts of a period like a week, day or month.
	 * 
	 * @param firstDate First date of a period.
	 * @param lastDate Last date of a period.
	 * @return List of Posts
	 */
	public List<Post> listPosts(final Date firstDate, final Date lastDate)
	{
		return this.dao.selectLastPosts(firstDate, lastDate);
	}
}
