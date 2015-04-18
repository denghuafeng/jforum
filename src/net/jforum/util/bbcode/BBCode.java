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
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
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
 * This file creation date: 02/08/2003 / 02:23:50
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.util.bbcode;

import java.io.Serializable;

/**
 * Represents a UBB code. Each code is matched through a regular expression,
 * and can either be replace by a regexp replacement, or by specifying a Java class.
 * If the latter, then the handler class must implement the net.jforum.util.bbcode.Substitution interface.
 */

public class BBCode implements Serializable
{
	private static final long serialVersionUID = -8744755081519897386L;
	private String tagName = "";
	private String regex;
	private String replace;
	private String rssReplace = null;
	private String className;
	private boolean removeQuotes;
	private boolean alwaysProcess;
	private boolean isRegexpReplace = true;
	private String lockForSmilies = null;

	public BBCode() {}

	/**
	 * Gets the regex
	 */
	public String getRegex() 
	{
		return this.regex;
	}

	/**
	 * Gets the tag name
	 */
	public String getTagName() 
	{
		return this.tagName;
	}

	public boolean removeQuotes()
	{
		return this.removeQuotes;
	}

	/**
	 * Sets the regular expression associated to the tag
	 */
	public void setRegex(String regex) 
	{
		this.regex = regex;
	}

	/**
	 * Sets the tag name
	 */
	public void setTagName(String tagName) 
	{
		this.tagName = tagName;
	}

	/**
	 * Gets the replacement string
	 */
	public String getReplace() 
	{
		return this.replace;
	}

	/**
	 * Sets the replacement string, to be applied when matching the code
	 */
	public void setReplace(String replace) 
	{
		this.replace = replace;
		isRegexpReplace = true;
	}

	/**
	 * Gets the replacement string for RSS feeds
	 */
	public String getRssReplace() 
	{
		return this.rssReplace;
	}

	/**
	 * Sets the replacement string for RSS feeds, to be applied when matching the code
	 */
	public void setRssReplace(String rssReplace) 
	{
		this.rssReplace = rssReplace;
	}

	/**
	 * Gets the class name of the handler class.
	 */
	public String getClassName() 
	{
		return this.className;
	}

	/**
	 * Sets the class name of the handler class, to be applied when matching the code
	 */
	public void setClassName(String className) 
	{
		this.className = className;
		isRegexpReplace = false;
	}

	public void enableAlwaysProcess()
	{
		this.alwaysProcess = true;
	}

	public boolean alwaysProcess()
	{
		return this.alwaysProcess;
	}

	public void enableRemoveQuotes()
	{
		this.removeQuotes = true;
	}

	public boolean isRegexpReplace()
	{
		return isRegexpReplace;
	}

	public String getLockedForSmilies()
	{
		return this.lockForSmilies;
	}

	/**
	 * Sets the UBB code tag name inside of which this smilie should be ignored.
	 * That's either [url] or [img].
	 */
	public void setLockedForSmilies (String tag)
	{
		this.lockForSmilies = tag;
	}
}
