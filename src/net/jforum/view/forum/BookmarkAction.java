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
 * Created on Jan 16, 2005 4:48:39 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.util.*;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.BookmarkDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Bookmark;
import net.jforum.entities.BookmarkType;
import net.jforum.entities.Forum;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.view.forum.common.Stats;
import net.jforum.util.I18n;
import net.jforum.util.SafeHtml;
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
public class BookmarkAction extends Command
{
	public void insert()
	{
		int type = this.request.getIntParameter("relation_type");
		if (type == BookmarkType.FORUM) {
			this.addForum();
		}
		else if (type == BookmarkType.TOPIC) {
			this.addTopic();
		}
		else if (type == BookmarkType.USER) {
			this.addUser();
		}
		else {
			this.error("Bookmarks.invalidType");
		}
	}

	private void addForum()
	{
		Forum forum = ForumRepository.getForum(this.request.getIntParameter("relation_id"));
		String title = forum.getName();
		String description = forum.getDescription();

		Bookmark bookmark = DataAccessDriver.getInstance().newBookmarkDAO().selectForUpdate(
				forum.getId(), BookmarkType.FORUM, SessionFacade.getUserSession().getUserId());
		if (bookmark != null) {
			if (bookmark.getTitle() != null) {
				title = bookmark.getTitle();
			}

			if (bookmark.getDescription() != null) {
				description = bookmark.getDescription();
			}

			this.context.put("bookmark", bookmark);
		}

		this.setTemplateName(TemplateKeys.BOOKMARKS_ADD_FORUM);
		this.context.put("title", title);
		this.context.put("description", description);
		this.context.put("relationType", Integer.valueOf(BookmarkType.FORUM));
		this.context.put("relationId", Integer.valueOf(forum.getId()));
	}

	private void addTopic()
	{
		Topic topic = DataAccessDriver.getInstance().newTopicDAO().selectById(
				this.request.getIntParameter("relation_id"));
		String title = topic.getTitle();

		Bookmark bookmark = DataAccessDriver.getInstance().newBookmarkDAO().selectForUpdate(
				topic.getId(), BookmarkType.TOPIC, SessionFacade.getUserSession().getUserId());
		if (bookmark != null) {
			if (bookmark.getTitle() != null) {
				title = bookmark.getTitle();
			}

			this.context.put("description", bookmark.getDescription());
			this.context.put("bookmark", bookmark);
		}

		this.setTemplateName(TemplateKeys.BOOKMARKS_ADD_TOPIC);
		this.context.put("title", title);
		this.context.put("relationType", Integer.valueOf(BookmarkType.TOPIC));
		this.context.put("relationId", Integer.valueOf(topic.getId()));
	}

	private void addUser()
	{
		User user = DataAccessDriver.getInstance().newUserDAO().selectById(
				this.request.getIntParameter("relation_id"));
		String title = user.getUsername();

		Bookmark bookmark = DataAccessDriver.getInstance().newBookmarkDAO().selectForUpdate(
				user.getId(), BookmarkType.USER, SessionFacade.getUserSession().getUserId());
		if (bookmark != null) {
			if (bookmark.getTitle() != null) {
				title = bookmark.getTitle();
			}

			this.context.put("description", bookmark.getDescription());
			this.context.put("bookmark", bookmark);
		}

		this.setTemplateName(TemplateKeys.BOOKMARKS_ADD_USER);
		this.context.put("title", title);
		this.context.put("relationType", Integer.valueOf(BookmarkType.USER));
		this.context.put("relationId", Integer.valueOf(user.getId()));
	}

	public void insertSave()
	{
		Bookmark bookmark = new Bookmark();
		final SafeHtml safeHtml = new SafeHtml();

		bookmark.setDescription(safeHtml.makeSafe(this.request.getParameter("description")));
		bookmark.setTitle(safeHtml.makeSafe(this.request.getParameter("title")));

		String publicVisible = this.request.getParameter("visible");
		bookmark.setPublicVisible(publicVisible != null && publicVisible.length() > 0);

		bookmark.setRelationId(this.request.getIntParameter("relation_id"));
		bookmark.setRelationType(this.request.getIntParameter("relation_type"));
		bookmark.setUserId(SessionFacade.getUserSession().getUserId());

		DataAccessDriver.getInstance().newBookmarkDAO().add(bookmark);
		this.setTemplateName(TemplateKeys.BOOKMARKS_INSERT_SAVE);
        Stats.record("Save bookmark", request.getRequestURL());
	}

	public void updateSave()
	{
		int id = this.request.getIntParameter("bookmark_id");
		BookmarkDAO bookmarkDao = DataAccessDriver.getInstance().newBookmarkDAO();
		Bookmark bookmark = bookmarkDao.selectById(id);

		if (!this.sanityCheck(bookmark)) {
			return;
		}

		final SafeHtml safeHtml = new SafeHtml();

		bookmark.setDescription(safeHtml.makeSafe(this.request.getParameter("description")));
		bookmark.setTitle(safeHtml.makeSafe(this.request.getParameter("title")));

		String visible = this.request.getParameter("visible");
		bookmark.setPublicVisible(StringUtils.isNotBlank(visible));

		bookmarkDao.update(bookmark);
		this.setTemplateName(TemplateKeys.BOOKMARKS_UPDATE_SAVE);
	}

	public void edit()
	{
		int id = this.request.getIntParameter("bookmark_id");
		BookmarkDAO bookmarkDao = DataAccessDriver.getInstance().newBookmarkDAO();
		Bookmark bookmark = bookmarkDao.selectById(id);

		if (!this.sanityCheck(bookmark)) {
			return;
		}

		this.setTemplateName(TemplateKeys.BOOKMARKS_EDIT);
		this.context.put("bookmark", bookmark);
	}

	public void delete()
	{
		int id = this.request.getIntParameter("bookmark_id");
		BookmarkDAO bookmarkDao = DataAccessDriver.getInstance().newBookmarkDAO();
		Bookmark bookmark = bookmarkDao.selectById(id);

		if (!this.sanityCheck(bookmark)) {
			return;
		}

		bookmarkDao.remove(id);

		JForumExecutionContext.setRedirect(this.request.getContextPath() + "/bookmarks/list/" + bookmark.getUserId()
				+ SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
	}

	private boolean sanityCheck(Bookmark bookmark)
	{
		if (bookmark == null) {
			this.error("Bookmarks.notFound");
			return false;
		}

		if (bookmark.getUserId() != SessionFacade.getUserSession().getUserId()) {
			this.error("Bookmarks.notOwner");
			return false;
		}

		return true;
	}

	private void error(final String message)
	{
		this.setTemplateName(TemplateKeys.BOOKMARKS_ERROR);
		this.context.put("message", I18n.getMessage(message));
	}

	public void disabled()
	{
		this.error("Bookmarks.featureDisabled");
	}

	public void anonymousIsDenied()
	{
		this.error("Bookmarks.anonymousIsDenied");
	}

	/**
	 * @see net.jforum.Command#list()
	 */
	/*
	public void list()
	{
		int userId = this.request.getIntParameter("user_id");

		User user = DataAccessDriver.getInstance().newUserDAO().selectById(userId);
		if (user.getId() == 0) {
			this.error("Bookmarks.notFound");
		}
		else {
			this.setTemplateName(TemplateKeys.BOOKMARKS_LIST);
			this.context.put("bookmarks", DataAccessDriver.getInstance().newBookmarkDAO().selectByUser(userId));
			this.context.put("forumType", Integer.valueOf(BookmarkType.FORUM));
			this.context.put("userType", Integer.valueOf(BookmarkType.USER));
			this.context.put("topicType", Integer.valueOf(BookmarkType.TOPIC));
			this.context.put("user", user);
			this.context.put("loggedUserId", Integer.valueOf(SessionFacade.getUserSession().getUserId()));
			this.context.put("pageTitle", I18n.getMessage("Bookmarks.for")+" "+user.getUsername());
		}
	}
	*/
	public void list()
	{
		int userId = 0;
		try {
			userId = this.request.getIntParameter("user_id");
		} catch (NumberFormatException nfex) {
			// no userID passed - means we're accessing our own bookmarks
			if (SessionFacade.isLogged()) {
				userId = SessionFacade.getUserSession().getUserId();
			} else {
				this.error("Bookmarks.notFound");
				return;
			}
		}

		this.setTemplateName(TemplateKeys.BOOKMARKS_LIST);
		List<Bookmark> bookmarks = DataAccessDriver.getInstance().newBookmarkDAO().selectByUser(userId);
		// remove bookmarks from list that are in forums which this user is not allowed to see
		for (Iterator<Bookmark> iter = bookmarks.iterator(); iter.hasNext(); ) {
			Bookmark b = (Bookmark) iter.next();			
			Forum f = ForumRepository.getForum(b.getForumId());
			if (f == null || !ForumRepository.isCategoryAccessible(f.getCategoryId())) {
				iter.remove();
			}
		}

		// a user viewing his own bookmarks also gets to see his topic and forum watches
		if (userId == SessionFacade.getUserSession().getUserId()) {
			this.context.put("watchedForums",
							DataAccessDriver.getInstance().newForumDAO().selectWatchesByUser(userId));
			this.context.put("watchedTopics",
							DataAccessDriver.getInstance().newTopicDAO().selectWatchesByUser(userId));
		}

		this.context.put("bookmarks", bookmarks);
		this.context.put("forumType", Integer.valueOf(BookmarkType.FORUM));
		this.context.put("userType", Integer.valueOf(BookmarkType.USER));
		this.context.put("topicType", Integer.valueOf(BookmarkType.TOPIC));
		User u = DataAccessDriver.getInstance().newUserDAO().selectById(userId);
		this.context.put("user", u);
		this.context.put("loggedUserId", Integer.valueOf(SessionFacade.getUserSession().getUserId()));
		this.context.put("pageTitle", I18n.getMessage("Bookmarks.for")+" "+u.getUsername());
		this.context.put("fr", new ForumRepository());
	}

	/**
	 * @see net.jforum.Command#process(net.jforum.context.RequestContext, net.jforum.context.ResponseContext, freemarker.template.SimpleHash) 
	 */
	public Template process(final RequestContext request, final ResponseContext response, final SimpleHash context)
	{
		if (SessionFacade.getUserSession().getUserId() == SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)
				&& !request.getAction().equals("list")) {
			request.addParameter("action", "anonymousIsDenied");
		}
		else if (!SecurityRepository.canAccess(SecurityConstants.PERM_BOOKMARKS_ENABLED)) {
			request.addParameter("action", "disabled");
		}

		return super.process(request, response, context);
	}
}
