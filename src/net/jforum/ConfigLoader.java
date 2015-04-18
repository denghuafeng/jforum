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
 * Created on 02/11/2004 12:45:37
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.jforum.api.integration.mail.pop.POPJobStarter;
import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.exceptions.CacheEngineStartupException;
import net.jforum.exceptions.ForumException;
import net.jforum.search.SearchFacade;
import net.jforum.sso.LoginAuthenticator;
import net.jforum.summary.SummaryScheduler;
import net.jforum.util.FileMonitor;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.QueriesFileListener;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.SystemGlobalsListener;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

/**
 * General utilities methods for loading configurations for JForum.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public final class ConfigLoader 
{
    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class);
    private static CacheEngine cache;

    /**
     * Start ( or restart ) <code>SystemGlobals</code>.
     * This method loads all configuration keys set at
     * <i>SystemGlobals.properties</i>, <i>&lt;user.name&gt;.properties</i>
     * and database specific stuff.
     * 
     * @param appPath The application root's directory
     */
    public static void startSystemglobals(final String appPath)
    {
        SystemGlobals.initGlobals(appPath, appPath + "/WEB-INF/config/SystemGlobals.properties");
    }

    /**
     * Loads module mappings for the system.
     * 
     * @param baseConfigDir The directory where the file <i>modulesMapping.properties</i> is.
     * @return The <code>java.util.Properties</code> instance, with the loaded modules 
     */
    public static Properties loadModulesMapping(final String baseConfigDir)
    {
        FileInputStream fis = null;

        try {
            final Properties modulesMapping = new Properties();
            fis = new FileInputStream(baseConfigDir + "/modulesMapping.properties");
            modulesMapping.load(fis);

            return modulesMapping;
        }
        catch (IOException e) {
            throw new ForumException( e);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }
        }
    }

    public static void createLoginAuthenticator()
    {
        final String className = SystemGlobals.getValue(ConfigKeys.LOGIN_AUTHENTICATOR);

        try {
            final LoginAuthenticator authenticator = (LoginAuthenticator) Class.forName(className).newInstance();
            SystemGlobals.setObjectValue(ConfigKeys.LOGIN_AUTHENTICATOR_INSTANCE, authenticator);
        }
        catch (Exception e) {
            throw new ForumException("Error while trying to create a login.authenticator instance ("
                + className + "): " + e, e);
        }
    }

    /**
     * Load url patterns.
     * The method tries to load url patterns from <i>WEB-INF/config/urlPattern.properties</i>
     */
    public static void loadUrlPatterns()  
    {
        FileInputStream fis = null;

        try {
            final Properties properties = new Properties();
            fis = new FileInputStream(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/urlPattern.properties");
            properties.load(fis);

            for (final Iterator<Map.Entry<Object, Object>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
                final Map.Entry<Object, Object> entry = iter.next();
                UrlPatternCollection.addPattern((String)entry.getKey(), (String)entry.getValue());
            }
        }
        catch (IOException e) {
            throw new ForumException(e);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }
        }
    }

    /**
     * Register a file change listener for following resources:
     * <ul>
     *   <li>
     *     SystemGlobalsListener
     *     <ul>
     *       <li><i>SystemGlobals.properties</i></li>
     *       <li><i>quartz-jforum.properties</i></li>
     *       <li>if exists: <i>jforum_custom.config</i></li>
     *     </ul>
     *   </li>
     *   <li>
     *     QueriesFileListener
     *     <ul>
     *       <li><i>generic_queries.sql</i></li>
     *       <li><i>&lt;database_name&gt;.sql</i></li>
     *     </ul>
     *   </li>
     * </ul>      
     */
    public static void listenForChanges()
    {
        final int fileChangesDelay = SystemGlobals.getIntValue(ConfigKeys.FILECHANGES_DELAY);

        if (fileChangesDelay > 0) {
            // Queries
            FileMonitor.getInstance().addFileChangeListener(new QueriesFileListener(),
                                                            SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_GENERIC), fileChangesDelay);

            FileMonitor.getInstance().addFileChangeListener(new QueriesFileListener(),
                                                            SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER), fileChangesDelay);

            // System Properties
            FileMonitor.getInstance().addFileChangeListener(new SystemGlobalsListener(),
                                                            SystemGlobals.getValue(ConfigKeys.DEFAULT_CONFIG), fileChangesDelay);

            if (new File(SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG)).exists()) {
                FileMonitor.getInstance().addFileChangeListener(new SystemGlobalsListener(),
                                                                SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG), fileChangesDelay);
            }

            // Quartz Properties
            FileMonitor.getInstance().addFileChangeListener(new SystemGlobalsListener(),
                                                            SystemGlobals.getValue(ConfigKeys.QUARTZ_CONFIG), fileChangesDelay);
        }
    }

    public static void loadDaoImplementation()
    {
        // Start the dao.driver implementation
        final String driver = SystemGlobals.getValue(ConfigKeys.DAO_DRIVER);

        LOGGER.info("Loading data access driver " + driver);

        try {
            final Class<?> clazz = Class.forName(driver);
            final DataAccessDriver dad = (DataAccessDriver)clazz.newInstance();
            DataAccessDriver.init(dad);
        }
        catch (Exception e) {
            throw new ForumException(e);
        }
    }

    public static void startCacheEngine()
    {
        try {
            final String cacheImpl = SystemGlobals.getValue(ConfigKeys.CACHE_IMPLEMENTATION);
            LOGGER.info("Using cache engine: " + cacheImpl);

            cache = (CacheEngine)Class.forName(cacheImpl).newInstance();
            cache.init();

            final String str = SystemGlobals.getValue(ConfigKeys.CACHEABLE_OBJECTS);
            if (str == null || str.trim().equals("")) {
                LOGGER.warn("Cannot find Cacheable objects to associate the cache engine instance.");
                return;
            }

            final String[] cacheableObjects = str.split(",");
            for (int i = 0; i < cacheableObjects.length; i++) {
                LOGGER.info("Creating an instance of " + cacheableObjects[i].trim());
                final Object obj = Class.forName(cacheableObjects[i].trim()).newInstance();

                if (obj instanceof Cacheable) {
                    ((Cacheable)obj).setCacheEngine(cache);
                }
                else {
                    LOGGER.error(cacheableObjects[i] + " is not an instance of net.jforum.cache.Cacheable");
                }
            }
        }
        catch (Exception e) {
            throw new CacheEngineStartupException("Error while starting the cache engine", e);
        }
    }

    public static void stopCacheEngine()
    {
        if (cache != null) {
            cache.stop();
        }
    }

    public static void startSearchIndexer()
    {
        SearchFacade.init();
    }

    /**
     * Init a Job who will send e-mails to the all users with a summary of posts...
     * @throws SchedulerException
     * @throws IOException
     */
    public static void startSummaryJob() throws SchedulerException {
        SummaryScheduler.startJob();
    }

    public static void startPop3Integration() throws SchedulerException
    {
        POPJobStarter.startJob();
    }

    private ConfigLoader() {}
}
