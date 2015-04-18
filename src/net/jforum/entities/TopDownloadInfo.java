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
 * Created on Sept 15, 2010 12:57:13 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.entities;

import java.io.Serializable;

/**
 * @author Andowson
 * @version $Id$
 */
public class TopDownloadInfo implements Serializable
{
	private static final long serialVersionUID = 455195420067276709L;
	private int forumId;
	private String forumName;
	private int topicId;
	private String topicTitle;
	private int attachId;
	private int downloadCount;
	private long filesize;
	private String realFilename;
	/**
	 * @return the forumId
	 */
	public int getForumId() {
		return forumId;
	}
	/**
	 * @param forumId the forumId to set
	 */
	public void setForumId(final int forumId) {
		this.forumId = forumId;
	}
	/**
	 * @return the forumName
	 */
	public String getForumName() {
		return forumName;
	}
	/**
	 * @param forumName the forumName to set
	 */
	public void setForumName(final String forumName) {
		this.forumName = forumName;
	}
	/**
	 * @return the topicId
	 */
	public int getTopicId() {
		return topicId;
	}
	/**
	 * @param topicId the topicId to set
	 */
	public void setTopicId(final int topicId) {
		this.topicId = topicId;
	}
	/**
	 * @return the topicTitle
	 */
	public String getTopicTitle() {
		return topicTitle;
	}
	/**
	 * @param topicTitle the topicTitle to set
	 */
	public void setTopicTitle(final String topicTitle) {
		this.topicTitle = topicTitle;
	}
	/**
	 * @return the attachId
	 */
	public int getAttachId() {
		return attachId;
	}
	/**
	 * @param attachId the attachId to set
	 */
	public void setAttachId(final int attachId) {
		this.attachId = attachId;
	}
	/**
	 * @return the downloadCount
	 */
	public int getDownloadCount() {
		return downloadCount;
	}
	/**
	 * @param downloadCount the downloadCount to set
	 */
	public void setDownloadCount(int downloadCount) {
		this.downloadCount = downloadCount;
	}
	/**
	 * @return the filesize
	 */
	public long getFilesize() {
		return filesize;
	}
	/**
	 * @param filesize the filesize to set
	 */
	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}
	/**
	 * @return the realFilename
	 */
	public String getRealFilename() {
		return realFilename;
	}
	/**
	 * @param realFilename the realFilename to set
	 */
	public void setRealFilename(String realFilename) {
		this.realFilename = realFilename;
	}
}
