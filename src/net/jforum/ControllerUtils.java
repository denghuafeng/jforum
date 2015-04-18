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
 * Created on Mar 17, 2005 5:38:11 PM
 *
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.lang.reflect.Method;
import java.util.Date;

import javax.servlet.http.Cookie;

import net.jforum.context.ForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.SessionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.DatabaseException;
import net.jforum.exceptions.ForumException;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.sso.SSO;
import net.jforum.sso.SSOUtils;
import net.jforum.util.I18n;
import net.jforum.util.Hash;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.view.forum.common.BannerCommon;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;

/**
 * Common methods used by the controller.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class ControllerUtils
{
	private static final Logger LOGGER = Logger.getLogger(ControllerUtils.class);
	
	/**
	 * Setup common variables used by almost all templates.
	 * 
	 * @param context SimpleHash The context to use
     * @param jforumContext JForumContext
	 */
	public void prepareTemplateContext(final SimpleHash context, final ForumContext jforumContext)
	{
		final RequestContext request = JForumExecutionContext.getRequest();
		
		context.put("karmaEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_KARMA_ENABLED));
		context.put("dateTimeFormat", SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
		context.put("autoLoginEnabled", SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED));
		context.put("sso", ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE)));
		context.put("contextPath", request.getContextPath());
		context.put("serverName", request.getServerName());
		context.put("templateName", SystemGlobals.getValue(ConfigKeys.TEMPLATE_DIR));
		context.put("extension", SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
		context.put("serverPort", Integer.toString(request.getServerPort()));
		context.put("I18n", I18n.getInstance());
		context.put("version", SystemGlobals.getValue(ConfigKeys.VERSION));
		context.put("forumTitle", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_TITLE));
		context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_TITLE));
		context.put("metaKeywords", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_METATAG_KEYWORDS));
		context.put("metaDescription", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_METATAG_DESCRIPTION));
		context.put("forumLink", SystemGlobals.getValue(ConfigKeys.FORUM_LINK));
		context.put("homepageLink", SystemGlobals.getValue(ConfigKeys.HOMEPAGE_LINK));
		context.put("encoding", SystemGlobals.getValue(ConfigKeys.ENCODING));
		context.put("bookmarksEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_BOOKMARKS_ENABLED));
		context.put("canAccessModerationLog", SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_LOG));
		context.put("JForumContext", jforumContext);
		context.put("bannerCommon", new BannerCommon());
		context.put("timestamp", Long.valueOf(System.currentTimeMillis()));
        String googleTracker = SystemGlobals.getValue(ConfigKeys.GA_ID);
        if (googleTracker != null && googleTracker.trim().length() > 0) {
            context.put("googleAnalyticsTracker", googleTracker.trim());
        }
        context.put("announcement", SystemGlobals.getValue(ConfigKeys.ANNOUNCEMENT));
	}

	/**
	 * Checks user credential / automatic login.
	 * 
	 * @param userSession The UserSession instance associated to the user's session
	 * @return <code>true</code> if auto login was enabled and the user was successfully 
	 * logged in.
	 * @throws DatabaseException
	 */
	protected boolean checkAutoLogin(final UserSession userSession)
	{
		final String cookieName = SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA);

		final Cookie cookie = this.getCookieTemplate(cookieName);
		final Cookie hashCookie = this.getCookieTemplate(SystemGlobals.getValue(ConfigKeys.COOKIE_USER_HASH));
		final Cookie autoLoginCookie = this.getCookieTemplate(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN));

		if (hashCookie != null && cookie != null
				&& !cookie.getValue().equals(SystemGlobals.getValue(ConfigKeys.ANONYMOUS_USER_ID))
				&& autoLoginCookie != null 
				&& "1".equals(autoLoginCookie.getValue())) {
			final String uid = cookie.getValue();
			final String uidHash = hashCookie.getValue();

			// Load the user-specific security hash from the database
			try {
				final UserDAO userDao = DataAccessDriver.getInstance().newUserDAO();
				final String userHash = userDao.getUserAuthHash(Integer.parseInt(uid));
				
				if (StringUtils.isBlank(userHash)) {
					return false;
				}
				
				final String securityHash = Hash.md5(userHash);
	
				if (securityHash.equals(uidHash)) {
					final int userId = Integer.parseInt(uid);
					userSession.setUserId(userId);
					
					final User user = userDao.selectById(userId);
	
					if (user == null || user.getId() != userId || user.isDeleted()) {
						userSession.makeAnonymous();
						return false;
					}
	
					this.configureUserSession(userSession, user);
					
					return true;
				}
			}
			catch (Exception e) {
				throw new DatabaseException(e);
			}
			
			userSession.makeAnonymous();
		}
		
		return false;
	}

	/**
	 * Setup options and values for the user's session if authentication was OK.
	 * 
	 * @param userSession The UserSession instance of the user
	 * @param user The User instance of the authenticated user
	 */
	protected void configureUserSession(final UserSession userSession, final User user)
	{
		userSession.dataToUser(user);

		// As a user may come back to the forum before its
		// last visit's session expires, we should check for
		// existent user information and then, if found, store
		// it to the database before getting his information back.
		final String sessionId = SessionFacade.isUserInSession(user.getId());

		UserSession tmpUs;
		if (sessionId == null) {
			final UserSessionDAO userSessionDao = DataAccessDriver.getInstance().newUserSessionDAO();
			tmpUs = userSessionDao.selectById(userSession, JForumExecutionContext.getConnection());	
		}
		else {			
			SessionFacade.storeSessionData(sessionId, JForumExecutionContext.getConnection());
			tmpUs = SessionFacade.getUserSession(sessionId);
			SessionFacade.remove(sessionId);
		}

		if (tmpUs == null) {
			userSession.setLastVisit(new Date(System.currentTimeMillis()));
		}
		else {
			// Update last visit and session start time
			userSession.setLastVisit(new Date(tmpUs.getStartTime().getTime() + tmpUs.getSessionTime()));
		}

		// If the execution point gets here, then the user
		// has chosen "autoLogin"
		userSession.setAutoLogin(true);
		SessionFacade.makeLogged();

		I18n.load(user.getLang());
	}

	/**
	 * Checks for user authentication using some SSO implementation
     * @param userSession UserSession
     */
	protected void checkSSO(final UserSession userSession)
	{
		try {
			final SSO sso = (SSO) Class.forName(SystemGlobals.getValue(ConfigKeys.SSO_IMPLEMENTATION)).newInstance();
			final String username = sso.authenticateUser(JForumExecutionContext.getRequest());

			if (username == null || username.trim().equals("")) {
				userSession.makeAnonymous();
			}
			else {
				final SSOUtils utils = new SSOUtils();

				if (!utils.userExists(username)) {
					final SessionContext session = JForumExecutionContext.getRequest().getSessionContext();

					String email = (String) session.getAttribute(SystemGlobals.getValue(ConfigKeys.SSO_EMAIL_ATTRIBUTE));
					String password = (String) session.getAttribute(SystemGlobals.getValue(ConfigKeys.SSO_PASSWORD_ATTRIBUTE));

					if (email == null) {
						email = SystemGlobals.getValue(ConfigKeys.SSO_DEFAULT_EMAIL);
					}

					if (password == null) {
						password = SystemGlobals.getValue(ConfigKeys.SSO_DEFAULT_PASSWORD);
					}

					utils.register(password, email);
				}

				this.configureUserSession(userSession, utils.getUser());
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ForumException("Error while executing SSO actions: " + e);
		}
	}

	/**
	 * Do a refresh in the user's session. This method will update the last visit time for the
	 * current user, as well checking for authentication if the session is new or the SSO user has
	 * changed
	 */
	public void refreshSession()
	{
		UserSession userSession = SessionFacade.getUserSession();
		final RequestContext request = JForumExecutionContext.getRequest();

		if (userSession == null) {
			userSession = new UserSession();
			userSession.registerBasicInfo();
			userSession.setSessionId(request.getSessionContext().getId());
			userSession.setIp(request.getRemoteAddr());
			SessionFacade.makeUnlogged();

			if (!JForumExecutionContext.getForumContext().isBot()) {
				// Non-SSO authentications can use auto login
				if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
					this.checkSSO(userSession);
				}
				else {					
					if (SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED)) {
						this.checkAutoLogin(userSession);
					}
					else {
						userSession.makeAnonymous();
					}
				}
			}

			SessionFacade.add(userSession);
		}
		else if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			SSO sso;
			
			try {
				sso = (SSO) Class.forName(SystemGlobals.getValue(ConfigKeys.SSO_IMPLEMENTATION)).newInstance();
			}
			catch (Exception e) {
				throw new ForumException(e);
			}

			// If SSO, then check if the session is valid
			if (!sso.isSessionValid(userSession, request)) {
				SessionFacade.remove(userSession.getSessionId());
				refreshSession();
			}
		}
		else {
			SessionFacade.getUserSession().updateSessionTime();
		}
	}

	/**
	 * Gets a cookie by its name.
	 * 
	 * @param name The cookie name to retrieve
	 * @return The <code>Cookie</code> object if found, or <code>null</code> otherwise
	 */
	public static Cookie getCookie(final String name)
	{
		final Cookie[] cookies = JForumExecutionContext.getRequest().getCookies();

		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				final Cookie cookie = cookies[i];

				if (cookie.getName().equals(name)) {
					return cookie;
				}
			}
		}

		return null;
	}
	
	/**
	 * Template method to get a cookie.
	 * Useful to situations when a subclass
	 * wants to have a different way to 
	 * retrieve a cookie.
	 * @param name The cookie name to retrieve
	 * @return The Cookie object if found, or null otherwise
	 * @see #getCookie(String)
	 */
	protected Cookie getCookieTemplate(final String name)
	{
		return ControllerUtils.getCookie(name);
	}

	/**
	 * Add or update a cookie. This method adds a cookie, serializing its value using XML.
	 * 
	 * @param name The cookie name.
	 * @param value The cookie value
	 */
	public static void addCookie(final String name, final String value)
	{
		String tmpValue = value;
		int maxAge = 3600 * 24 * 365;
		
		if (value == null) {
			maxAge = 0;
			tmpValue = "";
		}
		
		final Cookie cookie = new Cookie(name, tmpValue);
		cookie.setMaxAge(maxAge);
		String contextPath = SystemGlobals.getValue("context.path");
		if (contextPath.equals("")) {
			cookie.setPath("/");
		} else {
			cookie.setPath(contextPath);
		}

		try {
			String version = SystemGlobals.getValue("servlet.version"); // will be of the form "3.0"
			int majorVersion = Integer.parseInt(version.substring(0, version.indexOf(".")));
			if (majorVersion >= 3) {
				// setHttpOnly was introduced in Servlet API 3.0
				Class<Cookie> cookieClass = javax.servlet.http.Cookie.class;
				Method httpOnlyMethod = cookieClass.getMethod("setHttpOnly", new Class[] {boolean.class});
				httpOnlyMethod.invoke(cookie, new Object[] {Boolean.TRUE});
			}
		} catch (Exception ex) {
			LOGGER.warn("Could not set httpOnly for cookie '"+name+"': "+ex.getMessage());
		}

		JForumExecutionContext.getResponse().addCookie(cookie);
	}
	
	/**
	 * Template method to add a cookie.
	 * Useful to sustain when a subclass wants to add
	 * a cookie in a fashion different than the normal 
	 * behavior
	 * @param name The cookie name
	 * @param value The cookie value
	 * @see #addCookie(String, String)
	 */
	protected void addCookieTemplate(final String name, final String value)
	{
		ControllerUtils.addCookie(name, value);
	}
}
