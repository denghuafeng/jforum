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
 * Created on 04/09/2006 21:23:22
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.api.rest;

import java.util.List;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.entities.User;
import net.jforum.exceptions.APIException;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;

import org.apache.commons.lang3.StringUtils;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class UserREST extends Command
{
	/**
	 * List all users
	 */
	public void list()
	{
		try {
			this.authenticate();
			
			final UserDAO dao = DataAccessDriver.getInstance().newUserDAO();
			final List<User> users = dao.selectAll();
		
			this.setTemplateName(TemplateKeys.API_USER_LIST);
			this.context.put("users", users);
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
		}
	}
	
	/**
	 * Creates a new user.
	 * Required parameters are "username", "email" and "password".
	 */
	public void insert()
	{
		try {
			this.authenticate();
			
			final String username = this.requiredRequestParameter("username");
			final String email = this.requiredRequestParameter("email");
			final String password = this.requiredRequestParameter("password");
			
			if (username.length() > SystemGlobals.getIntValue(ConfigKeys.USERNAME_MAX_LENGTH)) {
				throw new APIException(I18n.getMessage("User.usernameTooBig"));
			}
			
			if (username.indexOf('<') > -1 || username.indexOf('>') > -1) {
				throw new APIException(I18n.getMessage("User.usernameInvalidChars"));
			}
			
			final UserDAO dao = DataAccessDriver.getInstance().newUserDAO();

			if (dao.isUsernameRegistered(username)) {
				throw new APIException(I18n.getMessage("UsernameExists"));
			}
			
			if (dao.findByEmail(email) != null) {
				throw new APIException(I18n.getMessage("User.emailExists", new Object[] { email }));
			}
			
			// OK, time to insert the user
			final User user = new User();
			user.setUsername(username);
			user.setEmail(email);
			user.setPassword(password);
			
			final int userId = dao.addNew(user);
			
			this.setTemplateName(TemplateKeys.API_USER_INSERT);
			this.context.put("userId", Integer.valueOf(userId));
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
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
