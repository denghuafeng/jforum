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
 * Created on 02/04/2004 - 20:31:35
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum.common;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import net.jforum.JForumExecutionContext;
import net.jforum.context.RequestContext;
import net.jforum.entities.User;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public final class ViewCommon
{
	private static final Logger LOGGER = Logger.getLogger(ViewCommon.class);

	/**
	 * Prepared the user context to use data pagination. 
	 * The following variables are set to the context:
	 * <p>
	 * 	<ul>
	 * 		<li> <i>totalPages</i> - total number of pages
	 * 		<li> <i>recordsPerPage</i> - how many records will be shown on each page
	 * 		<li> <i>totalRecords</i> - number of records fount
	 * 		<li> <i>thisPage</i> - the current page being shown
	 * 		<li> <i>start</i> - 
	 * 	</ul>
	 * </p>
	 * @param start int
	 * @param totalRecords  int
	 * @param recordsPerPage int
	 */
	public static void contextToPagination(final int start, final int totalRecords, final int recordsPerPage)
	{
		final SimpleHash context = JForumExecutionContext.getTemplateContext();

		context.put("totalPages", Double.valueOf(Math.ceil((double) totalRecords / (double) recordsPerPage)));
		context.put("recordsPerPage", Integer.valueOf(recordsPerPage));
		context.put("totalRecords", Integer.valueOf(totalRecords));
		context.put("thisPage", Double.valueOf(Math.ceil((double) (start + 1) / (double) recordsPerPage)));
		context.put("start", Integer.valueOf(start));
	}

	/**
	 * Prepares the template context to show the login page, using the current URI as return path.
	 * @return TemplateKeys.USER_LOGIN
	 */
	public static String contextToLogin() 
	{
		final RequestContext request = JForumExecutionContext.getRequest();

		String uri = request.getRequestURI();
        final String ctxPath = request.getContextPath() + "/";
        
        if (uri != null && uri.startsWith(ctxPath)) {
            uri = uri.substring(ctxPath.length());
        }
        
		final String query = request.getQueryString();
		final String returnPath = query == null ? uri : uri + "?" + query;

		return contextToLogin(returnPath);
	}

	/**
	 * Prepares the template context to show the login page, using "returnPath" as return path
	 * @param origReturnPath the URI to use as return path
	 * @return TemplateKeys.USER_LOGIN
	 */
	public static String contextToLogin(final String origReturnPath)
	{
		String returnPath = origReturnPath;
		JForumExecutionContext.getTemplateContext().put("returnPath", returnPath);

		if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			String redirect = SystemGlobals.getValue(ConfigKeys.SSO_REDIRECT);

			if (StringUtils.isNotEmpty(redirect)) {
				final URI redirectUri = URI.create(redirect);

				if (!redirectUri.isAbsolute()) {
					throw new ForumException("SSO redirect URL should start with a scheme");
				}

				try {
					returnPath = URLEncoder.encode( ViewCommon.getForumLink() + returnPath, "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					LOGGER.error(e);
				}

				if (redirect.indexOf('?') == -1) {
					redirect = new StringBuilder(redirect).append('?').toString();
				}
				else {
					redirect = new StringBuilder(redirect).append('&').toString();
				}

				redirect = new StringBuilder(redirect).append("returnUrl=").append(returnPath).toString();

				JForumExecutionContext.setRedirect(redirect);
			}
		}

		return TemplateKeys.USER_LOGIN;
	}

	/**
	 * Returns the initial page to start fetching records from.
	 *   
	 * @return The initial page number
	 */
	public static int getStartPage()
	{
		final String str = JForumExecutionContext.getRequest().getParameter("start");
		int start;

		if (StringUtils.isEmpty(str)) {
			start = 0;
		}
		else {
			start = Integer.parseInt(str);

			if (start < 0) {
				start = 0;
			}
		}

		return start;
	}

	/**
	 * Gets the forum base link.
	 * The returned link has a trailing slash
	 * @return The forum link, with the trailing slash
	 */
	public static String getForumLink()
	{
		String forumLink = SystemGlobals.getValue(ConfigKeys.FORUM_LINK);

		if (forumLink.charAt(forumLink.length() - 1) != '/') {
			forumLink = new StringBuilder(forumLink).append('/').toString();
		}

		return forumLink;
	}

	public static String toUtf8String(final String str)
	{
		final StringBuilder stringBuffer = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			final char chr = str.charAt(i);

			if ((chr >= 0) && (chr <= 255)) {
				stringBuffer.append(chr);
			}
			else {
				byte[] byt;

				try {
					byt = Character.toString(chr).getBytes("utf-8");
				}
				catch (Exception ex) {
					LOGGER.error(ex.getMessage(), ex);

					byt = new byte[0];
				}

				for (int j = 0; j < byt.length; j++) {
					int key = byt[j];

					if (key < 0) {
						key += 256;
					}

					stringBuffer.append('%').append(Integer.toHexString(key).toUpperCase());
				}
			}
		}

		return stringBuffer.toString();
	}

	/**
	 * Formats a date using the pattern defined in the configuration file.
	 * The key is the value of {@link net.jforum.util.preferences.ConfigKeys#DATE_TIME_FORMAT}
	 * @param date the date to format
	 * @return the string with the formatted date
	 */
	public static String formatDate(final Date date) 
	{
		final SimpleDateFormat sdf = new SimpleDateFormat(SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT), Locale.getDefault());
		return sdf.format(date);
	}

	public static String formatDateAsGmt(Date date) 
	{
		SimpleDateFormat df = new SimpleDateFormat(SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		df.setTimeZone(timeZone);
		return df.format(date);
	}

	/**
	 * Escapes &lt; by &amp;lt; and &gt; by &amp;gt;
	 * @param contents the string to parse
	 * @return the new string
	 */
	public static String espaceHtml(final String contents)
	{
		final StringBuilder stringBuffer = new StringBuilder(contents);

		replaceAll(stringBuffer, "<", "&lt");
		replaceAll(stringBuffer, ">", "&gt;");

		return stringBuffer.toString();
	}

	/**
	 * Replaces some string with another value
	 * @param stringBuffer the StrinbBuilder with the contents to work on
	 * @param what the string to be replaced
	 * @param with the new value
	 * @return the new string
	 */
	public static String replaceAll(StringBuilder stringBuffer, final String what, final String with)
	{
		int pos = stringBuffer.indexOf(what);

		while (pos > -1) {
			stringBuffer.replace(pos, pos + what.length(), with);
			pos = stringBuffer.indexOf(what);
		}

		return stringBuffer.toString();
	}

	/**
	 * Parse the user's signature, to make it proper to visualization
	 * @param user the user instance
	 */
	public static void prepareUserSignature(final User user)
	{
		if (user.getSignature() != null) {
			final StringBuilder stringBuffer = new StringBuilder(user.getSignature());

			replaceAll(stringBuffer, "\n", "<br />");

			user.setSignature(stringBuffer.toString());
			user.setSignature(PostCommon.prepareTextForDisplayExceptCodeTag(user.getSignature(), true, true));
		}
	}

	private ViewCommon() {}
}
