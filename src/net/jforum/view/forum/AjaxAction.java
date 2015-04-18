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
 * Created on 09/08/2007 09:31:17
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.PostDAO;
import net.jforum.entities.ModerationLog;
import net.jforum.entities.Post;
import net.jforum.entities.User;
import net.jforum.repository.PostRepository;
import net.jforum.search.LuceneManager;
import net.jforum.search.SearchFacade;
import net.jforum.util.SafeHtml;
import net.jforum.util.mail.Spammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.PostCommon;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class AjaxAction extends Command
{
	private static final Logger LOGGER = Logger.getLogger(AjaxAction.class);

	/**
	 * Sends a test message and set the status message in context
	 *  sender The sender's email address
	 *  host the smtp host
	 *  auth if need authorization or not
	 *  username the smtp server username, if auth is needed
	 *  password the smtp server password, if auth is needed
	 *  to the recipient
	 */
	public void sendTestMail()
	{
		String sender = this.request.getParameter("sender");
		String host = this.request.getParameter("host");
		String port = this.request.getParameter("port");
		String auth = this.request.getParameter("auth");
		String ssl = this.request.getParameter("ssl");
		String username = this.request.getParameter("username");
		String password = this.request.getParameter("password");
		String to = this.request.getParameter("to");

		// Save the current values
		String originalHost = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_HOST);
		String originalAuth = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_AUTH);
		String originalUsername = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_USERNAME);
		String originalPassword = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PASSWORD);
		String originalSender = SystemGlobals.getValue(ConfigKeys.MAIL_SENDER);
		String originalSSL = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_SSL);
		String originalPort = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PORT);

		// Now put the new ones
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, host);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, auth);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, username);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, password);
		SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, sender);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, ssl);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, port);

		String status = "OK";

		// Send the test mail
		class TestSpammer extends Spammer {
			public TestSpammer(String to) {
				List<User> l = new ArrayList<User>();

				User user = new User();
				user.setEmail(to);

				l.add(user);

				this.setUsers(l);

				this.setTemplateParams(JForumExecutionContext.newSimpleHash());
				this.prepareMessage("JForum Test Mail", null);
			}

			protected String processTemplate() {
				return ("Test mail from JForum Admin Panel. Sent at " + new Date());
			}

			protected void createTemplate(String messageFile) throws IOException {}
		}

		Spammer s = new TestSpammer(to);

		try {
			s.dispatchMessages();
		}
		catch (Exception e) {
			status = StringEscapeUtils.escapeEcmaScript(e.toString());
			LOGGER.error(e.toString(), e);
		}
		finally {
			// Restore the original values
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, originalHost);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, originalAuth);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, originalUsername);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, originalPassword);
			SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, originalSender);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, originalSSL);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, originalPort);
		}

		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", status);
	}

	public void isPostIndexed()
	{
		int postId = this.request.getIntParameter("post_id");

		this.setTemplateName(TemplateKeys.AJAX_IS_POST_INDEXED);

		LuceneManager manager = (LuceneManager)SearchFacade.manager();
		Document doc = manager.luceneSearch().findDocumentByPostId(postId);

		this.context.put("doc", doc);
	}

	public void loadPostContents()
	{
		// edit_area has id begin with 'x' to be compliant to XHTML, so we need to remove it.
		String id = this.request.getParameter("id");
		if (id == null || "".equals(id)) return;
		int postId = Integer.parseInt(id.substring(1));
		PostDAO dao = DataAccessDriver.getInstance().newPostDAO();
		Post post = dao.selectById(postId);
		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}

	public void savePost()
	{
		// edit_area has id begin with 'x' to be compliant to XHTML, so we need to remove it.
		String id = this.request.getParameter("id");
		if (id == null || "".equals(id)) return;
		int postId = Integer.parseInt(id.substring(1));
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		Post post = postDao.selectById(postId);

		String originalMessage = post.getText();

		if (PostCommon.canEditPost(post)) {
			post.setText(this.request.getParameter("value"));
			postDao.update(post);
			SearchFacade.update(post);
		}
		//post = PostCommon.preparePostForDisplay(post);

		boolean isModerator = SessionFacade.getUserSession().isModerator(post.getForumId());

		if (SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED)
				&& isModerator && post.getUserId() != SessionFacade.getUserSession().getUserId()) {
			ModerationHelper helper = new ModerationHelper();

			this.request.addParameter("log_original_message", originalMessage);
			this.request.addParameter("post_id", String.valueOf(post.getId()));
			this.request.addParameter("topic_id", String.valueOf(post.getTopicId()));

			ModerationLog log = helper.buildModerationLogFromRequest();
			log.getPosterUser().setId(post.getUserId());

			helper.saveModerationLog(log);
		}

		if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
			PostRepository.update(post.getTopicId(), PostCommon.preparePostForDisplay(post));
		} else {
			post = PostCommon.preparePostForDisplay(post);
		}

		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}

	public void previewPost()
	{
		Post post = new Post();

		post.setText(this.request.getParameter("text"));
		post.setSubject(this.request.getParameter("subject"));
		post.setHtmlEnabled("true".equals(this.request.getParameter("html")));
		post.setBbCodeEnabled("true".equals(this.request.getParameter("bbcode")));
		post.setSmiliesEnabled("true".equals(this.request.getParameter("smilies")));

		if (post.isHtmlEnabled()) {
			post.setText(new SafeHtml().makeSafe(post.getText()));
		}

		post = PostCommon.preparePostForDisplay(post);
		post.setSubject(StringEscapeUtils.escapeEcmaScript(post.getSubject()));
		post.setText(StringEscapeUtils.escapeEcmaScript(post.getText()));

		this.setTemplateName(TemplateKeys.AJAX_PREVIEW_POST);
		this.context.put("post", post);
	}

	/**
	 * @see net.jforum.Command#list()
	 */
	public void list()
	{
		this.enableIgnoreAction();
	}
}
