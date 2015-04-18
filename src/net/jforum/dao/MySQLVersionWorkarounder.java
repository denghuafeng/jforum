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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * Try to fix some database configuration problems.
 * This class will much likely do some checks only for MySQL.
 * @author Rafael Steil
 * @version $Id$
 */
public class MySQLVersionWorkarounder extends DBVersionWorkarounder
{
	private static final Logger LOGGER = Logger.getLogger(MySQLVersionWorkarounder.class);
    private static final String MYSQL_DATA_ACCESS_DRIVER = net.jforum.dao.mysql.MysqlDataAccessDriver.class.getName();

    public void handleWorkarounds(final Connection conn)
	{
		if (conn == null) {
			LOGGER.warn("Cannot work with a null connection");
			return;
    	}
    	
    	if (!"mysql".equals(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_NAME))) {
    		return;
    	}
    	
    	try {
    		final DatabaseMetaData meta = conn.getMetaData();
    		LOGGER.debug("MySQL Version: " + meta.getDatabaseProductVersion());
    		
    		final int major = meta.getDatabaseMajorVersion();
    		final int minor = meta.getDatabaseMinorVersion();
    		
    		if (major == 4 && minor == 0) {
    			this.handleMySql40x();
    		}
    		else if (major > 4 || (major == 4 && minor > 0)) {
    			this.handleMySql41xPlus();
    		}
    	}
    	catch (Exception e) {
    		LOGGER.error(e.toString(), e);
    	}
	}	
	
	private void handleMySql40x() throws IOException
	{
		this.ensureDaoClassIsCorrect(MYSQL_DATA_ACCESS_DRIVER);
		
		final Properties properties = this.loadSqlQueries();
		
		if (properties != null &&
			 (properties.size() == 0 || properties.getProperty("PermissionControl.deleteAllRoleValues") == null)) {
			final String path = this.buildPath("mysql_40.sql");

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
	
	private void handleMySql41xPlus() throws IOException
	{
		this.ensureDaoClassIsCorrect(MYSQL_DATA_ACCESS_DRIVER);
		
		final Properties properties = this.loadSqlQueries();
		
		if (properties != null && properties.size() > 0) {
			this.saveSqlQueries(new Properties());
		}
		
		this.fixEncoding();
	}
	
	private void fixEncoding() throws IOException
	{
		FileInputStream fis = null;
		OutputStream outputStream = null;
		
		try {
			final Properties properties = new Properties();
			
			final File file = new File(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_CONFIG));
			
			if (file.canWrite()) {
				fis = new FileInputStream(file);
				
				properties.load(fis);
				
				properties.setProperty(ConfigKeys.DATABASE_MYSQL_ENCODING, "");
				properties.setProperty(ConfigKeys.DATABASE_MYSQL_UNICODE, "");
				
				outputStream = new FileOutputStream(file);
				properties.store(outputStream, null);
			}
		}
		finally {
			if (fis != null) {
				fis.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}
}
