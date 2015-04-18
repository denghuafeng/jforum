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
 * Created on 30/05/2004 15:10:57
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import net.jforum.entities.UserSession;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericUserSessionDAO implements net.jforum.dao.UserSessionDAO
{
	/**
	 * @see net.jforum.dao.UserSessionDAO#add(net.jforum.entities.UserSession, java.sql.Connection)
	 */
	public void add(UserSession us, Connection conn)
	{
		this.add(us, conn, false);
	}

	private void add(UserSession us, Connection conn, boolean checked)
	{
		if (!checked && this.selectById(us, conn) != null) {
			return;
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.add"));
			pstmt.setString(1, us.getSessionId());
			pstmt.setInt(2, us.getUserId());
			pstmt.setTimestamp(3, new Timestamp(us.getStartTime().getTime()));
			pstmt.setString(4, us.getIp());

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
	 * @see net.jforum.dao.UserSessionDAO#update(net.jforum.entities.UserSession,
	 *      java.sql.Connection)
	 */
	public void update(UserSession us, Connection conn)
	{
		if (this.selectById(us, conn) == null) {
			this.add(us, conn, true);
			return;
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.update"));
			pstmt.setTimestamp(1, new Timestamp(us.getStartTime().getTime()));
			pstmt.setLong(2, us.getSessionTime());
			pstmt.setString(3, us.getSessionId());
			pstmt.setString(4, us.getIp());
			pstmt.setInt(5, us.getUserId());

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
	 * @see net.jforum.dao.UserSessionDAO#selectById(net.jforum.entities.UserSession,
	 *      java.sql.Connection)
	 */
	public UserSession selectById(UserSession us, Connection conn)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(SystemGlobals.getSql("UserSessionModel.selectById"));
			pstmt.setInt(1, us.getUserId());

			rs = pstmt.executeQuery();
			boolean found = false;

			UserSession returnUs = new UserSession(us);

			if (rs.next()) {
				returnUs.setSessionTime(rs.getLong("session_time"));
				returnUs.setStartTime(rs.getTimestamp("session_start"));
				found = true;
			}

			return (found ? returnUs : null);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}
}
