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
 * Created on 26/08/2006 22:20:46
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.api.integration.mail.pop;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.JForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.standard.StandardRequestContext;
import net.jforum.context.standard.StandardSessionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.view.forum.PostAction;

import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class POPPostAction
{
	private static final Logger LOGGER = Logger.getLogger(POPPostAction.class);

	public void insertMessages(final POPParser parser)
	{
		final long currentTimestamp = System.currentTimeMillis();
		int counter = 0;

		try {
			final JForumExecutionContext executionContext = JForumExecutionContext.get();

			final RequestContext request = new StandardRequestContext();
			executionContext.setForumContext(new JForumContext("/", "", request, null));

			JForumExecutionContext.set(executionContext);

			SessionFacade.setAttribute(ConfigKeys.TOPICS_READ_TIME, new HashMap<Integer, Long>());

			for (final Iterator<POPMessage> iter = parser.getMessages().iterator(); iter.hasNext(); ) {
				final POPMessage message = (POPMessage)iter.next();
				final String sessionId = currentTimestamp + message.getSender() + counter++;

				request.getSessionContext().setAttribute(StandardSessionContext.SESSION_ID, sessionId);

				final User user = this.findUser(message.getSender());

				if (user == null) {
					LOGGER.warn("Could not find user with email " + message.getSender() + ". Ignoring his message.");
					continue;
				}

				try {
					final UserSession userSession = new UserSession();
					userSession.setUserId(user.getId());
					userSession.setUsername(userSession.getUsername());
					userSession.setSessionId(sessionId);
					userSession.setStartTime(new Date(System.currentTimeMillis()));

					SessionFacade.add(userSession, sessionId);
					SessionFacade.setAttribute(ConfigKeys.LOGGED, "1");

					SessionFacade.removeAttribute(ConfigKeys.LAST_POST_TIME);
					SessionFacade.setAttribute(ConfigKeys.REQUEST_IGNORE_CAPTCHA, "1");

					this.insertMessage(message, user);
				}
				finally {
					SessionFacade.remove(sessionId);
				}
			}
		}
		finally {
			JForumExecutionContext.finish();
		}
	}

	/**
	 * Calls {@link PostAction#insertSave()}
	 * @param message the mail message
	 * @param user the user who's sent the message
	 */
	private void insertMessage(final POPMessage message, final User user)
	{
		this.addDataToRequest(message, user);

		final PostAction postAction = new PostAction(JForumExecutionContext.getRequest(), JForumExecutionContext.newSimpleHash());
		postAction.insertSave();
	}

	/**
	 * Extracts information from a mail message and adds it to the request context
	 * @param message the mail message
	 * @param user the user who's sending the message
	 */
	private void addDataToRequest(final POPMessage message, final User user)
	{
		final RequestContext request = JForumExecutionContext.getRequest(); 

		request.addParameter("forum_id", Integer.toString(this.discoverForumId(message.getListEmail())));
		request.addParameter("topic_type", Integer.toString(Topic.TYPE_NORMAL));
		request.addParameter("quick", "1");
		request.addParameter("subject", message.getSubject());
		request.addParameter("message", message.getMessage());

		final int topicId = this.discoverTopicId(message);

		if (topicId > 0) {
			request.addParameter("topic_id", Integer.toString(topicId));
		}

		if (!user.isBbCodeEnabled()) {
			request.addParameter("disable_bbcode", "on");
		}

		if (!user.isSmiliesEnabled()) {
			request.addParameter("disable_smilies", "on");
		}

		if (!user.isHtmlEnabled()) {
			request.addParameter("disable_html", "on");
		}
	}

	/**
	 * Tries to extract message relationship from the headers
	 * @param message the message to extract headers from
	 * @return the topic id, if found, or 0 (zero) otherwise
	 */
	private int discoverTopicId(final POPMessage message)
	{
		int topicId = 0;

		final String inReplyTo = message.getInReplyTo();

		if (inReplyTo != null) {
			topicId = MessageId.parse(inReplyTo).getTopicId();
		}

		return topicId;
	}

	/**
	 * Given an email address, finds the forum instance associated to it
	 * @param listEmail the forum's email address to search for
	 * @return the forum's id, or 0 (zero) if nothing was found
	 */
	private int discoverForumId(final String listEmail)
	{
		final ForumDAO dao = DataAccessDriver.getInstance().newForumDAO();
		return dao.discoverForumId(listEmail);
	}

	/**
	 * Finds a user by his email address
	 * @param email the email address to use in the search
	 * @return the matching record, or null if nothing was found
	 */
	private User findUser(final String email)
	{
		return DataAccessDriver.getInstance().newUserDAO().findByEmail(email);
	}
}
