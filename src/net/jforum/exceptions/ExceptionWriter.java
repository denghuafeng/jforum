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
 * /*
 * Created on Feb 3, 2005 5:15:34 PM
  * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class ExceptionWriter
{
	private static final Logger LOGGER = Logger.getLogger(ExceptionWriter.class);
	
	public void handleExceptionData(final Throwable throwable, final Writer wtr, final RequestContext request)
	{
		StringWriter strWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(strWriter);
		throwable.printStackTrace(writer);	
		
		final String currentUrl = this.extractCurrentUrl(request);
		
		writer.write(currentUrl != null ? currentUrl : "null");
		writer.close();
		
		try {
			LOGGER.error(strWriter);

			String message = "";
			Throwable cause = throwable.getCause();
			
			while (cause != null) {
				message = cause.toString();
				cause = cause.getCause();
			}
			
			if (message == null || message.equals("")) {
				message = throwable.getMessage();
			}
			
			if (message == null || message.equals("")) {
				message = throwable.toString();
			}

			final boolean canViewStackTrace = !SystemGlobals.getBoolValue(ConfigKeys.STACKTRACE_MODERATORS_ONLY)
				|| (SessionFacade.isLogged() && SessionFacade.getUserSession().isModerator());
			
			final String filter = "[<>]";
			String stackTrace = canViewStackTrace
				? strWriter.toString()
				: "Only moderators can view stack trace.";
			
			stackTrace = stackTrace.replaceAll(filter, "");
			message = message.replaceAll(filter, "");
			
			final SimpleHash templateContext = JForumExecutionContext.getTemplateContext();
			
			templateContext.put("stackTrace", stackTrace);
			templateContext.put("message", message);

			final Template template = JForumExecutionContext.getTemplateConfig().getTemplate("exception.html", SystemGlobals.getValue(ConfigKeys.ENCODING));
			template.process(templateContext, wtr);
		}
		catch (Exception e) {
			strWriter = new StringWriter();
			writer = new PrintWriter(strWriter);
			e.printStackTrace(writer);
			writer.close();
			LOGGER.error(strWriter);
		}
	}
	
	private String extractCurrentUrl(final RequestContext request)
	{
		String url = null;
		if (request != null) {
			final StringBuilder sb = new StringBuilder().append("\nURL is: ").append(request.getRequestURI()); 
			if (request.getQueryString() != null) {
				sb.append('?').append(request.getQueryString());				
			}
			url = sb.toString();
		}
		return url;
	}
}
