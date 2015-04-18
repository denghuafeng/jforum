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
 * Created on Jan 17, 2005 4:31:45 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao;

import java.util.List;
import java.util.Map;

import net.jforum.entities.Attachment;
import net.jforum.entities.AttachmentExtension;
import net.jforum.entities.AttachmentExtensionGroup;
import net.jforum.entities.QuotaLimit;
import net.jforum.entities.TopDownloadInfo;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public interface AttachmentDAO
{
	/**
	 * Adds a new attachment.
	 * 
	 * @param attachment The attachment to add
	 */
	void addAttachment(Attachment attachment);
	
	/**
	 * Updates an attachment.
	 * Only the file comment is updated.
	 * 
	 * @param attachment The attachment to update
	 */
	void updateAttachment(Attachment attachment);
	
	/**
	 * Remove an attachment.
	 * 
	 * @param id The attachment's id to remove
	 * @param postId the post id
	 */
	void removeAttachment(int id, int postId);
	
	/**
	 * Gets the attachments of some message.
	 * 
	 * @param postId The post id associated with the attachments.
	 * @return A list where each entry is a net.jforum.entities.Attachment 
	 * instance.
	 */
	List<Attachment> selectAttachments(int postId);
	
	/**
	 * Gets an attachment by its id
	 * 
	 * @param attachId The attachment id
	 * @return The attachment, or <code>null</code> if no record was found
	 */
	Attachment selectAttachmentById(int attachId);
	
	/**
	 * Inserts a new quota limit.
	 * 
	 * @param limit The data to insert
	 */
	void addQuotaLimit(QuotaLimit limit);
	
	/**
	 * Updates a quota limit.
	 * 
	 * @param limit The data to update
	 */
	void updateQuotaLimit(QuotaLimit limit);
	
	/**
	 * Deletes a quota limit
	 * 
	 * @param id The id of the quota to remove
	 */
	void removeQuotaLimit(int id);
	
	/**
	 * Removes a set of quota limit.
	 * 
	 * @param ids The ids to remove.
	 */
	void removeQuotaLimit(String[] ids);
	
	/**
	 * Associates a quota limit to some group.
	 * 
	 * @param groupId The group id
	 * @param quotaId The quota id
	 */
	void setGroupQuota(int groupId, int quotaId);
	
	/**
	 * Removes all quotas limits from all groups.
	 *  
	 */
	void cleanGroupQuota();
	
	/**
	 * Gets all registered quota limits
	 * 
	 * @return A list instance where each entry is a
	 * {@link net.jforum.entities.QuotaLimit} instance.
	 */
	List<QuotaLimit> selectQuotaLimit();
	
	/**
	 * Gets the quota associated to some group.
	 * 
	 * @param groupId The group id
	 * @return A <code>QuotaLimit</code> instance, or <code>null</code> if
	 * no records were found. 
	 */
	QuotaLimit selectQuotaLimitByGroup(int groupId) ;
	
	/**
	 * Gets the quota limits of registered groups.
	 * 
	 * @return A map instance where each key is the group id
	 * and the value is the quota limit id.
	 */
	Map<Integer, Integer> selectGroupsQuotaLimits();
	
	/**
	 * Adds a new extension group.
	 * 
	 * @param aeg The data to insert
	 */
	void addExtensionGroup(AttachmentExtensionGroup aeg);
	
	/**
	 * Updates some extension group.
	 * 
	 * @param aeg The data to update
	 */
	void updateExtensionGroup(AttachmentExtensionGroup aeg);
	
	/**
	 * Removes a set of extension groups.
	 * 
	 * @param ids The ids to remove.
	 */
	void removeExtensionGroups(String[] ids);
	
	/**
	 * Gets all extension groups.
	 * 
	 * @return A list instance where each entry is an 
	 * {@link net.jforum.entities.AttachmentExtensionGroup} instance.
	 */
	List<AttachmentExtensionGroup> selectExtensionGroups();
	
	/**
	 * Gets all extensions and its security options, 
	 * as well from the groups. 
	 * 
	 * @return A map instance where the key is the extension name
	 * and the value is a Boolean, indicating if the extension can
	 * be used in the uploaded files. If there is no entry for
	 * a given extension, then it means that it is allowed. 
	 */
	Map<String, Boolean> extensionsForSecurity();
	
	/**
	 * Adds a new extension
	 * 
	 * @param attext The extension to add
	 */
	void addExtension(AttachmentExtension attext);
	
	/**
	 * Updates an extension
	 * 
	 * @param attext The extension to update
	 */
	void updateExtension(AttachmentExtension attext);
	
	/**
	 * Removes a set of extensions
	 * 
	 * @param ids The ids to remove
	 */
	void removeExtensions(String[] ids);
	
	/**
	 * Gets all registered extensions
	 * 
	 * @return A list instance, where each entry is an
	 * {@link net.jforum.entities.AttachmentExtension} instance
	 */
	List<AttachmentExtension> selectExtensions();
	
	/**
	 * Gets an extension information by the extension's name
	 * @param extension
	 * @return AttachmentExtension
	 */
	AttachmentExtension selectExtension(String extension);

	/**
	 * Gets the download mode by the extension group id
	 * @param extensionGroupId extension group id
	 * @return true = physical download mode; false = inline download mode
	 */
	boolean isPhysicalDownloadMode(int extensionGroupId);	

	/**
	 * Selects top download attachments
	 *
	 * @param limit The number of attachments to retrieve
	    * @return List
	 */
	List<TopDownloadInfo> selectTopDownloads(int limit);
}
