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
 * Created on 30/11/2005 17:07:51
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import net.jforum.exceptions.DatabaseException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class C3P0PooledConnection extends DBConnection
{
	private static final Logger LOGGER = Logger.getLogger(C3P0PooledConnection.class);
	
	private transient ComboPooledDataSource dataSource;
	
	/**
	 * 
	 * @see net.jforum.DBConnection#init()
	 */
	public void init() throws PropertyVetoException
	{
		this.dataSource = new ComboPooledDataSource();
		
		this.dataSource.setDriverClass(SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_DRIVER));
		this.dataSource.setJdbcUrl(SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_STRING));
		this.dataSource.setMinPoolSize(SystemGlobals.getIntValue(ConfigKeys.DATABASE_POOL_MIN));
		this.dataSource.setMaxPoolSize(SystemGlobals.getIntValue(ConfigKeys.DATABASE_POOL_MAX));
		this.dataSource.setIdleConnectionTestPeriod(SystemGlobals.getIntValue(ConfigKeys.DATABASE_PING_DELAY));
		
		this.extraParams();
		
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
	
	private void extraParams()
	{
		final String extra = SystemGlobals.getValue(ConfigKeys.C3P0_EXTRA_PARAMS);
		
		if (StringUtils.isNotBlank(extra)) {
			final String[] param = extra.split(";");
			
			for (int i = 0; i < param.length; i++) {
				final String[] keyvalue = param[i].trim().split("=");
				
				if (keyvalue.length == 2) {
					this.invokeSetter(keyvalue[0], keyvalue[1]);
				}
			}
		}
	}
	
	/**
	 * Huge hack to invoke methods without the need of an external configuration file
	 * and without knowing the argument's type
	 */
	private void invokeSetter(final String propertyName, final String value)
	{
		try {
			final String setter = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
			
			final Method[] methods = this.dataSource.getClass().getMethods();
			
			for (int i = 0; i < methods.length; i++) {
				final Method method = methods[i];
				
				if (method.getName().equals(setter)) {
					final Class<?>[] paramTypes = method.getParameterTypes();
					
					if (paramTypes[0] == String.class) {
						method.invoke(this.dataSource, new Object[] { value });
					}
					else if (paramTypes[0] == int.class) {
						method.invoke(this.dataSource, new Object[] { Integer.valueOf(value) });
					}
					else if (paramTypes[0] == boolean.class) {
						method.invoke(this.dataSource, new Object[] { Boolean.valueOf(value) });
					}
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
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
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new DatabaseException(e);
		}
	}

	/**
	 * @see net.jforum.DBConnection#realReleaseAllConnections()
	 */
	public void realReleaseAllConnections()
	{
		try {
			DataSources.destroy(this.dataSource);
			Thread.sleep(1000);
			this.databaseUp = false;
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
			throw new DatabaseException(e);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
			throw new DatabaseException(e);
		}		
	}
}
