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
 * Created on 20.08.2006 18:52:22 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.context.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import net.jforum.context.ResponseContext;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author SergeMaslyukov
 * @version $Id$
 */
public class WebResponseContext implements ResponseContext
{
	private final HttpServletResponse response;

	public WebResponseContext(final HttpServletResponse response)
	{
		this.response = response;
	}

	public void setContentLength(final int len)
	{
		response.setContentLength(len);
	}

	public boolean containsHeader(final String name)
	{
		return response.containsHeader(name);
	}

	public void setHeader(final String name, final String value)
	{
		response.setHeader(name, value);
	}

	public void addCookie(final Cookie cookie)
	{
		response.addCookie(cookie);
	}

	public String encodeRedirectURL(final String url)
	{
		return response.encodeRedirectURL(url);
	}

	public void sendRedirect(final String location) throws IOException
	{
		String newLocation = location;
		if (SystemGlobals.getBoolValue(ConfigKeys.REDIRECT_ABSOLUTE_PATHS)) {
			final URI uri = URI.create(location);
			
			if (!uri.isAbsolute()) {
				newLocation = SystemGlobals.getValue(ConfigKeys.REDIRECT_BASE_URL) + location;
			}
		}
		
		response.sendRedirect(newLocation);
	}

	public String getCharacterEncoding()
	{
		return response.getCharacterEncoding();
	}

	public void setContentType(final String type)
	{
		response.setContentType(type);
	}

	public ServletOutputStream getOutputStream() throws IOException
	{
		return response.getOutputStream();
	}

	public PrintWriter getWriter() throws IOException
	{
		return response.getWriter();
	}

	public String encodeURL(final String url)
	{
		return response.encodeURL(url);
	}

	public void sendError(final int statusCode) throws IOException
	{
		response.sendError(statusCode);
	}

	public void addHeader(final String name, final String value)
	{
		response.addHeader(name, value);
	}
}
