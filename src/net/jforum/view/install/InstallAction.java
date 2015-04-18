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
 * Created on 27/08/2004 - 18:15:54
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import net.jforum.Command;
import net.jforum.ConfigLoader;
import net.jforum.DBConnection;
import net.jforum.DataSourceConnection;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.SimpleConnection;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.PostDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.DatabaseException;
import net.jforum.exceptions.ForumException;
import net.jforum.util.DbUtils;
import net.jforum.util.I18n;
import net.jforum.util.Hash;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * JForum Web Installer.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class InstallAction extends Command
{
    private static final Logger LOGGER = Logger.getLogger(InstallAction.class);

    private static final String POOLED_CONNECTION = net.jforum.PooledConnection.class.getName();
    private static final String SIMPLE_CONNECTION = net.jforum.SimpleConnection.class.getName();
    private static final String DATASOURCE_CONNECTION = net.jforum.DataSourceConnection.class.getName();

    public void welcome()
    {
        this.checkLanguage();

        this.context.put("language", this.getFromSession("language"));
        this.context.put("database", this.getFromSession("database"));
        this.context.put("dbhost", this.getFromSession("dbHost"));
        this.context.put("dbuser", this.getFromSession("dbUser"));
        this.context.put("dbname", this.getFromSession("dbName"));
        this.context.put("dbport", this.getFromSession("dbPort"));
        this.context.put("dbpasswd", this.getFromSession("dbPassword"));
        this.context.put("dbencoding", this.getFromSession("dbEncoding"));
        this.context.put("use_pool", this.getFromSession("usePool"));
        this.context.put("forumLink", this.getFromSession("forumLink"));
        this.context.put("siteLink", this.getFromSession("siteLink"));
        this.context.put("dbdatasource", this.getFromSession("dbdatasource"));

        this.setTemplateName(TemplateKeys.INSTALL_WELCOME);
    }

    private void checkLanguage()
    {
        String lang = this.request.getParameter("l");

        if (lang == null) {
            for (Enumeration<Locale> locales = this.request.getLocales();
                locales.hasMoreElements();) {
                lang = locales.nextElement().toString();			      
                if (I18n.languageExists(lang)) { 
                   break;
                }
            }
        }
        LOGGER.info("lang="+lang);
        if (lang != null) {
	        I18n.load(lang);

	        final UserSession userSession = new UserSession();
	        userSession.setLang(lang);
	        userSession.setStartTime(new Date(System.currentTimeMillis()));

	        SessionFacade.add(userSession);
	        this.addToSessionAndContext("language", lang);
        }
    }

    private String getFromSession(final String key)
    {
        return (String)this.request.getSessionContext().getAttribute(key);
    }

    private void error()
    {
        this.setTemplateName(TemplateKeys.INSTALL_ERROR);
    }

    public void doInstall()
    {
        if (!this.checkForWritableDir()) {
            return;
        }

        this.removeUserConfig();

        Connection conn = null;

        if (!"passed".equals(this.getFromSession("configureDatabase"))) {
            LOGGER.info("Going to configure the database...");
            conn = this.configureDatabase();
        }
        if (conn == null) {
            this.context.put("message", I18n.getMessage("Install.databaseError"));
            this.error();
            return;
        }

        // Database Configuration is OK
        this.addToSessionAndContext("configureDatabase", "passed");
        LOGGER.info("Database configuration is OK");

        boolean dbError = false;

        try {
            //this.setupAutoCommit(conn);

            if (!"passed".equals(this.getFromSession("createTables")) && !this.createTables(conn)) {
                this.context.put("message", I18n.getMessage("Install.createTablesError"));
                dbError = true;
                this.error();
                return;
            }

            // Create tables is OK
            this.addToSessionAndContext("createTables", "passed");
            LOGGER.info("Table creation is OK");

            LOGGER.info("Going to populate the database tables ...");
            this.setupAutoCommit(conn); 
            if (!"passed".equals(this.getFromSession("importTablesData")) && !this.importTablesData(conn)) {
                this.context.put("message", I18n.getMessage("Install.importTablesDataError"));
                dbError = true;
                this.error();
                return;
            }

            // Dump is OK
            this.addToSessionAndContext("importTablesData", "passed");
            LOGGER.info("Table data population is OK");

            // Set user.hash.sequence before calling updateAdminPassword()
            SystemGlobals.setValue(ConfigKeys.USER_HASH_SEQUENCE, Hash.md5(this.getFromSession("dbPassword")
                    + System.currentTimeMillis()));
            LOGGER.info("Generated user.hash.sequence = " + SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE));
            
            if (!this.updateAdminPassword(conn)) {
                this.context.put("message", I18n.getMessage("Install.updateAdminError"));
                dbError = true;
                this.error();
                return;
            }
        }
        finally {
			try {
				if (dbError) {
					conn.rollback();
				}
				else {
					conn.commit();
				}
				conn.close();
			}
			catch (SQLException e) { LOGGER.error(e); }
        }

        JForumExecutionContext.setRedirect(this.request.getContextPath() + "/install/install"
            + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION)
            + "?module=install&action=finished");
    }

    private void setupAutoCommit(final Connection conn)
    {
        try {
            conn.setAutoCommit(false);
        }
        catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void removeUserConfig()
    {
        final File file = new File(SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG));

        if (file.exists() && file.canWrite()) {
            try {
                file.delete();
            }
            catch (Exception e) {
                LOGGER.info(e.toString());
            }
        }
    }

    public void finished() 
    {
        this.setTemplateName(TemplateKeys.INSTALL_FINISHED);

        this.context.put("clickHere", I18n.getMessage("Install.clickHere"));
        this.context.put("forumLink", this.getFromSession("forumLink"));

        String lang = this.getFromSession("language");

        if (lang == null) {
            lang = "en_US";
        }

        this.context.put("lang", lang);

        this.fixModulesMapping();
        this.configureSystemGlobals();

        SessionFacade.remove(this.request.getSessionContext().getId());
    }

    private void fixModulesMapping()
    {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            // Modules Mapping
            final String modulesMapping = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/modulesMapping.properties";

            if (new File(modulesMapping).canWrite()) {
                final Properties properties = new Properties();
                fis = new FileInputStream(modulesMapping);
                properties.load(fis);

                if (properties.containsKey("install")) {
                    properties.remove("install");

                    fos = new FileOutputStream(modulesMapping);

                    properties.store(fos, "Modified by JForum Installer");
                    ConfigLoader.loadModulesMapping(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR));
                }

                this.addToSessionAndContext("mappingFixed", "true");
            }
        }
        catch (Exception e) {
            LOGGER.warn("Error while working on modulesMapping.properties: " + e);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }

            if (fos != null) {
                try { fos.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }
        }
    }

    private void configureSystemGlobals()
    {
        SystemGlobals.setValue(ConfigKeys.FORUM_LINK, this.getFromSession("forumLink"));
        SystemGlobals.setValue(ConfigKeys.HOMEPAGE_LINK, this.getFromSession("siteLink"));
        SystemGlobals.setValue(ConfigKeys.I18N_DEFAULT, this.getFromSession("language"));
        SystemGlobals.setValue(ConfigKeys.INSTALLED, "true");

        SystemGlobals.saveInstallation();
    }

    private boolean importTablesData(final Connection conn)
    {
        try
        {
            boolean status = true;
            final boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            final String dbType = this.getFromSession("database");

            final List<String> statements = ParseDBDumpFile.parse(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)
                                                                  + "/database/"
                                                                  + dbType
                                                                  + "/" + dbType + "_data_dump.sql");

            for (final Iterator<String> iter = statements.iterator(); iter.hasNext();) {
                String query = iter.next();

                if (query == null || "".equals(query.trim())) {
                    continue;
                }

                query = query.trim();
                if (query.indexOf("\\n") != -1) {
                    query = query.replace("\\n", "\n");
                }

                final Statement stmt = conn.createStatement();

                try {
                    if (query.startsWith("UPDATE") || query.startsWith("INSERT") || query.startsWith("SET")) {
                        stmt.executeUpdate(query);
                    }
                    else if (query.startsWith("SELECT")) {
                        ResultSet rs = stmt.executeQuery(query);
                        rs.close();
                    }
                    else {
                        throw new SQLException("Invalid query: " + query);
                    }
                }
                catch (SQLException ex) {
                    status = false;
                    conn.rollback();
                    LOGGER.error("Error importing data for " + query + ": " + ex, ex);
                    this.context.put("exceptionMessage", ex.getMessage() + "\n" + query);
                    break;
                }
                finally {
                    stmt.close();
                }
            }
            // handle blob post
            if ("oracle".equals(dbType)) {
                storeWelcomeMessage(conn);
            }

            conn.setAutoCommit(autoCommit);
            return status;
        }
        catch (SQLException e)
        {
            throw new ForumException(e);
        }
        catch (IOException e)
        {
            throw new ForumException(e);
        }
    }

    private boolean createTables(final Connection conn)
    {

        LOGGER.info("Going to create tables...");
        final String dbType = this.getFromSession("database");

        if ("postgresql".equals(dbType) || "oracle".equals(dbType)) {
            // This should be in a separate transaction block; otherwise, an empty database will fail.
            this.dropOracleOrPostgreSQLTables(dbType, conn);
        }
        try { 
            boolean status = true;
            final boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            final List<String> statements = ParseDBStructFile.parse(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)
                                                                    + "/database/"
                                                                    + dbType
                                                                    + "/" + dbType + "_db_struct.sql");


            for (final Iterator<String> iter = statements.iterator(); iter.hasNext(); ) {
                final String query = iter.next();

                if (query == null || "".equals(query.trim())) {
                    continue;
                }

                Statement stmt = null;

                try {
                    stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                }
                catch (SQLException ex) {
                    status = false;

                    LOGGER.error("Error executing query: " + query + ": " + ex, ex);
                    this.context.put("exceptionMessage", ex.getMessage() + "\n" + query);

                    break;
                }
                finally {
                    DbUtils.close(stmt);
                }
            }
            conn.setAutoCommit(autoCommit);
            return status;
        }
        catch (Exception e)
        {
            throw new ForumException(e);
        }
    }

    private void dropOracleOrPostgreSQLTables(final String dbName, final Connection conn)
    {
        Statement stmt = null;

        try {
            final boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            final List<String> statements = ParseDBStructFile.parse(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)
                                                                    + "/database/" + dbName + "/" + dbName + "_drop_tables.sql");

            this.setupAutoCommit(conn);
            for (final Iterator<String> iter = statements.iterator(); iter.hasNext(); ) {
                try {
                    final String query = iter.next();

                    if (query == null || "".equals(query.trim())) {
                        continue;
                    }

                    stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                    stmt.close();
                }
                catch (SQLException e) {
                    LOGGER.warn("IGNORE: " + e.toString());
                }
            }
            conn.setAutoCommit(autoCommit);
        }
        catch (SQLException e) {
            LOGGER.error(e.toString(), e);
        }
        finally {
            DbUtils.close(stmt);
        }
    }

    private boolean checkForWritableDir()
    {
        final boolean canWriteToWebInf = this.canWriteToWebInf();
        final boolean canWriteToLuceneIndex = this.canWriteToLuceneIndex();

        if (!canWriteToWebInf || !canWriteToLuceneIndex) {
            if (!canWriteToWebInf) {
                this.context.put("message", I18n.getMessage("Install.noWritePermission"));
            }
            else if (!canWriteToLuceneIndex) {
                this.context.put("message", I18n.getMessage("Install.noWritePermissionLucene", 
                                                            new Object[] { SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH) } ));
            }

            this.context.put("tryAgain", true);
            this.error();
            return false;
        }

        return true;
    }

    private boolean canWriteToWebInf()
    {
        return new File(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/modulesMapping.properties").canWrite();
    }

    private boolean canWriteToLuceneIndex()
    {
        final File file = new File(SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH));

        if (!file.exists()) {
            return file.mkdir();
        }

        return file.canWrite();
    }

    private void handleDatabasePort(final Properties properties, final String port)
    {
        final String portKey = ":${database.connection.port}";
        String connectionString = properties.getProperty(ConfigKeys.DATABASE_CONNECTION_STRING);

        if (StringUtils.isBlank(port)) {
            final int index = connectionString.indexOf(portKey);

            if (index > -1) {
                if (connectionString.charAt(index - 1) == '\\') {
                    connectionString = StringUtils.remove(connectionString, "\\" + portKey);
                }
                else {
                    connectionString = StringUtils.remove(connectionString, portKey);
                }
            }
        }
        else if (connectionString.indexOf(portKey) == -1) {
            final String hostKey = "${database.connection.host}";
            connectionString = StringUtils.replace(connectionString, hostKey, hostKey + portKey);
        }

        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_STRING, connectionString);
    }

    private void configureJDBCConnection()
    {
        final String username = this.getFromSession("dbUser");
        final String password = this.getFromSession("dbPassword");
        final String dbName = this.getFromSession("dbName");
        final String host = this.getFromSession("dbHost");
        final String type = this.getFromSession("database");
        final String encoding = this.getFromSession("dbEncoding");
        final String port = this.getFromSession("dbPort");

        final String dbConfigFilePath = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) 
            + "/database/" + type + "/" + type + ".properties";

        final Properties properties = new Properties();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(dbConfigFilePath);
            properties.load(fis);
        }
        catch (IOException e) {
            throw new ForumException(e);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }
        }

        this.handleDatabasePort(properties, port);

        // Write database information to the respective file
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_HOST, host);
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_USERNAME, username);
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_PASSWORD, password);
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_DBNAME, dbName);
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_ENCODING, encoding);
        properties.setProperty(ConfigKeys.DATABASE_CONNECTION_PORT, port);
        properties.setProperty(ConfigKeys.DATABASE_DRIVER_NAME, type);

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(dbConfigFilePath);
            properties.store(fos, null);
        }
        catch (Exception e) {
            LOGGER.warn("Error while trying to write to " + type + ".properties: " + e);
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) { LOGGER.error(e); }
            }
        }

        updateSystemGlobals(properties);
    }

	private void updateSystemGlobals(final Properties properties)
	{
		// Proceed to SystemGlobals / jforum-custom.conf configuration
        for (final Enumeration<Object> e = properties.keys(); e.hasMoreElements(); ) {
            final String key = (String)e.nextElement();
            final String value = properties.getProperty(key);

            SystemGlobals.setValue(key, value);

            LOGGER.info("Updating key " + key + " with value " + value);
        }
	}

    private void configureDataSourceConnection()
    {
        final String type = this.getFromSession("database");

        final String dbConfigFilePath = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) 
            + "/database/" + type + "/" + type + ".properties";

        final Properties properties = new Properties();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(dbConfigFilePath);
            properties.load(fis);
        }
        catch (IOException e) {
            throw new ForumException(e);
        }
        finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception e) { LOGGER.error(e.getMessage(), e); }
            }
        }

        updateSystemGlobals(properties);
    }

    private Connection configureDatabase()
    {
        final String database = this.getFromSession("database");
        final String connectionType = this.getFromSession("db_connection_type");
        String implementation;

        boolean isDatasource = false;

        if ("JDBC".equals(connectionType)) {
            implementation = "yes".equals(this.getFromSession("usePool")) && !"hsqldb".equals(database) 
                ? POOLED_CONNECTION
                    : SIMPLE_CONNECTION;
            this.configureJDBCConnection();
        }
        else {
            isDatasource = true;
            implementation = DATASOURCE_CONNECTION;
            SystemGlobals.setValue(ConfigKeys.DATABASE_DATASOURCE_NAME, this.getFromSession("dbdatasource"));
            this.configureDataSourceConnection();
        }

        SystemGlobals.setValue(ConfigKeys.DATABASE_CONNECTION_IMPLEMENTATION, implementation);

        Connection conn = null;
        try {
            DBConnection source;

            if (isDatasource) { 
                source = new DataSourceConnection();
            }
            else {
                source = new SimpleConnection(); 
            }

            source.init();

            conn = source.getConnection();
        }
        catch (Exception e) {
            LOGGER.warn("Error while trying to get a connection: " + e);
            this.context.put("exceptionMessage", e.getMessage());
        }

        return conn;
    }

    private boolean updateAdminPassword(final Connection conn)
    {
        LOGGER.info("Going to update the administrator's password");

        boolean status = false;

        PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement("UPDATE jforum_users SET user_password = ? WHERE username = 'Admin'");
            pstmt.setString(1, Hash.sha512(this.getFromSession("adminPassword")+SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE)));
            pstmt.executeUpdate();
            status = true;
        }
        catch (Exception e) {
            LOGGER.warn("Error while trying to update the administrator's password: " + e);
            this.context.put("exceptionMessage", e.getMessage());
        }
        finally {
            DbUtils.close(pstmt);
        }

        return status;
    }

    public void checkInformation() 
    {
        this.setTemplateName(TemplateKeys.INSTALL_CHECK_INFO);

        final String language = this.request.getParameter("language");
        final String database = this.request.getParameter("database");
        String dbHost = this.request.getParameter("dbhost");
        final String dbPort = this.request.getParameter("dbport");
        String dbUser = this.request.getParameter("dbuser");
        String dbName = this.request.getParameter("dbname");
        final String dbPassword = this.request.getParameter("dbpasswd");
        String dbEncoding = this.request.getParameter("dbencoding");
        String dbEncodingOther = this.request.getParameter("dbencoding_other");
        final String usePool = this.request.getParameter("use_pool");
        String forumLink = this.request.getParameter("forum_link");
        final String adminPassword = this.request.getParameter("admin_pass1");

        dbHost = this.notNullDefault(dbHost, "localhost");
        dbEncodingOther = this.notNullDefault(dbEncodingOther, "utf-8");
        dbEncoding = this.notNullDefault(dbEncoding, dbEncodingOther);
        forumLink = this.notNullDefault(forumLink, "http://localhost");
        dbName = this.notNullDefault(dbName, "jforum");

        if ("hsqldb".equals(database)) {
            dbUser = this.notNullDefault(dbUser, "sa");
        }

        this.addToSessionAndContext("language", language);
        this.addToSessionAndContext("database", database);
        this.addToSessionAndContext("dbHost", dbHost);
        this.addToSessionAndContext("dbPort", dbPort);
        this.addToSessionAndContext("dbUser", dbUser);
        this.addToSessionAndContext("dbName", dbName);
        this.addToSessionAndContext("dbPassword", dbPassword);
        this.addToSessionAndContext("dbEncoding", dbEncoding);
        this.addToSessionAndContext("usePool", usePool);
        this.addToSessionAndContext("forumLink", forumLink);
        this.addToSessionAndContext("siteLink", this.request.getParameter("site_link"));
        this.addToSessionAndContext("adminPassword", adminPassword);
        this.addToSessionAndContext("dbdatasource", this.request.getParameter("dbdatasource"));
        this.addToSessionAndContext("db_connection_type", this.request.getParameter("db_connection_type"));

        this.addToSessionAndContext("configureDatabase", null);
        this.addToSessionAndContext("createTables", null);
        this.addToSessionAndContext("importTablesData", null);

        this.context.put("canWriteToWebInf", this.canWriteToWebInf());
        this.context.put("moduleAction", "install_check_info.htm");
    }

    private void addToSessionAndContext(final String key, final String value)
    {
        this.request.getSessionContext().setAttribute(key, value);
        this.context.put(key, value);
    }

    private String notNullDefault(final String value, final String useDefault)
    {
        if (value == null || value.trim().equals("")) {
            return useDefault;
        }

        return value;
    }

    private void storeWelcomeMessage(final Connection conn) 
    {
        final String dbType = this.getFromSession("database");
        final String filePath = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)
            + "/database/"
            + dbType
            + "/" + dbType + "_data_jforum_posts_text_post_text_blob.txt";
        final File file = new File(filePath);
        String message = null;
        try {
            message = FileUtils.readFileToString(file);
            message = message.trim();
            if (message.indexOf("\\n") != -1) {
                message = message.replace("\\n", "\n");
            }
        } catch (IOException e) {
            LOGGER.error("Loading congratulation message failed", e);
        }
        if (message != null) {
            saveMessage(conn, "Welcome to JForum", message, Topic.TYPE_NORMAL);
        }
    }

    private void saveMessage(final Connection conn, final String subject, final String message, final int topicType) 
    {
        try {
            ConfigLoader.createLoginAuthenticator();
            ConfigLoader.loadDaoImplementation();

            SystemGlobals.loadQueries(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_GENERIC),
                                      SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));

            SystemGlobals.setValue(ConfigKeys.SEARCH_INDEXING_ENABLED, "false");

            final JForumExecutionContext executionContext = JForumExecutionContext.get();
            executionContext.setConnection(conn);
            JForumExecutionContext.set(executionContext);

            final User user = new User(2);
            final int forumId = 1;

            // Create topic
            final Topic topic = new Topic();
            topic.setPostedBy(user);
            topic.setTitle(subject);
            topic.setTime(new Date());
            topic.setType(topicType);
            topic.setForumId(forumId);

            final TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
            topicDao.addNew(topic);

            // Create post
            final Post post = new Post();
            post.setSubject(topic.getTitle());
            post.setTime(topic.getTime());
            post.setUserId(user.getId());
            post.setText(message);
            post.setForumId(topic.getForumId());
            post.setSmiliesEnabled(true);
            post.setHtmlEnabled(true);
            post.setBbCodeEnabled(true);
            post.setUserIp("127.0.0.1");
            post.setTopicId(topic.getId());

            final PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
            postDao.addNew(post);

            // Update topic
            topic.setFirstPostId(post.getId());
            topic.setLastPostId(post.getId());

            topicDao.update(topic);
            DataAccessDriver.getInstance().newUserDAO().incrementPosts(post.getUserId());

            // Update forum stats
            final ForumDAO forumDao = DataAccessDriver.getInstance().newForumDAO();
            forumDao.incrementTotalTopics(forumId, 1);
            forumDao.setLastPost(forumId, post.getId());
        }
        finally {
            SystemGlobals.setValue(ConfigKeys.SEARCH_INDEXING_ENABLED, "true");

            final JForumExecutionContext executionContext = JForumExecutionContext.get();
            executionContext.setConnection(null);
            JForumExecutionContext.set(executionContext);
        }
    }

    /** 
     * @see net.jforum.Command#list()
     */
    @Override
    public void list()
    {
        this.welcome();
    }

    /** 
     * @see net.jforum.Command#process(net.jforum.context.RequestContext, net.jforum.context.ResponseContext, freemarker.template.SimpleHash) 
     * @param request RequestContext     
     * @param response ResponseContext
     * @param context SimpleHash
     */
    @Override
    public Template process(final RequestContext request,
                            final ResponseContext response,
                            final SimpleHash context)  
    {
        this.setTemplateName(TemplateKeys.EMPTY);
        return super.process(request, response, context);
    }
}
