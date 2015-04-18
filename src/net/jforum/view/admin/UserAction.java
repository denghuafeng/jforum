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
 * Created on Apr 19, 2003 / 9:13:16 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.GroupDAO;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Group;
import net.jforum.entities.User;
import net.jforum.repository.SecurityRepository;
import net.jforum.util.I18n;
import net.jforum.util.TreeGroup;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.UserCommon;
import net.jforum.view.forum.common.ViewCommon;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class UserAction extends AdminCommand 
{
	private static final String USERS = "users";
	private static final String USER_ID = "user_id";	
	
	private final UserDAO userDao = DataAccessDriver.getInstance().newUserDAO();
	private final GroupDAO groupDao = DataAccessDriver.getInstance().newGroupDAO();
	
	public void list()
	{
		final int start = this.preparePagination(userDao.getTotalUsers());
		final int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);
		
		this.context.put(USERS, userDao.selectAll(start ,usersPerPage));
		this.commonData();
	}
	
	public void pendingActivations()
	{
		final List<User> users = userDao.pendingActivations();
		
		this.setTemplateName(TemplateKeys.USER_ADMIN_PENDING_ACTIVATIONS);
		this.context.put(USERS, users);
	}
	
	public void activateAccount()
	{
		final String[] ids = this.request.getParameterValues(USER_ID);
		
		if (ids != null) {			
			for (int i = 0; i < ids.length; i++) {
				final int userId = Integer.parseInt(ids[i]);
				userDao.writeUserActive(userId);
			}
		}
		
		this.pendingActivations();
	}
	
	private int preparePagination(final int totalUsers)
	{
		final int start = ViewCommon.getStartPage();
		final int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);
		
		ViewCommon.contextToPagination(start, totalUsers, usersPerPage);
		
		return start;
	}
	
	private void commonData()
	{
		this.context.put("selectedList", new ArrayList<User>());
		this.context.put("groups", new TreeGroup().getNodes());
		this.setTemplateName(TemplateKeys.USER_ADMIN_COMMON);
		this.context.put("searchAction", "list");
		this.context.put("searchId", Integer.valueOf(-1));
		this.context.put("action", "list");
	}
	
	public void groupSearch()
	{
		final int groupId = this.request.getIntParameter("group_id");
		if (groupId == 0) {
			this.list();
			return;
		}
	
		final int start = this.preparePagination(userDao.getTotalUsersByGroup(groupId));
		final int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);
		
		this.commonData();
		
		final List<Integer> list = new ArrayList<Integer>();
		list.add(Integer.valueOf(groupId));
		
		this.context.put("selectedList", list);
		this.context.put("searchAction", "groupSearch");
		this.context.put(USERS, userDao.selectAllByGroup(groupId, start, usersPerPage));
		this.context.put("searchId", Integer.valueOf(groupId));
		this.context.put("action", "groupSearch");
	}
	
	public void search()
	{
		final String group = this.request.getParameter("group_id");
		final String username = getTrimmedNonNullParameter("username");
        final String email = getTrimmedNonNullParameter("email");
        final String userId = getTrimmedNonNullParameter("userId");
        final String ip = getTrimmedNonNullParameter("ip");

	    if (!"".equals(ip)) {
            ipSearch();
        } else if (!"".equals(userId)) {
            List<User> users = new ArrayList<User>();
            try {
                User user = userDao.selectById(Integer.parseInt(userId));
                if (user != null) {
                    users.add(user);
                }
            } catch (NumberFormatException ignored) {
            }

            this.commonData();

            this.context.put(USERS, users);
            this.context.put("userId", userId);
            this.context.put("start", 1);
        } else if (!"".equals(email)) {
            emailSearch();
        } else if (!"".equals(username)) {
            List<User> users = userDao.findByName(username, false);

            this.commonData();

            this.context.put(USERS, users);
            this.context.put("search", username);
            this.context.put("start", 1);
        } else if (!"0".equals(group)) {
            this.groupSearch();
        } else {
            this.list();
        }
	}

    /**
    * Returns a trimmed value for the provided parameter name. If the parameter
    * does not exist, then an empty string is returned.
    *
    * @param parameterName the name of the parameter that we want the value of
    * @return the trimmed value or an empty string
    */
    private String getTrimmedNonNullParameter(String parameterName) {
        String value = this.request.getParameter(parameterName);
        return (value == null) ? "" : value.trim();
    }

    /**
     * Performs the search by IP address. Separated out so that pagination
     * can be easily handled. Still called by the generic search method so
     * that we have a single form on the search page.
     */
    public void ipSearch() {
        String ip = this.request.getParameter("ip");
        ip = (ip == null) ? "" : ip.trim().replaceAll("\\*", "%");

        int total = userDao.getTotalUsersByIp(ip);
        int start = this.preparePagination(total);
        int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

        List<User> users = userDao.findAllUsersByIp(ip, start, usersPerPage);

        this.commonData();

        this.context.put(USERS, users);
        this.context.put("ip", this.request.getParameter("ip"));
        this.context.put("start", 1);
        this.context.put("searchAction", "ipSearch");
        this.context.put("searchId", this.request.getParameter("ip"));
    }

    /**
    * Performs the search by email address. Separated out so that pagination
    * can be easily handled. Still called by the generic search method so
    * that we have a single form on the search page.
    */
    public void emailSearch() {
        String email = this.request.getParameter("email");
        email = (email == null) ? "" : email.trim();

        int total = userDao.getTotalUsersWithEmail(email);
        int start = this.preparePagination(total);
        int usersPerPage = SystemGlobals.getIntValue(ConfigKeys.USERS_PER_PAGE);

        List<User> users = userDao.findAllUsersByEmail(email, start, usersPerPage);

        this.commonData();
        this.context.put(USERS, users);
        this.context.put("email", email);
        this.context.put("searchAction", "emailSearch");
        this.context.put("searchId", email);
    }

	public void edit()
	{
		final int userId = this.request.getIntParameter("id");	
		final User user = userDao.selectById(userId);
		
		this.setTemplateName(TemplateKeys.USER_ADMIN_EDIT);
		this.context.put("u", user);
		this.context.put("action", "editSave");		
		this.context.put("specialRanks", DataAccessDriver.getInstance().newRankingDAO().selectSpecials());
		this.context.put("avatarAllowExternalUrl", SystemGlobals.getBoolValue(ConfigKeys.AVATAR_ALLOW_EXTERNAL_URL));
		this.context.put("avatarPath", SystemGlobals.getValue(ConfigKeys.AVATAR_IMAGE_DIR));
		this.context.put("admin", true);
	}

	public void editSave() 
	{
		int userId = this.request.getIntParameter(USER_ID);
		UserCommon.saveUser(userId);

		this.list();
	}

	// Delete
	public void delete()
	{
		String ids[] = this.request.getParameterValues(USER_ID);
		
		if (ids != null) {
			for (int i = 0; i < ids.length; i++) {
				
				int user = Integer.parseInt(ids[i]);
				
				if (userDao.isDeleted(user)){
					userDao.undelete(user);
				} 
				else {
					String sessionId = SessionFacade.isUserInSession(user);
					
					if (sessionId != null) {
						SessionFacade.remove(sessionId);
					}
					
					userDao.delete(user);
				}
			}
		}
		
		this.list();
	}
	
	// Groups
	public void groups()
	{
		int userId = this.request.getIntParameter("id");
		
		User user = userDao.selectById(userId);
		
		List<Integer> selectedList = new ArrayList<Integer>();
		for (Iterator<Group> iter = user.getGroupsList().iterator(); iter.hasNext(); ) {
			selectedList.add(Integer.valueOf(iter.next().getId()));
		}
		
		this.context.put("selectedList", selectedList);
		this.context.put("groups", new TreeGroup().getNodes());
		this.context.put("user", user);
		this.context.put("userId", Integer.valueOf(userId));
		this.setTemplateName(TemplateKeys.USER_ADMIN_GROUPS);
		this.context.put("groupFor", I18n.getMessage("User.GroupsFor", new String[] { user.getUsername() }));
	}
	
	// Groups Save
	public void groupsSave()
	{
		int userId = this.request.getIntParameter(USER_ID);
		
		// Remove the old groups
		List<Group> allGroupsList = groupDao.selectAll();
		int[] allGroups = new int[allGroupsList.size()];
		
		int counter = 0;
		for (Iterator<Group> iter = allGroupsList.iterator(); iter.hasNext(); counter++) {
			Group group = iter.next();
			
			allGroups[counter] = group.getId();
		}
		
		userDao.removeFromGroup(userId, allGroups);
		
		// Associate the user to the selected groups
		String[] selectedGroups = this.request.getParameterValues("groups");
		
		if(selectedGroups == null) {
			selectedGroups = new String[0]; 
		}
		
		int[] newGroups = new int[selectedGroups.length];
		
		for (int i = 0; i < selectedGroups.length; i++) {
			newGroups[i] = Integer.parseInt(selectedGroups[i]);
		}
		
		userDao.addToGroup(userId, newGroups);
		SecurityRepository.remove(userId);
		
		this.list();
	}
}
