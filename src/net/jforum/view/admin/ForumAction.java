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
 * Created on Mar 28, 2003 / 8:21:56 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;

import java.util.ArrayList;
import java.util.List;

import net.jforum.dao.CategoryDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.dao.MailIntegrationDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Category;
import net.jforum.entities.Forum;
import net.jforum.entities.MailIntegration;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.RolesRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.Role;
import net.jforum.security.RoleValue;
import net.jforum.security.RoleValueCollection;
import net.jforum.security.SecurityConstants;
import net.jforum.util.TreeGroup;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.admin.common.ModerationCommon;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class ForumAction extends AdminCommand 
{
	private static final String FORUM_ID = "forum_id";
	/**
	 * Listing
	 */
	public void list()
	{
		this.context.put("categories", DataAccessDriver.getInstance().newCategoryDAO().selectAll());
		this.context.put("repository", new ForumRepository());
		this.setTemplateName(TemplateKeys.FORUM_ADMIN_LIST);
	}
	
	/**
	 * One more, one more
	 */
	public void insert()
	{
		final CategoryDAO categoryDao = DataAccessDriver.getInstance().newCategoryDAO();
		
		this.context.put("groups", new TreeGroup().getNodes());
		this.context.put("selectedList", new ArrayList<Forum>());
		this.setTemplateName(TemplateKeys.FORUM_ADMIN_INSERT);
		this.context.put("categories",categoryDao.selectAll());
		this.context.put("action", "insertSave");		
	}
	
	/**
	 * Edit
	 */
	public void edit()
	{
		final int forumId = this.request.getIntParameter(FORUM_ID);
		final ForumDAO forumDao = DataAccessDriver.getInstance().newForumDAO();
		
		final CategoryDAO categoryDao = DataAccessDriver.getInstance().newCategoryDAO();
		
		this.setTemplateName(TemplateKeys.FORUM_ADMIN_EDIT);
		this.context.put("categories", categoryDao.selectAll());
		this.context.put("action", "editSave");
		this.context.put("forum", forumDao.selectById(forumId));
		
		// Mail Integration
		final MailIntegrationDAO integrationDao = DataAccessDriver.getInstance().newMailIntegrationDAO();
		this.context.put("mailIntegration", integrationDao.find(forumId));
	}
	
	public void editSave()
	{
		ForumDAO forumDao = DataAccessDriver.getInstance().newForumDAO();
		Forum forum = forumDao.selectById(this.request.getIntParameter(FORUM_ID));
		
		boolean moderated = forum.isModerated();
		int categoryId = forum.getCategoryId();
		
		forum.setDescription(this.request.getParameter("description"));
		forum.setIdCategories(this.request.getIntParameter("categories_id"));
		forum.setName(this.request.getParameter("forum_name"));
		forum.setModerated("1".equals(this.request.getParameter("moderate")));

		forumDao.update(forum);

		if (moderated != forum.isModerated()) {
			new ModerationCommon().setTopicModerationStatus(forum.getId(), forum.isModerated());
		}
		
		if (categoryId != forum.getCategoryId()) {
			forum.setIdCategories(categoryId);
			ForumRepository.removeForum(forum);
			
			forum.setIdCategories(this.request.getIntParameter("categories_id"));
			ForumRepository.addForum(forum);
		}
		else {
			ForumRepository.reloadForum(forum.getId());
		}
		
		this.handleMailIntegration();
		
		this.list();
	}
	
	private void handleMailIntegration()
	{
		int forumId = this.request.getIntParameter(FORUM_ID);
		MailIntegrationDAO dao = DataAccessDriver.getInstance().newMailIntegrationDAO();
		
		if (!"1".equals(this.request.getParameter("mail_integration"))) {
			dao.delete(forumId);
		}
		else {
			boolean exists = dao.find(forumId) != null;
			
			MailIntegration mailIntegration = this.fillMailIntegrationFromRequest();
			
			if (exists) {
				dao.update(mailIntegration);
			}
			else {
				dao.add(mailIntegration);
			}
		}
	}
	
	private MailIntegration fillMailIntegrationFromRequest()
	{
		MailIntegration mailIntegration = new MailIntegration();
		
		mailIntegration.setForumId(this.request.getIntParameter(FORUM_ID));
		mailIntegration.setForumEmail(this.request.getParameter("forum_email"));
		mailIntegration.setPopHost(this.request.getParameter("pop_host"));
		mailIntegration.setPopUsername(this.request.getParameter("pop_username"));
		mailIntegration.setPopPassword(this.request.getParameter("pop_password"));
		mailIntegration.setPopPort(this.request.getIntParameter("pop_port"));
		mailIntegration.setSsl("1".equals(this.request.getParameter("requires_ssl")));
		
		return mailIntegration;
	}
	
	public void up()
	{
		this.processOrdering(true);
	}
	
	public void down()
	{
		this.processOrdering(false);
	}
	
	private void processOrdering(final boolean isUp)
	{
		Forum toChange = new Forum(ForumRepository.getForum(Integer.parseInt(
				this.request.getParameter(FORUM_ID))));
		
		Category category = ForumRepository.getCategory(toChange.getCategoryId());
		List<Forum> forums = new ArrayList<Forum>(category.getForums());
		int index = forums.indexOf(toChange);
		
		if (index == -1 || (isUp && index == 0) || (!isUp && index + 1 == forums.size())) {
			this.list();
			return;
		}
		
		ForumDAO fm = DataAccessDriver.getInstance().newForumDAO();
		
		if (isUp) {
			// Get the forum which comes *before* the forum we're changing
			Forum otherForum = new Forum(forums.get(index - 1));
			fm.setOrderUp(toChange, otherForum);
		}
		else {
			// Get the forum which comes *after* the forum we're changing
			Forum otherForum = new Forum(forums.get(index + 1));
			fm.setOrderDown(toChange, otherForum);
		}
		
		category.changeForumOrder(toChange);
		ForumRepository.refreshCategory(category);
		
		this.list();
	}
	
	/**
	 * Delete
	 */
	public void delete()
	{
		String ids[] = this.request.getParameterValues(FORUM_ID);
		
		ForumDAO forumDao = DataAccessDriver.getInstance().newForumDAO();
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		
		if (ids != null) {
			for (int i = 0; i < ids.length; i++) {
				int forumId = Integer.parseInt(ids[i]);

				topicDao.deleteByForum(forumId);
				forumDao.delete(forumId);
				
				ForumRepository.removeForum(ForumRepository.getForum(forumId));
			}
			
			SecurityRepository.clean();
			RolesRepository.clear();
		}
		
		this.list();
	}
	
	/**
	 * A new one
	 */
	public void insertSave()
	{
		Forum forum = new Forum();
		forum.setDescription(this.request.getParameter("description"));
		forum.setIdCategories(this.request.getIntParameter("categories_id"));
		forum.setName(this.request.getParameter("forum_name"));	
		forum.setModerated("1".equals(this.request.getParameter("moderate")));
			
		int forumId = DataAccessDriver.getInstance().newForumDAO().addNew(forum);
		forum.setId(forumId);
		
		ForumRepository.addForum(forum);
		
		GroupSecurityDAO gmodel = DataAccessDriver.getInstance().newGroupSecurityDAO();
		PermissionControl pc = new PermissionControl();
		pc.setSecurityModel(gmodel);
		
		String[] allGroups = this.request.getParameterValues("groups");
		
		// Access
		String[] groups = this.request.getParameterValues("groupsAccess");
		if (groups != null) {
			this.addRole(pc, SecurityConstants.PERM_FORUM, forum.getId(), groups);
		}
		else {
			this.addRole(pc, SecurityConstants.PERM_FORUM, forum.getId(), allGroups);
		}
		
		// Anonymous posts
		groups = this.request.getParameterValues("groupsAnonymous");
		if (groups != null) {
			this.addRole(pc, SecurityConstants.PERM_ANONYMOUS_POST, forum.getId(), groups);
		}
		/*else {
			//this.addRole(pc, SecurityConstants.PERM_ANONYMOUS_POST, f.getId(), allGroups);
		}*/
		
		// Read-only
		groups = this.request.getParameterValues("groupsReadOnly");
		if (groups != null) {
			this.addRole(pc, SecurityConstants.PERM_READ_ONLY_FORUMS, forum.getId(), groups);
		}
		else {
			this.addRole(pc, SecurityConstants.PERM_READ_ONLY_FORUMS, forum.getId(), allGroups);
		}
		
		// Reply-only
		this.addRole(pc, SecurityConstants.PERM_REPLY_ONLY, forum.getId(), allGroups);
		
		// HTML
		groups = this.request.getParameterValues("groupsHtml");
		if (groups != null) {
			this.addRole(pc, SecurityConstants.PERM_HTML_DISABLED, forum.getId(), groups);
		}
		else {
			this.addRole(pc, SecurityConstants.PERM_HTML_DISABLED, forum.getId(), allGroups);
		}
		
		SecurityRepository.clean();
		RolesRepository.clear();
		
		this.request.addParameter(FORUM_ID, String.valueOf(forumId));
		this.handleMailIntegration();

		this.list();
	}
	
	private void addRole(final PermissionControl permissionControl, final String roleName, final int forumId, final String[] groups) 
	{
		Role role = new Role();
		role.setName(roleName);		
		
		for (int i = 0; i < groups.length; i++) {
			int groupId = Integer.parseInt(groups[i]);
			RoleValueCollection roleValues = new RoleValueCollection();
			
			RoleValue roleValue = new RoleValue();
			roleValue.setValue(Integer.toString(forumId));
			roleValues.add(roleValue);
			
			permissionControl.addRoleValue(groupId, role, roleValues);
		}
	}
}
