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
 * Created on 21/08/2006 21:23:29
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.entities;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class MailIntegration
{
	private int forumId;
	private int popPort;
	private boolean ssl;
	private String forumEmail;
	private String popHost;
	private String popUsername;
	private String popPassword;
	
	/**
	 * @return the forumId
	 */
	public int getForumId()
	{
		return this.forumId;
	}
	
	/**
	 * @return the popHost
	 */
	public String getPopHost()
	{
		return this.popHost;
	}
	
	/**
	 * @return the popPassword
	 */
	public String getPopPassword()
	{
		return this.popPassword;
	}
	
	/**
	 * @return the popPort
	 */
	public int getPopPort()
	{
		return this.popPort;
	}
	
	/**
	 * @return the popUsername
	 */
	public String getPopUsername()
	{
		return this.popUsername;
	}
	
	/**
	 * @param forumId the forumId to set
	 */
	public void setForumId(final int forumId)
	{
		this.forumId = forumId;
	}
	
	/**
	 * @param popHost the popHost to set
	 */
	public void setPopHost(final String popHost)
	{
		this.popHost = popHost;
	}
	
	/**
	 * @param popPassword the popPassword to set
	 */
	public void setPopPassword(final String popPassword)
	{
		this.popPassword = popPassword;
	}
	
	/**
	 * @param popPort the popPort to set
	 */
	public void setPopPort(final int popPort)
	{
		this.popPort = popPort;
	}
	
	/**
	 * @param popUsername the popUsername to set
	 */
	public void setPopUsername(final String popUsername)
	{
		this.popUsername = popUsername;
	}

	/**
	 * @return the forumEmail
	 */
	public String getForumEmail()
	{
		return this.forumEmail;
	}

	/**
	 * @param forumEmail the forumEmail to set
	 */
	public void setForumEmail(final String forumEmail)
	{
		this.forumEmail = forumEmail;
	}
	
	public void setSsl(final boolean ssl)
	{
		this.ssl = ssl;
	}
	
	public boolean isSsl()
	{
		return this.ssl;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return new StringBuilder()
			.append('[')
			.append("email=").append(this.forumEmail)
			.append(", host=").append(this.popHost)
			.append(", port=").append(this.popPort)
			.append(", ssl=").append(this.ssl)
			.append(']')
			.toString();
	}
}
