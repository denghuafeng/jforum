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
 * Created on 30/12/2003 / 21:40:54
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.entities;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

import net.jforum.ControllerUtils;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.Captcha;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import com.octo.captcha.image.ImageCaptcha;

/**
 * Stores information about user's session.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class UserSession implements Serializable
{
	private static final long serialVersionUID = 0;
	
	private long sessionTime;
	
	private int userId;
	private int privateMessages;
	
	private Date startTime;
	private Date lastVisit;
	
	private String sessionId;
	private String username;
	private String lang;
	private String ip;

	private boolean autoLogin;
	private boolean viewOnline;
	
	private transient ImageCaptcha imageCaptcha = null;

	public UserSession() {
		// Empty Constructor
	}

	public UserSession(final UserSession userSession)
	{
		if (userSession.getStartTime() != null) {
			this.startTime = new Date(userSession.getStartTime().getTime());
		}

		if (userSession.getLastVisit() != null) {
			this.lastVisit = new Date(userSession.getLastVisit().getTime());
		}
		
		this.sessionTime = userSession.getSessionTime();
		this.userId = userSession.getUserId();
		this.sessionId = userSession.getSessionId();
		this.username = userSession.getUsername();
		this.autoLogin = userSession.isAutoLogin();
		this.viewOnline = userSession.isViewOnline();
		this.lang = userSession.getLang();
		this.privateMessages = userSession.getPrivateMessages();
		this.imageCaptcha = userSession.imageCaptcha;
		this.ip = userSession.getIp();
	}
	
	public Date sessionLastUpdate()
	{
		return new Date(this.startTime.getTime() + this.sessionTime);
	}
	
	public void setIp(final String ip)
	{
		this.ip = ip;
	}
	
	public String getIp()
	{
		return this.ip;
	}

	/**
	 * Set session's start time.
	 * 
	 * @param startTime  Start time in milliseconds
	 */
	public void setStartTime(final Date startTime)
	{
		this.startTime = startTime;
	}

	/**
	 * @return Returns the privateMessages.
	 */
	public int getPrivateMessages()
	{
		return this.privateMessages;
	}

	/**
	 * @param privateMessages The privateMessages to set.
	 */
	public void setPrivateMessages(final int privateMessages)
	{
		this.privateMessages = privateMessages;
	}

	/**
	 * Set session last visit time.
	 * 
	 * @param lastVisit Time in milliseconds
	 */
	public void setLastVisit(final Date lastVisit)
	{
		this.lastVisit = lastVisit;
	}

	/**
	 * Set user's id
	 * 
	 * @param userId The user id
	 */
	public void setUserId(final int userId)
	{
		this.userId = userId;
	}

	/**
	 * Set user's name
	 * 
	 * @param username The username
	 */
	public void setUsername(final String username)
	{
		this.username = username;
	}

	public void setSessionId(final String sessionId)
	{
		this.sessionId = sessionId;
	}

	public void setSessionTime(final long sessionTime)
	{
		this.sessionTime = sessionTime;
	}

	public void setLang(final String lang)
	{
		this.lang = lang;
	}

	/**
	 * Update the session time.
	 */
	public void updateSessionTime()
	{
		this.sessionTime = System.currentTimeMillis() - this.startTime.getTime();
	}

	/**
	 * Enable or disable auto-login.
	 * 
	 * @param autoLogin  <code>true</code> or <code>false</code> to represent auto-login status
	 */
	public void setAutoLogin(boolean autoLogin)
	{
		this.autoLogin = autoLogin;
	}

	/**
	 * Enable or disable the show-online status
	 * 
	 * @param viewOnline  <code>true</code> or <code>false</code> to represent show-onlinestatus
	 */
	public void setViewOnline(boolean viewOnline)
	{
		this.viewOnline = viewOnline;
	}

	/**
	 * Gets user's session start time
	 * 
	 * @return Start time in milliseconds
	 */
	public Date getStartTime()
	{
		return this.startTime;
	}

	public String getLang()
	{
		return this.lang;
	}

	/**
	 * Gets user's last visit time
	 * 
	 * @return Time in milliseconds
	 */
	public Date getLastVisit()
	{
		return this.lastVisit;
	}

	/**
	 * Gets the session time.
	 * 
	 * @return The session time
	 */
	public long getSessionTime()
	{
		return this.sessionTime;
	}

	/**
	 * Gets user's id
	 * 
	 * @return The user id
	 */
	public int getUserId()
	{
		return this.userId;
	}

	/**
	 * Gets the username
	 * 
	 * @return The username
	 */
	public String getUsername()
	{
		if (this.username == null && this.userId == SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)) {
			this.username = I18n.getMessage("Guest");
		}
		
		return this.username;
	}

	/**
	 * Gets auto-login status
	 * 
	 * @return <code>true</code> if auto-login is enabled, or <code>false</code> if disabled.
	 */
	public boolean isAutoLogin()
	{
		return this.autoLogin;
	}

	/**
	 * Gets view-online status
	 * 
	 * @return <code>true</code> if view-online is enabled, or <code>false</code> if disabled.
	 */
	public boolean isViewOnline()
	{
		return this.viewOnline;
	}

	/**
	 * Gets the session id related to this user session
	 * 
	 * @return A string with the session id
	 */
	public String getSessionId()
	{
		return this.sessionId;
	}

	/**
	 * Checks if the user is an administrator
	 * 
	 * @return <code>true</code> if the user is an administrator
	 */
	public boolean isAdmin()
	{
		return SecurityRepository.canAccess(this.userId, SecurityConstants.PERM_ADMINISTRATION);
	}

	/**
	 * Checks if the user is a moderator
	 * 
	 * @return <code>true</code> if the user has moderations rights
	 */
	public boolean isModerator()
	{
		return SecurityRepository.canAccess(this.userId, SecurityConstants.PERM_MODERATION);
	}
	
	/**
	 * Checks if the user can moderate a forum
	 * 
	 * @param forumId the forum's id to check for moderation rights
	 * @return <code>true</code> if the user has moderations rights
	 */
	public boolean isModerator(int forumId)
	{
		final PermissionControl permissionControl = SecurityRepository.get(this.userId);
		
		return (permissionControl.canAccess(SecurityConstants.PERM_MODERATION))
			&& (permissionControl.canAccess(SecurityConstants.PERM_MODERATION_FORUMS, 
				Integer.toString(forumId)));
	}

	/**
	 * Makes the user's session "anonymous" - eg, the user. This method sets the session's start and
	 * last visit time to the current datetime, the user id to the return of a call to
	 * <code>SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)</code> and finally sets
	 * session attribute named "logged" to "0" will be considered a non-authenticated / anonymous
	 * user
	 */
	public void makeAnonymous()
	{
		this.registerBasicInfo();
		
		ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN), null);
		ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA),
			SystemGlobals.getValue(ConfigKeys.ANONYMOUS_USER_ID));

		SessionFacade.makeUnlogged();
	}

	/**
	 * Sets the startup and last visit time to now, as well set the
	 * user id to Anonymous. This method is usually called when the
	 * user hits the forum for the first time. 
	 */
	public void registerBasicInfo()
	{
		this.setStartTime(new Date(System.currentTimeMillis()));
		this.setLastVisit(new Date(System.currentTimeMillis()));
		this.setUserId(SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
		this.setUsername(I18n.getMessage("Guest"));
	}

	/**
	 * Sets a new user session information using information from a <code>User</code> instance.
	 * This method sets the user id, username, the number of private messages, the session's start
	 * time ( set to the current date and time ) and the language.
	 * 
	 * @param user The <code>User</code> instance to get data from
	 */
	public void dataToUser(User user)
	{
		this.setUserId(user.getId());
		this.setUsername(user.getUsername());
		this.setPrivateMessages(user.getPrivateMessagesCount());
		this.setStartTime(new Date(System.currentTimeMillis()));
		this.setLang(user.getLang());
		this.setViewOnline(user.isViewOnlineEnabled());
	}

	/**
	 * Get the captcha image to challenge the user
	 * 
	 * @return BufferedImage the captcha image to challenge the user
	 */
	public BufferedImage getCaptchaImage()
	{
		if (this.imageCaptcha == null) {
			return null;
		}
		
		return this.imageCaptcha.getImageChallenge();
	}

	/**
	 * Validate the captcha response of user
	 * 
	 * @param origUserResponse String the captcha response from user
	 * @return boolean true if the answer is valid, otherwise return false
	 */
	public boolean validateCaptchaResponse(String origUserResponse)
	{
		String userResponse = origUserResponse;
		if ((SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_REGISTRATION) 
				|| SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS))
				&& this.imageCaptcha != null) {
			
			if (SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_IGNORE_CASE)) {
				userResponse = userResponse.toLowerCase(Locale.US);
			}
			
			final boolean result =  this.imageCaptcha.validateResponse(userResponse).booleanValue();
			this.destroyCaptcha();
			return result;
		}
		
		return false;
	}

	/**
	 * create a new image captcha
	 * 
	 */
	public void createNewCaptcha()
	{
		this.destroyCaptcha();
		this.imageCaptcha = Captcha.getInstance().getNextImageCaptcha();
	}

	/**
	 * Destroy the current captcha validation is done
	 * 
	 */
	public void destroyCaptcha()
	{
		if (this.imageCaptcha != null) {
			this.imageCaptcha.disposeChallenge();
		}
	}
	
	/**
     * use JForumExecutionContext.getForumContext().isBot() instead
     *
     *
	 * Checks if it's a bot
	 * @return <code>true</code> if this user session is from any robot
	 */
	public boolean isBot()
	{
        return JForumExecutionContext.getForumContext().isBot();
    }
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(final Object obj)
	{
		if (!(obj instanceof UserSession)) {
			return false;
		}
		
		return this.sessionId.equals(((UserSession)obj).getSessionId());
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return this.sessionId.hashCode();
	}
}
