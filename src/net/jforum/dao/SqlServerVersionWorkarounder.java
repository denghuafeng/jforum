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
 * Created on 29/11/2005 13:25:55
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * Try to fix some database configuration problems.
 * This class will much likely do some checks only for MS SQL Server.
 * @author Andowson Chang
 * @version $Id$
 */
public class SqlServerVersionWorkarounder extends DBVersionWorkarounder
{
	private static final Logger LOGGER = Logger.getLogger(SqlServerVersionWorkarounder.class);
    private static final String SQLSERVER_2000_DATA_ACCESS_DRIVER = net.jforum.dao.sqlserver.SqlServer2000DataAccessDriver.class.getName();
    private static final String SQLSERVER_DATA_ACCESS_DRIVER = net.jforum.dao.sqlserver.SqlServerDataAccessDriver.class.getName();

    public void handleWorkarounds(final Connection conn)
	{
		if (conn == null) {
			LOGGER.warn("Cannot work with a null connection");
			return;
    	}
    	
    	if (!"sqlserver".equals(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_NAME))) {
    		return;
    	}
    	
    	try {
    		final DatabaseMetaData meta = conn.getMetaData();
    		LOGGER.debug("SQL Server Version: " + meta.getDatabaseProductVersion());
    		
    		final int major = meta.getDatabaseMajorVersion();
    		final int minor = meta.getDatabaseMinorVersion();
    		LOGGER.debug("SQL Server Major Version: " + major);
    		LOGGER.debug("SQL Server Minor Version: " + minor);
    		
    		if (major == 8) {
    			this.handleSQLServer2000();
    			LOGGER.debug("handleSQLServer2000()");
    		}
    		else if (major > 8) {
    			this.handleSQLServer2005xPlus();
    			LOGGER.debug("handleSQLServer2005xPlus()");
    		}
    	}
    	catch (Exception e) {
    		LOGGER.error(e.toString(), e);
    	}
	}
	
	private void handleSQLServer2000() throws IOException
	{
		this.ensureDaoClassIsCorrect(SQLSERVER_2000_DATA_ACCESS_DRIVER);		
		
		final Properties properties = this.loadSqlQueries();
		
		final String path = this.buildPath("sqlserver_2000.sql");
				
		final FileInputStream fis = new FileInputStream(path);
			
		try {
			properties.load(fis);
			this.saveSqlQueries(properties);
		}
		finally {
			fis.close();
		}
	}
	
	private void handleSQLServer2005xPlus() throws IOException
	{
		this.ensureDaoClassIsCorrect(SQLSERVER_DATA_ACCESS_DRIVER);
		
        final Properties properties = this.loadSqlQueries();
		
		final String path = this.buildPath("sqlserver_2005.sql");
				
		final FileInputStream fis = new FileInputStream(path);
			
		try {
			properties.load(fis);
			this.saveSqlQueries(properties);
		}
		finally {
			fis.close();
		}
	}
}
