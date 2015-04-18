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
 * Created on Mar 3, 2003 / 11:07:02 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;

import java.util.ArrayList;
import java.util.List;

import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.GroupDAO;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.entities.Group;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.RolesRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.PermissionSection;
import net.jforum.security.XMLPermissionControl;
import net.jforum.util.I18n;
import net.jforum.util.TreeGroup;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;

/**
 * ViewHelper class for group administration.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class GroupAction extends AdminCommand 
{
	private static final String GROUP_ID = "group_id";
	
	/**
	 * Listing
	 */
	public void list()
	{
		this.context.put("groups", new TreeGroup().getNodes());
		this.setTemplateName(TemplateKeys.GROUP_LIST);
	}
	
	/**
	 * Insert
	 */
	public void insert()
	{
		this.context.put("groups", new TreeGroup().getNodes());
		this.context.put("action", "insertSave");
		this.context.put("selectedList", new ArrayList<Group>());
		this.setTemplateName(TemplateKeys.GROUP_INSERT);
	}
	
	/**
	 * Save information for an existing group
	 */
	public void editSave()
	{
		final int groupId = this.request.getIntParameter(GROUP_ID);
			
		final Group group = new Group();
		group.setDescription(this.request.getParameter("group_description"));
		group.setId(groupId);
		
		int parentId = this.request.getIntParameter("parent_id");
		
		if (parentId == group.getId()) {
			parentId = 0;
		}
		
		group.setParentId(parentId);
		group.setName(this.request.getParameter("group_name"));

		DataAccessDriver.getInstance().newGroupDAO().update(group);
			
		this.list();
	}
	
	/**
	 * Edit a group
	 */
	public void edit()
	{
		final int groupId = this.request.getIntParameter(GROUP_ID);
		final GroupDAO groupDao = DataAccessDriver.getInstance().newGroupDAO();
		
		this.setTemplateName(TemplateKeys.GROUP_EDIT);
					
		this.context.put("group", groupDao.selectById(groupId));
		this.context.put("groups", new TreeGroup().getNodes());
		this.context.put("selectedList", new ArrayList<Group>());
		this.context.put("action", "editSave");	
	}
	
	/**
	 * Deletes a group
	 */
	public void delete() 
	{		
		final String groupId[] = this.request.getParameterValues(GROUP_ID);
		
		if (groupId == null) {
			this.list();
			
			return;
		}
		
		final List<String> errors = new ArrayList<String>();
		final GroupDAO groupDao = DataAccessDriver.getInstance().newGroupDAO();
			
		for (int i = 0; i < groupId.length; i++) {
			final int id = Integer.parseInt(groupId[i]);
			
			if (groupDao.canDelete(id)) {
				groupDao.delete(id);
			}
			else {
				errors.add(I18n.getMessage(I18n.CANNOT_DELETE_GROUP, new Object[] { Integer.valueOf(id) }));
			}
		}
		
		if (!errors.isEmpty()) {
			this.context.put("errorMessage", errors);
		}
			
		this.list();
	}
	
	/**
	 * Saves a new group
	 */
	public void insertSave()
	{
		final GroupDAO groupDao = DataAccessDriver.getInstance().newGroupDAO();
		
		final Group group = new Group();
		group.setDescription(this.request.getParameter("group_description"));
		group.setParentId(this.request.getIntParameter("parent_id"));
		group.setName(this.request.getParameter("group_name"));
			
		groupDao.addNew(group);			
			
		this.list();
	}
	
	/**
	 * Permissions
	 */
	public void permissions()
	{
		final int id = this.request.getIntParameter(GROUP_ID);
		
		final PermissionControl permissionControl = new PermissionControl();
		permissionControl.setRoles(DataAccessDriver.getInstance().newGroupSecurityDAO().loadRoles(id));
		
		final String xmlconfig = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/permissions.xml"; 
		final List<PermissionSection> sections = new XMLPermissionControl(permissionControl).loadConfigurations(xmlconfig); 
		
		final GroupDAO groupDao = DataAccessDriver.getInstance().newGroupDAO();

		this.context.put("sections", sections);
		this.context.put("group", groupDao.selectById(id));
		this.setTemplateName(TemplateKeys.GROUP_PERMISSIONS);
	}
	
	public void permissionsSave()
	{
		final int id = this.request.getIntParameter("id");
		
		final GroupSecurityDAO gmodel = DataAccessDriver.getInstance().newGroupSecurityDAO();
		
		final PermissionControl pc = new PermissionControl();
		pc.setSecurityModel(gmodel);
		
		new PermissionProcessHelper(pc, id).processData();

		SecurityRepository.clean();
		RolesRepository.clear();
		ForumRepository.clearModeratorList();
		
		this.list();
	}
}
