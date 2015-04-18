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
 * Created on 29/01/2006 - 12:19:11
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.http.HttpServletResponse;

import net.jforum.context.ForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;

/**
 * Data execution context. 
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class JForumExecutionContext
{
    private static ThreadLocal<JForumExecutionContext> userData = new ThreadLocal<JForumExecutionContext>();
	private static final Logger LOGGER = Logger.getLogger(JForumExecutionContext.class);
	private static Configuration templateConfig;
	private static final BeansWrapper beansWrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();

	private transient Connection conn;
    private ForumContext forumContext;
    private transient final SimpleHash context = new SimpleHash(beansWrapper); 
    private transient String redirectTo;
    private String contentType;
    private transient boolean isCustomContent;
    private transient boolean enableRollback;

	/**
	 * Gets the execution context.
	 * @return JForumExecutionContext
	 */
	public static JForumExecutionContext get()
	{
		JForumExecutionContext executionContext = (JForumExecutionContext)userData.get();

		if (executionContext == null) {
			executionContext = new JForumExecutionContext();
			userData.set(executionContext);
		}

		return executionContext;
	}

	/**
	 * Checks if there is an execution context already set
	 * @return <code>true</code> if there is an execution context
	 * @see #get()
	 */
	public static boolean exists()
	{
		return (userData.get() != null);
	}

	/**
	 * Sets the default template configuration 
	 * @param config The template configuration to set
	 */
	public static void setTemplateConfig(final Configuration config)
	{
		templateConfig = config;
	}

	/**
	 * Gets a reference to the default template configuration settings.
	 * @return The template configuration instance
	 */
	public static Configuration getTemplateConfig()
	{
		return templateConfig;
	}

	/**
	 * Sets the execution context
	 * @param executionContext JForumExecutionContext
	 */
	public static void set(final JForumExecutionContext executionContext)
	{
		userData.set(executionContext);
	}

	/**
	 * Sets a connection
	 * @param conn The connection to use
	 */
	public void setConnection(final Connection conn)
	{
		this.conn = conn;
	}

	/**
	 * Gets the current thread's connection
	 * @return Connection
	 */
	public static Connection getConnection() 
	{
		return getConnection(true);
	}

	public static Connection getConnection(final boolean validate)
	{
		final JForumExecutionContext executionContext = get();
		Connection conn = executionContext.conn;

		if (validate && conn == null) {
			conn = DBConnection.getImplementation().getConnection();

			try {
				conn.setAutoCommit(!SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS));
			}
			catch (Exception e) {
                //catch error autocommit
				LOGGER.error(e);
            }

			executionContext.setConnection(conn);
			set(executionContext);
		}
	    
		return conn; 
	}

    public static ForumContext getForumContext()
    {
        return ((JForumExecutionContext)userData.get()).forumContext;
    }

    public void setForumContext(final ForumContext forumContext)
    {
        this.forumContext = forumContext;
    }

    /**
	 * Gets the current thread's request
	 * @return WebContextRequest
	 */
	public static RequestContext getRequest() {
		return getForumContext().getRequest();
	}

	/**
	 * Gets the current thread's response
	 * @return HttpServletResponse
	 */
	public static ResponseContext getResponse() {
		return getForumContext().getResponse();
	}

	/**
	 * Gets the current thread's template context
	 * @return SimpleHash
	 */
	public static SimpleHash getTemplateContext() {
		return ((JForumExecutionContext)userData.get()).context;
	}

	/**
	 * Gets the current thread's <code>DataHolder</code> instance
     * @param redirect String
     */
	public static void setRedirect(final String redirect) {
		((JForumExecutionContext)userData.get()).redirectTo = redirect;
	}

	/**
	 * Sets the content type for the current http response.
	 * @param cntType String
	 */
	public static void setContentType(final String cntType) {
		((JForumExecutionContext)userData.get()).contentType = cntType;
	}

	/**
	 * Gets the content type for the current request.
	 * @return String
	 */
	public static String getContentType()
	{
		return ((JForumExecutionContext)userData.get()).contentType;
	}

	/**
	 * Gets the URL to redirect to, if any.
	 * @return The URL to redirect, of <code>null</code> if none.
	 */
	public static String getRedirectTo()
	{
		final JForumExecutionContext executionContext = (JForumExecutionContext)userData.get();
		return (executionContext == null ? null : executionContext.redirectTo);
	}

	/**
	 * Marks the request to use a binary content-type.
	 * @param enable boolean
	 */
	public static void enableCustomContent(final boolean enable) {
		((JForumExecutionContext)userData.get()).isCustomContent = enable;
	}

	/**
	 * Checks if the current request is binary
	 * @return <code>true</code> if the content type for the current request is 
	 * any binary data.
	 */
	public static boolean isCustomContent()
	{
		return ((JForumExecutionContext)userData.get()).isCustomContent;
	}

	/**
	 * Forces the request to not commit the connection.
	 */
	public static void enableRollback() {
		((JForumExecutionContext)userData.get()).enableRollback = true;
	}

	/**
	 * Check if commit is disabled or not for the current request.
	 * @return <code>true</code> if a commit should NOT be made
	 */
	public static boolean shouldRollback() {
		return ((JForumExecutionContext)userData.get()).enableRollback;
	}

    /**
     * Send UNAUTHORIZED to the browser and ask user to login via basic authentication
     */
	public static void requestBasicAuthentication()  
	{
		getResponse().addHeader("WWW-Authenticate", "Basic realm=\"JForum\"");

		try {
			getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
		catch (IOException e) {
			throw new ForumException(e);
		}

		enableCustomContent(true);
    }

	/**
	 * Finishes the execution context
	 */
	public static void finish()
	{
		final Connection conn = JForumExecutionContext.getConnection(false);

		if (conn != null) {
			if (SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS)) {
				if (JForumExecutionContext.shouldRollback()) {
					try {
						conn.rollback();
					}
					catch (Exception e) {
						LOGGER.error("Error while rolling back a transaction", e);
					}
				}
				else {
					try {
						conn.commit();
					}
					catch (Exception e) {
						LOGGER.error("Error while commiting a transaction", e);
					}
				}
			}

			try {
				DBConnection.getImplementation().releaseConnection(conn);
			}
			catch (Exception e) {
				LOGGER.error("Error while releasing the connection : " + e, e);
			}
		}

		userData.set(null);
	}

	/**
	 * Creating SimpleHash objects in one central place lets us confine
	 * the FreeMarker version information to this class.
	 */
	public static SimpleHash newSimpleHash() {
		return new SimpleHash(beansWrapper);
	}
}
