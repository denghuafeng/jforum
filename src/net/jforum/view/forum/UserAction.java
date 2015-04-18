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
 * Created on May 12, 2003 / 8:31:25 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.jforum.Command;
import net.jforum.ControllerUtils;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.dao.BanlistDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.entities.Banlist;
import net.jforum.entities.Bookmark;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.repository.BanlistRepository;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.RankingRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.security.StopForumSpam;
import net.jforum.util.I18n;
import net.jforum.util.Hash;
import net.jforum.util.concurrent.Executor;
import net.jforum.util.mail.ActivationKeySpammer;
import net.jforum.util.mail.EmailSenderTask;
import net.jforum.util.mail.LostPasswordSpammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.Stats;
import net.jforum.view.forum.common.UserCommon;
import net.jforum.view.forum.common.ViewCommon;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class UserAction extends Command 
{
	private static final Logger LOGGER = Logger.getLogger(UserAction.class);

	private static final String USERNAME = "username";
	private static final String USER_ID = "user_id";
	private static final String PAGE_TITLE = "pageTitle";
	private static final String MESSAGE = "message";
	private static final String EMAIL = "email";

	private final UserDAO userDao = DataAccessDriver.getInstance().newUserDAO();
	private final UserSessionDAO userSessionDao = DataAccessDriver.getInstance().newUserSessionDAO();

	private boolean canEdit()
	{
		final int tmpId = SessionFacade.getUserSession().getUserId();
		final boolean canEdit = SessionFacade.isLogged() && tmpId == this.request.getIntParameter(USER_ID);

		if (!canEdit) {
			this.profile();
		}

		return canEdit;
	}

	public void edit()
	{
		if (this.canEdit()) {
			final int userId = this.request.getIntParameter(USER_ID);
			final User user = userDao.selectById(userId);

			this.context.put("u", user);
			this.context.put("action", "editSave");
			this.context.put(PAGE_TITLE, I18n.getMessage("UserProfile.profileFor") + " " + user.getUsername());
			this.context.put("avatarAllowExternalUrl", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_ALLOW_EXTERNAL_URL));
			this.context.put("avatarPath", SystemGlobals.getValue(ConfigKeys.AVATAR_IMAGE_DIR));
			this.setTemplateName(TemplateKeys.USER_EDIT);
		} 
	}

	public void editDone()
	{
		this.context.put("editDone", true);
		this.edit();
	}

	public void editSave()
	{
		if (this.canEdit()) {
			final int userId = this.request.getIntParameter(USER_ID);
			final List<String> warns = UserCommon.saveUser(userId);

			if (!warns.isEmpty()) {
				this.context.put("warns", warns);
				this.edit();
			} 
			else {
				JForumExecutionContext.setRedirect(this.request.getContextPath()
					+ "/user/editDone/" + userId
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			}
		}
	}

	private void registrationDisabled()
	{
		this.setTemplateName(TemplateKeys.USER_REGISTRATION_DISABLED);
		this.context.put(MESSAGE, I18n.getMessage("User.registrationDisabled"));
	}

	private void insert(final boolean hasErrors)
	{
		final int userId = SessionFacade.getUserSession().getUserId();

		if ((!SystemGlobals.getBoolValue(ConfigKeys.REGISTRATION_ENABLED)
				&& !SecurityRepository.get(userId).canAccess(SecurityConstants.PERM_ADMINISTRATION))
				|| ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			this.registrationDisabled();
			return;
		}

		if (!hasErrors && SystemGlobals.getBoolValue(ConfigKeys.AGREEMENT_SHOW) && !this.agreementAccepted()) {
			this.setTemplateName(TemplateKeys.AGREEMENT_LIST);
			this.context.put("agreementContents", this.agreementContents());
			return;
		}

		this.setTemplateName(TemplateKeys.USER_INSERT);
		this.context.put("action", "insertSave");
		this.context.put(USERNAME, this.request.getParameter(USERNAME));
		this.context.put(EMAIL, this.request.getParameter(EMAIL));
		this.context.put(PAGE_TITLE, I18n.getMessage("ForumBase.register"));

		if (SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_REGISTRATION)){
			this.context.put("captcha_reg", true);
		}

		SessionFacade.removeAttribute(ConfigKeys.AGREEMENT_ACCEPTED);
	}

	public void insert() 
	{
		this.insert(false);
	}

	public void acceptAgreement()
	{
		SessionFacade.setAttribute(ConfigKeys.AGREEMENT_ACCEPTED, "1");
		JForumExecutionContext.setRedirect(this.request.getContextPath()
			+ "/user/insert"
			+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
	}

	private String agreementContents()
	{
		StringBuilder contents = new StringBuilder();

		try {
			String directory = new StringBuilder()
				.append(SystemGlobals.getApplicationPath()) 
				.append(SystemGlobals.getValue(ConfigKeys.AGREEMENT_FILES_PATH)) 
				.append('/')
				.toString();

			String filename = "terms_" + I18n.getUserLanguage() + ".txt";

			File file = new File(directory + filename);

			if (!file.exists()) {
				filename = SystemGlobals.getValue(ConfigKeys.AGREEMENT_DEFAULT_FILE);
				file = new File(directory + filename);

				if (!file.exists()) {
					throw new FileNotFoundException("Could not locate any terms agreement file");
				}
			}

			contents.append(FileUtils.readFileToString(file, SystemGlobals.getValue(ConfigKeys.ENCODING)));
		}
		catch (Exception e) {
			LOGGER.warn("Failed to read agreement data: " + e, e);
			contents = new StringBuilder(I18n.getMessage("User.agreement.noAgreement"));
		}

		return contents.toString();
	}

	private boolean agreementAccepted()
	{
		return "1".equals(SessionFacade.getAttribute(ConfigKeys.AGREEMENT_ACCEPTED));
	}

	public void insertSave()
	{
		UserSession userSession = SessionFacade.getUserSession();
		int userId = userSession.getUserId();

		if ((!SystemGlobals.getBoolValue(ConfigKeys.REGISTRATION_ENABLED)
				&& !SecurityRepository.get(userId).canAccess(SecurityConstants.PERM_ADMINISTRATION))
				|| ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			this.registrationDisabled();
			return;
		}

		User user = new User();

		String username = this.request.getParameter(USERNAME);
		String password = this.request.getParameter("password");
		String email = this.request.getParameter(EMAIL);
		String captchaResponse = this.request.getParameter("captchaResponse");
		String ip = this.request.getRemoteAddr();

		boolean error = false;
		if (StringUtils.isBlank(username)
				|| StringUtils.isBlank(password)) {
			this.context.put("error", I18n.getMessage("UsernamePasswordCannotBeNull"));
			error = true;
		}

		if (username != null) {
			username = username.trim();
		}

        if (!error && username != null && username.length() > SystemGlobals.getIntValue(ConfigKeys.USERNAME_MAX_LENGTH)) {
			this.context.put("error", I18n.getMessage("User.usernameTooBig"));
			error = true;
		}

		if (!error && username != null && (username.indexOf('<') > -1 || username.indexOf('>') > -1)) {
			this.context.put("error", I18n.getMessage("User.usernameInvalidChars"));
			error = true;
		}

		if (!error && userDao.isUsernameRegistered(username)) {
			this.context.put("error", I18n.getMessage("UsernameExists"));
			error = true;
		}

		if (!error && userDao.findByEmail(email) != null) {
			this.context.put("error", I18n.getMessage("User.emailExists", new String[] { email }));
			error = true;
		}

		if (!error && !userSession.validateCaptchaResponse(captchaResponse)){
			this.context.put("error", I18n.getMessage("CaptchaResponseFails"));
			error = true;
		}

		final BanlistDAO banlistDao = DataAccessDriver.getInstance().newBanlistDAO();
		boolean stopForumSpamEnabled = SystemGlobals.getBoolValue(ConfigKeys.STOPFORUMSPAM_API_ENABLED);
		if (stopForumSpamEnabled && StopForumSpam.checkIp(ip)) {
			LOGGER.info("Forum Spam found! Block it: " + ip);
			final Banlist banlist = new Banlist();
			banlist.setIp(ip);
			if (!BanlistRepository.shouldBan(banlist)) {
				banlistDao.insert(banlist);
				BanlistRepository.add(banlist);
			}
			error = true;
		} else if (stopForumSpamEnabled && StopForumSpam.checkEmail(email)) {
			LOGGER.info("Forum Spam found! Block it: " + email);
			final Banlist banlist = new Banlist();
			banlist.setEmail(email);
			if (!BanlistRepository.shouldBan(banlist)) {
				banlistDao.insert(banlist);
				BanlistRepository.add(banlist);
			} else { // email already exists, block source ip now
				LOGGER.info("Forum Spam found! Block it: " + ip);
				final Banlist banlist2 = new Banlist();
				banlist2.setIp(ip);
				banlistDao.insert(banlist2);
				BanlistRepository.add(banlist2);
			}
			error = true;
		}

		if (error) {
			this.insert(true);
			return;
		}

		user.setUsername(username);
		user.setPassword(Hash.sha512(password+SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE)));
		user.setEmail(email);

		boolean needMailActivation = SystemGlobals.getBoolValue(ConfigKeys.MAIL_USER_EMAIL_AUTH);

		if (needMailActivation) {
			user.setActivationKey(Hash.md5(username + System.currentTimeMillis() + SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE) + new Random().nextInt(999999)));
		}

		int newUserId = userDao.addNew(user);

		if (needMailActivation) {
			Executor.execute(new EmailSenderTask(new ActivationKeySpammer(user)));

			this.setTemplateName(TemplateKeys.USER_INSERT_ACTIVATE_MAIL);
			this.context.put(MESSAGE, I18n.getMessage("User.GoActivateAccountMessage"));
		} 
		else if(SecurityRepository.get(userId).canAccess(SecurityConstants.PERM_ADMINISTRATION)) {
			JForumExecutionContext.setRedirect(this.request.getContextPath()
				+ "/adminUsers/list"
				+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
		}
		else {
			this.logNewRegisteredUserIn(newUserId, user);
		}

		if (!needMailActivation) {
			userDao.writeUserActive(newUserId);
		}
	}

	public void activateAccount()
	{
		String hash = this.request.getParameter("hash");
		int userId = Integer.parseInt(this.request.getParameter(USER_ID));

		User user = userDao.selectById(userId);

		boolean isValid = userDao.validateActivationKeyHash(userId, hash);

		if (isValid) {
			// Activate the account
			userDao.writeUserActive(userId);
			this.logNewRegisteredUserIn(userId, user);
		} 
		else {
			this.setTemplateName(TemplateKeys.USER_INVALID_ACTIVATION);
			this.context.put(MESSAGE, I18n.getMessage("User.invalidActivationKey", 
				new Object[] { this.request.getContextPath()
					+ "/user/activateManual"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) 
				}
			));
		}
	}

	public void activateManual()
	{
		this.setTemplateName(TemplateKeys.ACTIVATE_ACCOUNT_MANUAL);
	}

	private void logNewRegisteredUserIn(final int userId, final User user) 
	{
		UserSession userSession = SessionFacade.getUserSession();
		SessionFacade.remove(userSession.getSessionId());
		userSession.setAutoLogin(true);
		userSession.setUserId(userId);
		userSession.setUsername(user.getUsername());
		userSession.setLastVisit(new Date(System.currentTimeMillis()));
		userSession.setStartTime(new Date(System.currentTimeMillis()));
		SessionFacade.makeLogged();

		SessionFacade.add(userSession);

		// Finalizing.. show the user the congratulations page
		JForumExecutionContext.setRedirect(this.request.getContextPath()
			+ "/user/registrationComplete"
			+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
	}

	public void registrationComplete()
	{
		int userId = SessionFacade.getUserSession().getUserId();

		// prevent increment total users through directly type in url
		if (userId <= ForumRepository.lastRegisteredUser().getId()) {
			JForumExecutionContext.setRedirect(this.request.getContextPath()
					+ "/forums/list"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			return;
		}

		ForumRepository.setLastRegisteredUser(userDao.selectById(userId));
		ForumRepository.incrementTotalUsers();

		String profilePage = JForumExecutionContext.getForumContext().encodeURL("/user/edit/" + userId);
		String homePage = JForumExecutionContext.getForumContext().encodeURL("/forums/list");

		String message = I18n.getMessage("User.RegistrationCompleteMessage", 
				new Object[] { profilePage, homePage });
		this.context.put(MESSAGE, message);
		this.setTemplateName(TemplateKeys.USER_REGISTRATION_COMPLETE);
	}

	public void validateLogin()
	{
		String password;
		String username;

		if (parseBasicAuthentication()) {
			username = (String)this.request.getAttribute(USERNAME);
			password = (String)this.request.getAttribute("password");
		} 
		else {
			username = this.request.getParameter(USERNAME);
			password = this.request.getParameter("password");
		}

		boolean validInfo = false;

		if (password.length() > 0) {
			User user = this.validateLogin(username, password);

			if (user != null) {
				// Note: here we only want to set the redirect location if it hasn't already been
				// set. This will give the LoginAuthenticator a chance to set the redirect location.
				this.buildSucessfulLoginRedirect();

				SessionFacade.makeLogged();

				String sessionId = SessionFacade.isUserInSession(user.getId());
				UserSession userSession = new UserSession(SessionFacade.getUserSession());

				// Remove the "guest" session
				SessionFacade.remove(userSession.getSessionId());

				userSession.dataToUser(user);

				UserSession currentUs = SessionFacade.getUserSession(sessionId);

				// Check if the user is returning to the system
				// before its last session has expired ( hypothesis )
                UserSession tmpUs;
				if (sessionId != null && currentUs != null) {
					// Write its old session data
					SessionFacade.storeSessionData(sessionId, JForumExecutionContext.getConnection());
					tmpUs = new UserSession(currentUs);
					SessionFacade.remove(sessionId);
				}
				else {
					tmpUs = userSessionDao.selectById(userSession, JForumExecutionContext.getConnection());
				}

				I18n.load(user.getLang());

				// Autologin
				if (this.request.getParameter("autologin") != null
						&& SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED)) {
					userSession.setAutoLogin(true);

					// Generate the user-specific hash
					String systemHash = Hash.md5(SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE) + user.getId());
					String userHash = Hash.md5(System.currentTimeMillis() + systemHash);

					// Persist the user hash
					userDao.saveUserAuthHash(user.getId(), userHash);

					systemHash = Hash.md5(userHash);

					ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN), "1");
					ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_USER_HASH), systemHash);
				}
				else {
					// Remove cookies for safety
					ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_USER_HASH), null);
					ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN), null);
				}

				if (tmpUs == null) {
					userSession.setLastVisit(new Date(System.currentTimeMillis()));
				}
				else {
					// Update last visit and session start time
					userSession.setLastVisit(new Date(tmpUs.getStartTime().getTime() + tmpUs.getSessionTime()));
				}

				SessionFacade.add(userSession);
				SessionFacade.setAttribute(ConfigKeys.TOPICS_READ_TIME, new HashMap<Integer, Long>());
				ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA), 
					Integer.toString(user.getId()));

				SecurityRepository.load(user.getId(), true);
				validInfo = true;
			}
		}

		// Invalid login
		if (!validInfo) {
			this.context.put("invalidLogin", "1");
			this.setTemplateName(TemplateKeys.USER_VALIDATE_LOGIN);

			if (isValidReturnPath()) {
				this.context.put("returnPath", this.request.getParameter("returnPath"));
			}
		} 
		else if (isValidReturnPath()) {
			JForumExecutionContext.setRedirect(this.request.getParameter("returnPath"));
		}
	}

	private void buildSucessfulLoginRedirect()
	{
		if (JForumExecutionContext.getRedirectTo() == null) {
			String forwaredHost = request.getHeader("X-Forwarded-Host");

			if (forwaredHost == null 
					|| SystemGlobals.getBoolValue(ConfigKeys.LOGIN_IGNORE_XFORWARDEDHOST)) {
				JForumExecutionContext.setRedirect(this.request.getContextPath()
					+ "/forums/list"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
			}
			else {
				JForumExecutionContext.setRedirect(this.request.getScheme()
					+ "://"
					+ forwaredHost
					+ this.request.getContextPath()
					+ "/forums/list"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION)); 
			}
		}
	}

    public void validateLogin(final RequestContext request)  {
        this.request = request;
        validateLogin();
    }

    public static boolean hasBasicAuthentication(final RequestContext request) {
        String auth = request.getHeader("Authorization");
        return (auth != null && auth.startsWith("Basic "));
    }

    private boolean parseBasicAuthentication()
	{
		if (hasBasicAuthentication(request)) {
			String auth = request.getHeader("Authorization");
			String decoded;

			decoded = String.valueOf(new Base64().decode(auth.substring(6)));

			int p = decoded.indexOf(':');

			if (p != -1) {
				request.setAttribute(USERNAME, decoded.substring(0, p));
				request.setAttribute("password", decoded.substring(p + 1));
				return true;
			}
		}
		return false;
	}

    private User validateLogin(final String name, final String password)
	{
        return userDao.validateLogin(name, password);
	}

	public void profile()
	{
		DataAccessDriver da = DataAccessDriver.getInstance();

		User user = userDao.selectById(this.request.getIntParameter(USER_ID));

		if (user.getId() == 0) {
			this.userNotFound();
		}
		else {
			this.setTemplateName(TemplateKeys.USER_PROFILE);
			this.context.put("karmaEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_KARMA_ENABLED));
			this.context.put("rank", new RankingRepository());
			this.context.put("u", user);
			this.context.put("avatarAllowExternalUrl", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_ALLOW_EXTERNAL_URL));
			this.context.put("avatarPath", SystemGlobals.getValue(ConfigKeys.AVATAR_IMAGE_DIR));
			this.context.put("showAvatar", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_SHOW));
			this.context.put("showKarma", SystemGlobals.getBoolValue(ConfigKeys.KARMA_SHOW));

			int loggedId = SessionFacade.getUserSession().getUserId();
			int count = 0;

			List<Bookmark> bookmarks = da.newBookmarkDAO().selectByUser(user.getId());
			for (Iterator<Bookmark> iter = bookmarks.iterator(); iter.hasNext(); ) {
				Bookmark bookmark = iter.next();

				if (bookmark.isPublicVisible() || loggedId == user.getId()) {
					count++;
				}
			}

			this.context.put(PAGE_TITLE, I18n.getMessage("UserProfile.allAbout")+" "+user.getUsername());
			this.context.put("nbookmarks", Integer.valueOf(count));
			this.context.put("ntopics", Integer.valueOf(da.newTopicDAO().countUserTopics(user.getId())));
			this.context.put("nposts", Integer.valueOf(da.newPostDAO().countUserPosts(user.getId())));
            this.context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));

			Stats.record("User profile page", request.getRequestURL());
		}
	}

	private void userNotFound()
	{
		this.context.put(MESSAGE, I18n.getMessage("User.notFound"));
		this.setTemplateName(TemplateKeys.USER_NOT_FOUND);
	}

	public void logout()
	{
		JForumExecutionContext.setRedirect(this.request.getContextPath()
			+ "/forums/list"
			+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));

		UserSession userSession = SessionFacade.getUserSession();
		SessionFacade.storeSessionData(userSession.getSessionId(), JForumExecutionContext.getConnection());

		SessionFacade.makeUnlogged();
		SessionFacade.remove(userSession.getSessionId());

		// Disable auto login
		userSession.setAutoLogin(false);
		userSession.makeAnonymous();

		SessionFacade.add(userSession);
	}

	public void login()
	{
		if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			this.registrationDisabled();
			return;
		}

		if (isValidReturnPath()) {
			this.context.put("returnPath", this.request.getParameter("returnPath"));
		}
		else if (!SystemGlobals.getBoolValue(ConfigKeys.LOGIN_IGNORE_REFERER)) {
			String referer = this.request.getHeader("Referer");

			if (referer != null) {
				this.context.put("returnPath", referer);
			}
		}

		this.context.put(PAGE_TITLE, I18n.getMessage("ForumBase.login"));
		this.setTemplateName(TemplateKeys.USER_LOGIN);
	}

	// Lost password form
	public void lostPassword() 
	{
		this.setTemplateName(TemplateKeys.USER_LOSTPASSWORD);
		this.context.put(PAGE_TITLE, I18n.getMessage("PasswordRecovery.title"));
	}

	public User prepareLostPassword(String origUsername, final String email)
	{
		String username = origUsername;
		User user = null;

		if (email != null && !email.trim().equals("")) {
			username = userDao.getUsernameByEmail(email);
		}

		if (username != null && !username.trim().equals("")) {
			List<User> l = userDao.findByName(username, true);
			if (!l.isEmpty()) {
				user = l.get(0);
			}
		}

		if (user == null) {
			return null;
		}

		String hash = Hash.md5(user.getEmail() 
				+ System.currentTimeMillis() 
				+ SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE) 
				+ new Random().nextInt(999999));
		userDao.writeLostPasswordHash(user.getEmail(), hash);

		user.setActivationKey(hash);

		return user;
	}

	// Send lost password email
	public void lostPasswordSend()
	{
		String email = this.request.getParameter(EMAIL);
		String username = this.request.getParameter(USERNAME);

		User user = this.prepareLostPassword(username, email);
		if (user == null) {
			// user could not be found
			this.context.put(MESSAGE,
					I18n.getMessage("PasswordRecovery.invalidUserEmail"));
			this.lostPassword();
			return;
		}

		Executor.execute(new EmailSenderTask(
				new LostPasswordSpammer(user, 
					SystemGlobals.getValue(ConfigKeys.MAIL_LOST_PASSWORD_SUBJECT))));

		this.setTemplateName(TemplateKeys.USER_LOSTPASSWORD_SEND);
		this.context.put(MESSAGE, I18n.getMessage(
			"PasswordRecovery.emailSent",
			new String[] { 
					this.request.getContextPath()
					+ "/user/login"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) 
				}));
	}

	// Recover user password ( aka, ask him a new one )
	public void recoverPassword()
	{
		String hash = this.request.getParameter("hash");

		this.setTemplateName(TemplateKeys.USER_RECOVERPASSWORD);
		this.context.put("recoverHash", hash);
	}

	public void recoverPasswordValidate()
	{
		String hash = this.request.getParameter("recoverHash");
		String email = this.request.getParameter(EMAIL);

		String message;
		boolean isOk = userDao.validateLostPasswordHash(email, hash);

		if (isOk) {
			String password = this.request.getParameter("newPassword");
			userDao.saveNewPassword(Hash.sha512(password+SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE)), email);

			message = I18n.getMessage("PasswordRecovery.ok",
				new String[] { this.request.getContextPath()
					+ "/user/login"
					+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION) });
		} 
		else {
			message = I18n.getMessage("PasswordRecovery.invalidData");
		}

		this.setTemplateName(TemplateKeys.USER_RECOVERPASSWORD_VALIDATE);
		this.context.put(MESSAGE, message);
	}

	public void list()
	{
		int start = this.preparePagination(userDao.getTotalUsers());
		int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

		List<User> users = userDao.selectAll(start ,usersPerPage);
		this.context.put("users", users);
		this.context.put(PAGE_TITLE, I18n.getMessage("ForumBase.usersList"));
		this.setTemplateName(TemplateKeys.USER_LIST);
	}

	public void listGroup()
	{
		int groupId = this.request.getIntParameter("group_id");

		int start = this.preparePagination(userDao.getTotalUsersByGroup(groupId));
		int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

		List<User> users = userDao.selectAllByGroup(groupId, start ,usersPerPage);

		this.context.put("users", users);
		this.setTemplateName(TemplateKeys.USER_LIST);
	}

	/**
	 * @deprecated probably will be removed. Use KarmaAction to load Karma
	 */
	public void searchKarma() 
	{
		int start = this.preparePagination(userDao.getTotalUsers());
		int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

		//Load all users with your karma
		List<User> users = userDao.selectAllWithKarma(start ,usersPerPage);
		this.context.put("users", users);
		this.setTemplateName(TemplateKeys.USER_SEARCH_KARMA);
	}

	private int preparePagination(int totalUsers)
	{
		int start = ViewCommon.getStartPage();
		int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

		ViewCommon.contextToPagination(start, totalUsers, usersPerPage);

		return start;
	}

	private boolean isValidReturnPath() {
		if (request.getParameter("returnPath") != null) {
			return request.getParameter("returnPath").startsWith(SystemGlobals.getValue(ConfigKeys.FORUM_LINK));
		} else {
			return false;
		}
	}
}
