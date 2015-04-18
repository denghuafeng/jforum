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
 * Created on 19/03/2004 - 18:44:56
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic.security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.dao.generic.AutoKeys;
import net.jforum.entities.Group;
import net.jforum.entities.User;
import net.jforum.exceptions.DatabaseException;
import net.jforum.repository.RolesRepository;
import net.jforum.security.Role;
import net.jforum.security.RoleCollection;
import net.jforum.security.RoleValue;
import net.jforum.security.RoleValueCollection;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericGroupSecurityDAO extends AutoKeys implements GroupSecurityDAO
{
	private List<Integer> selectForumRoles(final int forumId) 
	{
		final List<Integer> l = new ArrayList<Integer>();
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PermissionControl.selectForumRoles"));
			pstmt.setString(1, String.valueOf(forumId));
			
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				l.add(Integer.valueOf(rs.getInt("role_id")));
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
		
		return l;
	}
	
	public void deleteForumRoles(final int forumId) 
	{
		PreparedStatement pstmt = null;
		
		final List<Integer> roleIds = this.selectForumRoles(forumId);
		
		try {
			final StringBuilder ids = new StringBuilder();
			
			for (final Iterator<Integer> iterator = roleIds.iterator(); iterator.hasNext();) {
				final Integer id = (Integer)iterator.next();
				ids.append(id).append(',');
			}
			
			ids.append("-1");
			
			// Role values
			String sql = SystemGlobals.getSql("PermissionControl.deleteRoleValues");
			sql = StringUtils.replace(sql, "#IDS#", ids.toString());
			
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setString(1, String.valueOf(forumId));
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}
	
	/**
	 * @see net.jforum.dao.GroupSecurityDAO#deleteAllRoles(int)
	 */
	public void deleteAllRoles(final int groupId)
	{
		PreparedStatement pstmt = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PermissionControl.deleteAllRoleValues"));
			pstmt.setInt(1, groupId);
			pstmt.executeUpdate();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PermissionControl.deleteAllGroupRoles"));
			pstmt.setInt(1, groupId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.GroupSecurityDAO#addRole(int, net.jforum.security.Role)
	 */
	public void addRole(final int id, final Role role)
	{
		this.addRole(id, role, null);
	}

	/**
	 * @see net.jforum.dao.GroupSecurityDAO#addRole(int, net.jforum.security.Role,
	 *      net.jforum.security.RoleValueCollection)
	 */
	public void addRole(int id, Role role, RoleValueCollection roleValues)
	{
		this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("PermissionControl.lastGeneratedRoleId"));
		SecurityCommon.executeAddRole(SystemGlobals.getSql("PermissionControl.addGroupRole"), id, role, roleValues,
			this.supportAutoGeneratedKeys(), this.getAutoGeneratedKeysQuery());
	}

	/**
	 * @see net.jforum.dao.GroupSecurityDAO#loadRoles(int)
	 */
	public RoleCollection loadRoles(int groupId)
	{
		return this.loadRoles(new int[] { groupId });
	}
	
	protected RoleCollection loadRoles(int[] groupIds)
	{
		String sql = SystemGlobals.getSql("PermissionControl.loadGroupRoles");
		String groupIdAsString = SecurityCommon.groupIdAsString(groupIds);
		
		if ("".equals(groupIdAsString)) {
			// We suppose there is no "negative" group ids
			sql = sql.replaceAll("#IN#", "-1");
		}
		else {
			sql = sql.replaceAll("#IN#", groupIdAsString);
		}
		
		RoleCollection roles = null;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			roles = SecurityCommon.loadRoles(rs);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
		
		return roles;
	}

	/**
	 * @see net.jforum.dao.GroupSecurityDAO#addRoleValue(int, net.jforum.security.Role, net.jforum.security.RoleValueCollection)
	 */
	public void addRoleValue(int groupId, Role role, RoleValueCollection rvc)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("PermissionControl.getRoleIdByName"));
			pstmt.setString(1, role.getName());
			pstmt.setInt(2, groupId);

			int roleId = -1;

			rs = pstmt.executeQuery();
			if (rs.next()) {
				roleId = rs.getInt("role_id");
			}

			rs.close();
			pstmt.close();

			if (roleId == -1) {
				this.addRole(groupId, role, rvc);
			}
			else {
				pstmt = JForumExecutionContext.getConnection().prepareStatement(
						SystemGlobals.getSql("PermissionControl.addRoleValues"));
				pstmt.setInt(1, roleId);

				for (Iterator<?> iter = rvc.iterator(); iter.hasNext();) {
					RoleValue rv = (RoleValue) iter.next();
					pstmt.setString(2, rv.getValue());
					pstmt.executeUpdate();
				}
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.GroupSecurityDAO#loadRolesByUserGroups(net.jforum.entities.User)
	 */
	public RoleCollection loadRolesByUserGroups(User user)
	{
		List<Group> groups = user.getGroupsList();

		// When the user is associated to more than one group, we
		// should check the merged roles
		int[] groupIds = this.getSortedGroupIds(groups);

		RoleCollection groupRoles = RolesRepository.getGroupRoles(groupIds);

		// Not cached yet? then do it now
		if (groupRoles == null) {
			groupRoles = this.loadRoles(groupIds);
			RolesRepository.addGroupRoles(groupIds, groupRoles);
		}

		return groupRoles;
	}

	private int[] getSortedGroupIds(List<Group> groups)
	{
		int[] groupsIds = new int[groups.size()];
		int i = 0;

		for (Iterator<Group> iter = groups.iterator(); iter.hasNext();) {
			groupsIds[i++] = iter.next().getId();
		}

		Arrays.sort(groupsIds);

		return groupsIds;
	}
}
