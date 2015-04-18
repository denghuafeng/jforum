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
 * Created on Jan 18, 2005 3:08:48 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum.common;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Attachment;
import net.jforum.entities.AttachmentExtension;
import net.jforum.entities.AttachmentInfo;
import net.jforum.entities.Group;
import net.jforum.entities.Post;
import net.jforum.entities.QuotaLimit;
import net.jforum.entities.User;
import net.jforum.exceptions.AttachmentException;
import net.jforum.exceptions.AttachmentSizeTooBigException;
import net.jforum.exceptions.BadExtensionException;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.view.forum.common.Stats;
import net.jforum.util.I18n;
import net.jforum.util.Hash;
import net.jforum.util.image.ImageUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class AttachmentCommon
{
	private static final Logger LOGGER = Logger.getLogger(AttachmentCommon.class);
	private static final String DENY_ALL = "*";
	
	private final RequestContext request;
	private AttachmentDAO attachmentDao;
	private final boolean canProceed;
	private final Map<UploadUtils, Attachment> filesToSave = new HashMap<UploadUtils, Attachment>();
	
	public AttachmentCommon(final RequestContext request, final int forumId)
	{
		this.request = request;
		this.attachmentDao = DataAccessDriver.getInstance().newAttachmentDAO();
		
		this.canProceed = SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, 
			Integer.toString(forumId));
	}
	
	public void preProcess()
	{
		if (!this.canProceed) {
			return;
		}
		
		final String totalFiles = this.request.getParameter("total_files");
		
		if (totalFiles == null || "".equals(totalFiles)) {
			return;
		}
		
		int total = Integer.parseInt(totalFiles);
		
		if (total < 1) {
			return;
		}
		
		if (total > SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_MAX_POST)) {
			total = SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_MAX_POST);
		}

		long totalSize = 0;
		final int userId = SessionFacade.getUserSession().getUserId();
		final Map<String, Boolean> extensions = this.attachmentDao.extensionsForSecurity();
		
		for (int i = 0; i < total; i++) {
			final FileItem item = (FileItem)this.request.getObjectParameter("file_" + i);
			
			if (item == null) {
				continue;
			}

			if (item.getName().indexOf('\000') > -1) {
				LOGGER.warn("Possible bad attachment (null char): " + item.getName()
					+ " - user_id: " + SessionFacade.getUserSession().getUserId());
				continue;
			}
			
			final UploadUtils uploadUtils = new UploadUtils(item);

			// Check if the extension is allowed
			boolean containsExtension = extensions.containsKey(uploadUtils.getExtension());
			boolean denyAll = extensions.containsKey(DENY_ALL);

			boolean isAllowed = (!denyAll && !containsExtension)
				|| (containsExtension && extensions.get(uploadUtils.getExtension()).equals(Boolean.TRUE));

			if (!isAllowed) { 
				throw new BadExtensionException(I18n.getMessage("Attachments.badExtension", 
					new String[] { uploadUtils.getExtension() }));
			}

			// Check comment length:
			String comment = this.request.getParameter("comment_" + i);
			if (comment.length() > 254) {
				throw new AttachmentException("Comment too long.");
			}
			
			Attachment attachment = new Attachment();
			attachment.setUserId(userId);
			
			AttachmentInfo info = new AttachmentInfo();
			info.setFilesize(item.getSize());
			info.setComment(comment);
			info.setMimetype(item.getContentType());
			
			// Get only the filename, without the path (IE does that)
			String realName = this.stripPath(item.getName());
			
			info.setRealFilename(realName);
			info.setUploadTimeInMillis(System.currentTimeMillis());
			
			AttachmentExtension ext = this.attachmentDao.selectExtension(uploadUtils.getExtension().toLowerCase());
			if (ext.isUnknown()) {
				ext.setExtension(uploadUtils.getExtension());
			}
			
			info.setExtension(ext);
			String savePath = this.makeStoreFilename(info);
			info.setPhysicalFilename(savePath);
			
			attachment.setInfo(info);
			filesToSave.put(uploadUtils, attachment);
			
			totalSize += item.getSize();
		}
		
		// Check upload limits
		QuotaLimit quotaLimit = this.getQuotaLimit(userId);
		if ((quotaLimit != null) && quotaLimit.exceedsQuota(totalSize)) {
			throw new AttachmentSizeTooBigException(I18n.getMessage("Attachments.tooBig", 
					new Integer[] { Integer.valueOf(quotaLimit.getSizeInBytes() / 1024), 
					Integer.valueOf((int)totalSize / 1024) }));			
		}
	}

	/**
	 * @param origRealName String
	 * @return String
	 */
	public String stripPath(String origRealName)
	{
		String realName = origRealName;
		String separator = "/";
		int index = realName.lastIndexOf(separator);
		
		if (index == -1) {
			separator = "\\";
			index = realName.lastIndexOf(separator);
		}
		
		if (index > -1) {
			realName = realName.substring(index + 1);
		}
		
		return realName;
	}
	
	public void insertAttachments(final Post post)
	{
		if (!this.canProceed) {
			return;
		}
		
		//post.hasAttachments(this.filesToSave.size() > 0);
		
		for (Iterator<Map.Entry<UploadUtils, Attachment>>  iter = this.filesToSave.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<UploadUtils, Attachment> entry = iter.next();
			Attachment attachment = entry.getValue();
			attachment.setPostId(post.getId());
			
			String path = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR) 
				+ "/" 
				+ attachment.getInfo().getPhysicalFilename();
			
			this.attachmentDao.addAttachment(attachment);
			entry.getKey().saveUploadedFile(path);
			
			if (this.shouldCreateThumb(attachment)) {
				this.createSaveThumb(path);
			}

			Stats.record("File upload", entry.getKey().getOriginalName());
		}
	}
	
	private boolean shouldCreateThumb(final Attachment attachment) {
		String extension = attachment.getInfo().getExtension().getExtension().toLowerCase();
		if (SystemGlobals.getBoolValue(ConfigKeys.ATTACHMENTS_IMAGES_CREATE_THUMB)
            && Attachment.isPicture(extension)) {
            String path = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR)
			     + "/" 
			     + attachment.getInfo().getPhysicalFilename();
		    File imageFile = new File(path);
		    BufferedImage image = null;
		    try {
			    image = ImageIO.read(imageFile);
		    } catch (IOException e) {
		    	LOGGER.error(e.toString(), e);
		    }
		    int width = image.getWidth(null);
		    int height = image.getHeight(null);
		    return (width > SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_IMAGES_MAX_THUMB_W) 
		        || height > SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_IMAGES_MAX_THUMB_H));
        }
        return false;
	}
	
	private void createSaveThumb(final String path) {
		try {
			BufferedImage image = ImageUtils.resizeImage(path, ImageUtils.IMAGE_JPEG, 
				SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_IMAGES_MAX_THUMB_W),
				SystemGlobals.getIntValue(ConfigKeys.ATTACHMENTS_IMAGES_MAX_THUMB_H));
			ImageUtils.saveImage(image, path + "_thumb", ImageUtils.IMAGE_JPEG);
		}
		catch (Exception e) {
			LOGGER.error(e.toString(), e);
		}
	}
	
	public QuotaLimit getQuotaLimit(final int userId)
	{
		QuotaLimit ql = new QuotaLimit();
		User user = DataAccessDriver.getInstance().newUserDAO().selectById(userId);
		
		for (Iterator<Group> iter = user.getGroupsList().iterator(); iter.hasNext();) {
			QuotaLimit l = this.attachmentDao.selectQuotaLimitByGroup(iter.next().getId());
			if (l == null) {
				continue;
			}
			
			if (l.getSizeInBytes() > ql.getSizeInBytes()) {
				ql = l;
			}
		}
		
		if (ql.getSize() == 0) {
			return null;
		}
		
		return ql;
	}
	
	public void editAttachments(final int postId, final int forumId)
	{
		// Allow removing the attachments at least
		AttachmentDAO am = DataAccessDriver.getInstance().newAttachmentDAO();
		
		// Check for attachments to remove
		List<String> deleteList = new ArrayList<String>();
		String[] delete = null;
		String s = this.request.getParameter("delete_attach");
		
		if (s != null) {
			delete = s.split(",");
		}
		
		if (delete != null) {
			for (int i = 0; i < delete.length; i++) {
				if (delete[i] != null && !delete[i].equals("")) {
					int id = Integer.parseInt(delete[i]);
					Attachment a = am.selectAttachmentById(id);
					
					am.removeAttachment(id, postId);
					
					String filename = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR)
						+ "/" + a.getInfo().getPhysicalFilename();
					
					File f = new File(filename);
					
					if (f.exists()) {
						boolean result = f.delete();
						if (result != true) {
							LOGGER.error("Delete file failed: " + f.getName());
						}
					}
					
					// Check if we have a thumb to delete
					f = new File(filename + "_thumb");
					
					if (f.exists()) {
						boolean result = f.delete();
						if (result != true) {
							LOGGER.error("Delete file failed: " + f.getName());
						}
					}
					
					// Remove the empty parent directory
					File parent = f.getParentFile();
					if (parent.list().length == 0) {
						boolean result = parent.delete();
						if (result != true) {
							LOGGER.error("Delete directory failed: " + parent.getName());
						}
					}
					
					// Remove the empty grand parent directory
					File grandparent = parent.getParentFile();
					if (grandparent.list().length == 0) {
						boolean result = grandparent.delete();
						if (result != true) {
							LOGGER.error("Delete directory failed: " +grandparent.getName());
						}
					}
				}
			}
			
			deleteList = Arrays.asList(delete);
		}
		
		if (!SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, 
				Integer.toString(forumId))
				&& !SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD)) {
			return;
		}
		
		// Update
		String[] attachIds = null;
		s = this.request.getParameter("edit_attach_ids");
		if (s != null) {
			attachIds = s.split(",");
		}
		
		if (attachIds != null) {
			for (int i = 0; i < attachIds.length; i++) {
				if (deleteList.contains(attachIds[i]) 
						|| attachIds[i] == null || attachIds[i].equals("")) {
					continue;
				}
				
				int id = Integer.parseInt(attachIds[i]);
				Attachment a = am.selectAttachmentById(id);
				a.getInfo().setComment(this.request.getParameter("edit_comment_" + id));

				am.updateAttachment(a);
			}
		}
	}
	
	private String makeStoreFilename(AttachmentInfo attInfo)
	{
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.get(Calendar.YEAR);
		
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		
		StringBuilder dir = new StringBuilder(256);
		dir.append(year).append('/').append(month).append('/').append(day).append('/');
		
		File path = new File(SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR) + "/" + dir); 
		// check if we have the directory already
		if (!path.exists()) {
			boolean result = path.mkdirs();
			if (!result) {
				LOGGER.error("Create directory failed: " + SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR) + "/" + dir);
			}
		}		
		
		return dir
			.append(Hash.md5(attInfo.getRealFilename() + System.currentTimeMillis() + SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE) + new Random().nextInt(999999)))
			.append('_')
			.append(SessionFacade.getUserSession().getUserId())
			.append('.')
			.append(attInfo.getExtension().getExtension())
			.append('_')
			.toString();
	}
	
	public List<Attachment> getAttachments(final int postId, final int forumId)
	{
		if (!SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD)
				&& !SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, 
						Integer.toString(forumId))) {
			return new ArrayList<Attachment>();
		}
		
		return this.attachmentDao.selectAttachments(postId);
	}
	
	public boolean isPhysicalDownloadMode(final int extensionGroupId) 
	{
		return this.attachmentDao.isPhysicalDownloadMode(extensionGroupId);
	}

	public void deleteAttachments(final int postId, final int forumId) 
	{
		// Attachments
		List<Attachment> attachments = DataAccessDriver.getInstance().newAttachmentDAO().selectAttachments(postId);
		StringBuilder attachIds = new StringBuilder();
		
		for (Iterator<Attachment> iter = attachments.iterator(); iter.hasNext(); ) {
			Attachment a = iter.next();
			attachIds.append(a.getId()).append(',');
		}
		
		this.request.addOrReplaceParameter("delete_attach", attachIds.toString());
		this.editAttachments(postId, forumId);
	}
}
