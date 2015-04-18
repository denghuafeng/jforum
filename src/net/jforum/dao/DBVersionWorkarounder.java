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
 * Created on 13/12/2009 21:56:55
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.jforum.ConfigLoader;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public class DBVersionWorkarounder {

	private static final Logger LOGGER = Logger.getLogger(DBVersionWorkarounder.class);

	protected void ensureDaoClassIsCorrect(final String shouldBe) throws IOException {
		if (!shouldBe.equals(SystemGlobals.getValue(ConfigKeys.DAO_DRIVER))) {
			LOGGER.info("DAO class is incorrect. Setting it to " + shouldBe);
			
			this.fixDAODriver(shouldBe);
			
			SystemGlobals.setValue(ConfigKeys.DAO_DRIVER, shouldBe);
			ConfigLoader.loadDaoImplementation();
		}
	}

	protected Properties loadSqlQueries() throws IOException {
		// First, check if we really have a problem
		final String sqlQueries = SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER);
		
		final File file = new File(sqlQueries);
		
		final Properties properties = new Properties();
		
		final FileInputStream fis = new FileInputStream(file);
		
		try {
			properties.load(fis);
			
			if (file.canWrite()) {
				return properties;
			}
		}
		finally {
			try {
				fis.close();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		
		LOGGER.warn("Cannot overwrite" + sqlQueries + " file. Insuficient privileges");
		return null;
	}

	protected void saveSqlQueries(final Properties properties) throws IOException {
		final FileOutputStream fos = new FileOutputStream(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
		
		try {
			properties.store(fos, null);
		}
		finally {
			fos.close();
		}
	
		SystemGlobals.loadQueries(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
	}

	private void fixDAODriver(final String daoClassName) throws IOException {
		final String driverConfigPath = SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_CONFIG);
		
		final File file = new File(driverConfigPath);
		
		if (file.canWrite()) {
			// Fix the DAO class
			final Properties properties = new Properties();
			
			final FileInputStream fis = new FileInputStream(driverConfigPath);
			FileOutputStream fos = null;
			
			try {
				properties.load(fis);
				properties.setProperty(ConfigKeys.DAO_DRIVER, daoClassName);
				
				fos = new FileOutputStream(driverConfigPath);
				properties.store(fos, null);
			}
			finally {
				if (fos != null) {
					fos.close();
				}
	            
				fis.close();
			}
		}
		else {
			LOGGER.warn("Cannot overwrite" + driverConfigPath + ". Insuficient privileges");
		}
	}

	protected String buildPath(final String concat) {
		return new StringBuilder(256)
			.append(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR))
			.append("/database/")
			.append(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_NAME))
			.append('/')
			.append(concat)
			.toString();
	}

}
