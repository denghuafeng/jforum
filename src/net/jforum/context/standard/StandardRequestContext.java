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
 * Created on 26/08/2006 21:56:05
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.context.standard;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

import javax.servlet.http.Cookie;

import net.jforum.context.RequestContext;
import net.jforum.context.SessionContext;

/**
 * Request context non-dependent of HTTP 
 * @author Rafael Steil
 * @version $Id$
 */
public class StandardRequestContext implements RequestContext
{
	private transient final Hashtable<String, Object> data;
	private transient final SessionContext sessionContext;
	
	public StandardRequestContext()
	{
		this.data = new Hashtable<String, Object>();
		this.sessionContext = new StandardSessionContext();
	}
	
	/**
	 * @see net.jforum.context.RequestContext#addParameter(java.lang.String, java.lang.Object)
	 */
	public void addParameter(final String name, final Object value)
	{
		if (this.data.contains(name)) {
			this.data.remove(name);
		}
		
		this.data.put(name, value);
	}
	
	/**
	 * @see net.jforum.context.RequestContext#addOrReplaceParameter(java.lang.String, java.lang.Object)
	 */
	public void addOrReplaceParameter(final String name, final Object value) 
	{
		this.addParameter(name, value);
	}

	/**
	 * @see net.jforum.context.RequestContext#getAction()
	 */
	public String getAction()
	{
		
		return null;
	}

	/**
	 * @see net.jforum.context.RequestContext#getAttribute(java.lang.String)
	 */
	public Object getAttribute(final String name)
	{
		return this.getParameter(name);
	}

	/**
	 * This method will always return null
	 */
	public String getContextPath()
	{
		return null;
	}

	/**
	 * This method will always return null
	 */
	public Cookie[] getCookies()
	{
		return new Cookie[0];
	}

	/**
	 * This method will always return null
	 */
	public String getHeader(final String name)
	{
		return null;
	}

	/**
	 * @see net.jforum.context.RequestContext#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(final String parameter)
	{
		return Integer.parseInt(this.getParameter(parameter));
	}

	/**
	 * @see net.jforum.context.RequestContext#getModule()
	 */
	public String getModule()
	{
		
		return null;
	}

	/**
	 * @see net.jforum.context.RequestContext#getObjectParameter(java.lang.String)
	 */
	public Object getObjectParameter(final String parameter)
	{
		return this.data.get(parameter);
	}

	/**
	 * @see net.jforum.context.RequestContext#getParameter(java.lang.String)
	 */
	public String getParameter(final String name)
	{
		final Object value = this.data.get(name);
		return value == null ? null : value.toString();
	}

	/**
	 * @see net.jforum.context.RequestContext#getParameterNames()
	 */
	public Enumeration<String> getParameterNames()
	{
		return this.data.keys();
	}

	/**
	 * This method will always return null;
	 */
	public String[] getParameterValues(final String name)
	{
		return new String[0];
	}

	/**
	 * This method will always return null
	 */
	public String getQueryString()
	{
		return null;
	}

	/**
	 * @see net.jforum.context.RequestContext#getRemoteAddr()
	 */
	public String getRemoteAddr()
	{
		
		return null;
	}

	/**
	 * This method will always return null
	 */
	public String getRemoteUser()
	{
		return null;
	}

	/**
	 * This method will always return null
	 */
	public String getRequestURI()
	{
		return null;
	}

	/**
	 * This method will always return null
	 */
	public String getScheme()
	{
		return null;
	}

	/**
	 * This method will always return null
	 */
	public String getServerName()
	{
		return null;
	}

	/**
	 * This method will always return 0
	 */
	public int getServerPort()
	{
		return 0;
	}

	/**
	 * @see net.jforum.context.RequestContext#getSessionContext()
	 */
	public SessionContext getSessionContext()
	{
		return this.sessionContext;
	}

	/**
	 * This method is equal to {@link #getSessionContext()}
	 */
	public SessionContext getSessionContext(final boolean create)
	{
		return this.getSessionContext();
	}

	/**
	 * @see net.jforum.context.RequestContext#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(final String name)
	{
		this.data.remove(name);
	}

	/**
	 * This method is equal to {@link #addParameter(String, Object)}
	 */
	public void setAttribute(final String name, final Object obj)
	{
		this.addParameter(name, obj);
	}

	/**
	 * This method does nothing 
	 */
	public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
		// Empty method
	}

	@SuppressWarnings("unchecked")
	public Enumeration<Locale> getLocales() {
		return (Enumeration<Locale>) Collections.enumeration(Collections.EMPTY_LIST);
		
	}

	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException("this method only supported for web layer");
	}
}
