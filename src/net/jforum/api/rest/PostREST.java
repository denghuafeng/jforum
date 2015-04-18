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
 * Created on 01/10/2011 14:32:22
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.api.rest;

import java.util.Date;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.APIException;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.PostAction;

import org.apache.commons.lang3.StringUtils;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Andowson Chang
 *
 */
public class PostREST extends Command {

	/* (non-Javadoc)
	 * @see net.jforum.Command#list()
	 */
	@Override
	public void list() {
		try {
			this.authenticate();
			// do nothing here
			// TODO: add implementation
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
		}
	}

	/**
	 * Creates a new post.
	 * Required parameters are "email", "forum_id", "subject" and "message".
	 */
	public void insert()
	{
		try {
			this.authenticate();

			final String email = this.requiredRequestParameter("email");
			final String forumId = this.requiredRequestParameter("forum_id");
			final String subject = this.requiredRequestParameter("subject");
			final String message = this.requiredRequestParameter("message");

			final UserDAO dao = DataAccessDriver.getInstance().newUserDAO();
			User user = dao.findByEmail(email);
			// If user's email not exists, use anonymous instead
			if (user == null) {
				user = new User();
				user.setId(SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
				user.setUsername(I18n.getMessage("Guest"));
			}
			// OK, time to insert the post
			final UserSession userSession = SessionFacade.getUserSession();
			userSession.setUserId(user.getId());
			userSession.setUsername(user.getUsername());
			String sessionId = userSession.getSessionId(); 
			userSession.setStartTime(new Date(System.currentTimeMillis()));
			SessionFacade.makeLogged();

			SessionFacade.removeAttribute(ConfigKeys.LAST_POST_TIME);
			SessionFacade.setAttribute(ConfigKeys.REQUEST_IGNORE_CAPTCHA, "1");

			final Post post = new Post();
			post.setForumId(Integer.parseInt(forumId));
			post.setSubject(subject);
			post.setText(message);
			this.insertMessage(user, post);
			String postLink = JForumExecutionContext.getRedirectTo(); 
			JForumExecutionContext.setRedirect(null); 
			this.setTemplateName(TemplateKeys.API_POST_INSERT); 
			this.context.put("postLink", postLink);
			SessionFacade.makeUnlogged();
			SessionFacade.remove(sessionId);
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
		}
	}

	/**
	 * Calls {@link PostAction#insertSave()}
	 * @param post the post
	 * @param user the user who's doing the post
	 */
	private void insertMessage(final User user, final Post post)
	{
		this.addDataToRequest(user, post);

		final PostAction postAction = new PostAction(JForumExecutionContext.getRequest(), JForumExecutionContext.newSimpleHash());
		postAction.insertSave();
	}

	/**
	 * Extracts information from a mail message and adds it to the request context
	 * @param post the post
	 * @param user the user who's doing the post
	 */
	private void addDataToRequest(final User user, final Post post)
	{
		final RequestContext request = JForumExecutionContext.getRequest(); 

		request.addParameter("topic_type", Integer.toString(Topic.TYPE_NORMAL));
		request.addParameter("quick", "1");

		final int topicId = post.getTopicId();
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
	 * Retrieves a parameter from the request and ensures it exists
	 * @param paramName the parameter name to retrieve its value
	 * @return the parameter value
	 * @throws APIException if the parameter is not found or its value is empty
	 */
	private String requiredRequestParameter(final String paramName)
	{
		final String value = this.request.getParameter(paramName);

		if (StringUtils.isBlank(value)) {
			throw new APIException("The parameter '" + paramName + "' was not found");
		}

		return value;
	}

	/**
	 * Tries to authenticate the user accessing the API
	 * @throws APIException if the authentication fails
	 */
	private void authenticate()
	{
		final String apiKey = this.requiredRequestParameter("api_key");

		final RESTAuthentication auth = new RESTAuthentication();

		if (!auth.validateApiKey(apiKey)) {
			throw new APIException("The provided API authentication information is not valid");
		}
	}

	public Template process(final RequestContext request, final ResponseContext response, final SimpleHash context)
	{
		JForumExecutionContext.setContentType("text/xml");
		return super.process(request, response, context);
	}
}
