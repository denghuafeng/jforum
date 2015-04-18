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
 * Created on Mar 3, 2003 / 1:35:30 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.entities.Group;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericGroupDAO implements net.jforum.dao.GroupDAO
{
	/**
	 * @see net.jforum.dao.GroupDAO#selectById(int)
	 */
	public Group selectById(final int groupId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.selectById"));
			pstmt.setInt(1, groupId);

			resultSet = pstmt.executeQuery();

			Group group = new Group();

			if (resultSet.next()) {
				group = this.getGroup(resultSet);
			}

			return group;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.GroupDAO#canDelete(int)
	 */
	public boolean canDelete(final int groupId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.canDelete"));
			pstmt.setInt(1, groupId);

			boolean status = false;

			resultSet = pstmt.executeQuery();
			if (!resultSet.next() || resultSet.getInt("total") < 1) {
				status = true;
			}

			return status;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.GroupDAO#delete(int)
	 */
	public void delete(final int groupId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.delete"));
			pstmt.setInt(1, groupId);

			pstmt.executeUpdate();
			
			final GroupSecurityDAO securityDao = DataAccessDriver.getInstance().newGroupSecurityDAO();
			securityDao.deleteAllRoles(groupId);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.GroupDAO#update(net.jforum.entities.Group)
	 */
	public void update(final Group group)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.update"));
			pstmt.setString(1, group.getName());
			pstmt.setInt(2, group.getParentId());
			pstmt.setString(3, group.getDescription());
			pstmt.setInt(4, group.getId());

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
	 * @see net.jforum.dao.GroupDAO#addNew(net.jforum.entities.Group)
	 */
	public void addNew(final Group group)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.addNew"));
			pstmt.setString(1, group.getName());
			pstmt.setString(2, group.getDescription());
			pstmt.setInt(3, group.getParentId());

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
	 * @see net.jforum.dao.GroupDAO#selectUsersIds(int)
	 */
	public List<Integer> selectUsersIds(final int groupId)
	{
		final ArrayList<Integer> list = new ArrayList<Integer>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("GroupModel.selectUsersIds"));
			pstmt.setInt(1, groupId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(Integer.valueOf(resultSet.getInt("user_id")));
			}

			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected List<Group> fillGroups(final ResultSet resultSet) throws SQLException
	{
		final List<Group> list = new ArrayList<Group>();

		while (resultSet.next()) {
			list.add(this.getGroup(resultSet));
		}

		return list;
	}

	protected Group getGroup(final ResultSet resultSet) throws SQLException
	{
		final Group group = new Group();

		group.setId(resultSet.getInt("group_id"));
		group.setDescription(resultSet.getString("group_description"));
		group.setName(resultSet.getString("group_name"));
		group.setParentId(resultSet.getInt("parent_id"));

		return group;
	}

	/**
	 * @see net.jforum.dao.GroupDAO#selectAll()
	 */
	public List<Group> selectAll()
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("GroupModel.selectAll"));
			resultSet = pstmt.executeQuery();

			return this.fillGroups(resultSet);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}
}
