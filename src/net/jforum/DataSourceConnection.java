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
 * Created on Jan 7, 2005 7:44:40 PM
 *
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.jforum.exceptions.DatabaseException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * DataSource connection implementation for JForum.
 * The datasourcename should be set in the key 
 * <code>database.datasource.name</code> at 
 * SystemGlobals.properties.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class DataSourceConnection extends DBConnection
{
	private static final Logger LOGGER = Logger.getLogger(DataSourceConnection.class);
	
	private transient DataSource dataSource;
	
	/**
	 * @see net.jforum.DBConnection#init()
	 */
	public void init() throws NamingException 
	{
		final Context context = new InitialContext();
		this.dataSource = (DataSource)context.lookup(SystemGlobals.getValue(
				ConfigKeys.DATABASE_DATASOURCE_NAME));
		try {
			// Try to validate the connection url
			final Connection conn = this.getConnection();

			if (conn != null) {
				this.releaseConnection(conn);
				this.databaseUp = true;
			}
		} catch (Exception e) {
			this.databaseUp = false;
		}
	}
	/**
	 * @see net.jforum.DBConnection#getConnection()
	 */
	public Connection getConnection()
	{
		try {
			return this.dataSource.getConnection();
		}
		catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
			throw new DatabaseException(e);
		}
	}
}
