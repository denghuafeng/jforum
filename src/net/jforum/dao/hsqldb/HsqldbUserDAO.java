/*
 * Copyright (c) JForum Team
 * All rights reserved.

 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:

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
 * Created on 09/11/2004 22:36:07
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.generic.GenericUserDAO;
import net.jforum.entities.User;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Marc Wick
 * @author Rafael Steil
 * @version $Id$
 */
public class HsqldbUserDAO extends GenericUserDAO
{
	/**
	 * @see net.jforum.dao.generic.GenericUserDAO#selectAllByGroup(int, int, int)
	 */
	public List<User> selectAllByGroup(final int groupId, final int start, final int count)
	{
		return super.selectAllByGroup(start, count, groupId);
	}
	
	/**
     * @see net.jforum.dao.UserDAO#findAllUsersByEmail(String, int, int)
	 */
	public List<User> findAllUsersByEmail(String email, int start, int count) {
		List<User> result = new ArrayList<User>();
		PreparedStatement p = null;
		ResultSet rs = null;

		User u = null;

		try {
			p = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("UserModel.findByEmail"));
			p.setInt(1, start);
			p.setInt(2, count);
			p.setString(3, email);
			rs = p.executeQuery();

			while (rs.next()) {
				u = new User();
				fillUserFromResultSet(u, rs);
				result.add(u);
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}

		return result;
	}
	
	/**
     * @see net.jforum.dao.UserDAO#findAllUsersByIp(String, int, int)
	 */
	public List<User> findAllUsersByIp(String ip, int start, int count) {
        List<User> result = new ArrayList<User>();
        PreparedStatement p = null;
        ResultSet rs = null;

        User u = null;

        try {
            p = JForumExecutionContext.getConnection().prepareStatement(
                    SystemGlobals.getSql("UserModel.findByIp"));            
            p.setInt(1, start);
            p.setInt(2, count);
            p.setString(3, ip);
            rs = p.executeQuery();

            while (rs.next()) {
                u = new User();
                fillUserFromResultSet(u, rs);
                result.add(u);
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            DbUtils.close(rs, p);
        }

        return result;
	}
}