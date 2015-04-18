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
 * Created on 29/11/2004 23:07:10
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum.common;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.entities.User;
import net.jforum.repository.SpamRepository;
import net.jforum.util.I18n;
import net.jforum.util.Hash;
import net.jforum.util.SafeHtml;
import net.jforum.util.image.ImageUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 */
public final class UserCommon 
{
	private static final Logger LOGGER = Logger.getLogger(UserCommon.class);
	private static final String IMAGE_AVATAR = SystemGlobals.getValue(ConfigKeys.AVATAR_STORE_DIR);

	/**
	 * Updates the user information
	 * 
	 * @param userId int The user id we are saving
     * @return List
	 */
	public static List<String> saveUser(final int userId)
	{
		final List<String> errors = new ArrayList<String>();

		final UserDAO userDao = DataAccessDriver.getInstance().newUserDAO();
		final User user = userDao.selectById(userId);

		final RequestContext request = JForumExecutionContext.getRequest();
		final boolean isAdmin = SessionFacade.getUserSession().isAdmin();

		if (isAdmin) {
			final String username = request.getParameter("username");

			if (username != null) {
				user.setUsername(username.trim());
			}

			if (request.getParameter("rank_special") != null) {
				user.setRankId(request.getIntParameter("rank_special"));
			}
		}

		final SafeHtml safeHtml = new SafeHtml();

		user.setId(userId);
		user.setIcq(safeHtml.makeSafe(request.getParameter("icq")));
		user.setTwitter(safeHtml.makeSafe(request.getParameter("twitter")));
		user.setAim(safeHtml.makeSafe(request.getParameter("aim")));
		user.setMsnm(safeHtml.makeSafe(request.getParameter("msn")));
		user.setYim(safeHtml.makeSafe(request.getParameter("yim")));
		user.setFrom(safeHtml.makeSafe(request.getParameter("location")));
		user.setOccupation(checkForSpam(safeHtml, request.getParameter("occupation"), isAdmin));
		user.setInterests(checkForSpam(safeHtml, request.getParameter("interests"), isAdmin));
		user.setBiography(checkForSpam(safeHtml, request.getParameter("biography"), isAdmin));
		user.setSignature(checkForSpam(safeHtml, request.getParameter("signature"), isAdmin));
		user.setViewEmailEnabled(request.getParameter("viewemail").equals("1"));
		user.setViewOnlineEnabled(request.getParameter("hideonline").equals("0"));
		user.setNotifyPrivateMessagesEnabled(request.getParameter("notifypm").equals("1"));
		user.setNotifyOnMessagesEnabled(request.getParameter("notifyreply").equals("1"));
		user.setAttachSignatureEnabled(request.getParameter("attachsig").equals("1"));
		user.setHtmlEnabled(request.getParameter("allowhtml").equals("1"));
		user.setLang(request.getParameter("language"));
		user.setBbCodeEnabled("1".equals(request.getParameter("allowbbcode")));
		user.setSmiliesEnabled("1".equals(request.getParameter("allowsmilies")));
		user.setNotifyAlways("1".equals(request.getParameter("notify_always")));
		user.setNotifyText("1".equals(request.getParameter("notify_text")));

		String website = safeHtml.makeSafe(request.getParameter("website"));
		if (StringUtils.isNotEmpty(website) && !website.toLowerCase(Locale.US).startsWith("http://") 
				&& !website.toLowerCase(Locale.US).startsWith("https://")) {
			website = "http://" + website;
		}

		user.setWebSite(website);

		String currentPassword = request.getParameter("current_password");
		String currentPasswordMD5 = "", currentPasswordSHA512 = "", currentPasswordSHA512Salt = "";
		final boolean isCurrentPasswordEmpty = currentPassword == null || "".equals(currentPassword.trim());

		if (isAdmin || !isCurrentPasswordEmpty) {
			if (!isCurrentPasswordEmpty) {
				currentPasswordMD5 = Hash.md5(currentPassword);
				currentPasswordSHA512 = Hash.sha512(currentPassword);
				currentPasswordSHA512Salt = Hash.sha512(currentPassword+SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE));
			}

			if (isAdmin
					|| user.getPassword().equals(currentPasswordMD5)
					|| user.getPassword().equals(currentPasswordSHA512)
					|| user.getPassword().equals(currentPasswordSHA512Salt)) {
				user.setEmail(safeHtml.makeSafe(request.getParameter("email")));

				final String newPassword = request.getParameter("new_password");

				if (newPassword != null && newPassword.length() > 0) {
					user.setPassword(Hash.sha512(newPassword+SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE)));
				}
			}
			else {
				errors.add(I18n.getMessage("User.currentPasswordInvalid"));
			}
		}

		if (request.getParameter("avatardel") != null) {
			final File file = new File(IMAGE_AVATAR + user.getAvatar());
			if (file.exists()) {
				final boolean result = file.delete();
				if (!result) {
					LOGGER.error("Delete file failed: " + file.getName());
				}
			}
			user.setAvatar(null);
		}

		if (request.getObjectParameter("avatar") != null) {
			try {
				UserCommon.handleAvatar(user);
			}
			catch (Exception e) {
				LOGGER.warn("Problems while uploading the avatar: " + e);
				errors.add(I18n.getMessage("User.avatarUploadError"));
			}
		} 
		else if (SystemGlobals.getBoolValue(ConfigKeys.AVATAR_ALLOW_EXTERNAL_URL)) {
			final String avatarUrl = request.getParameter("avatarUrl");
			if (StringUtils.isNotEmpty(avatarUrl)) {
				if (avatarUrl.toLowerCase(Locale.US).startsWith("http://") 
						|| avatarUrl.toLowerCase(Locale.US).startsWith("https://")) {
					// make sure it's really an image
					try {
						BufferedImage image = ImageIO.read(new URL(avatarUrl));
						if (image != null) {
							user.setAvatar(avatarUrl);
						} else {
							user.setAvatar(null);
							errors.add("URL is not an image");
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
						errors.add("URL malformed");
					} catch (IOException e) {
						e.printStackTrace();
						errors.add("read image error");
					}
				}
				else {
					errors.add(I18n.getMessage("User.avatarUrlShouldHaveHttp"));
				}
			}
		}

		if (errors.isEmpty()) {
			userDao.update(user);
		}

		if (SessionFacade.getUserSession().getUserId() == userId) {
		    SessionFacade.getUserSession().setLang(user.getLang());
		}
		return errors;
	}

    private static String checkForSpam (SafeHtml safeHtml, String text, boolean isAdmin) {
		String result = SpamRepository.findSpam(text);
        if (isAdmin || (result == null)) {
			return safeHtml.makeSafe(text);
        } else {
			return "";
		}
    }

	/**
	 * @param user User
	 */
	private static void handleAvatar(final User user)
	{
		boolean result = false;
		// Delete old avatar file
		if (user.getAvatar() != null) {
			final File avatarFile = new File(user.getAvatar());

			final File fileToDelete = new File(IMAGE_AVATAR + avatarFile.getName());

			if (fileToDelete.exists()) {
				result = fileToDelete.delete();
				if (!result) {
					LOGGER.error("Delete file failed: " + fileToDelete.getName());
				}
			}
		}

		final String fileName = Hash.md5(Integer.toString(user.getId()));
		FileItem item = (FileItem)JForumExecutionContext.getRequest().getObjectParameter("avatar");
		UploadUtils uploadUtils = new UploadUtils(item);

		// Gets file extension
		String extension = uploadUtils.getExtension().toLowerCase();
		int type = ImageUtils.IMAGE_UNKNOWN;

		if ("jpg".equals(extension) || "jpeg".equals(extension)) {
			type = ImageUtils.IMAGE_JPEG;
		} 
		else if ("gif".equals(extension)) {  
			type = ImageUtils.IMAGE_GIF;  
		} 
		else if ("png".equals(extension)) {  
			type = ImageUtils.IMAGE_PNG;  
		}

		if (type != ImageUtils.IMAGE_UNKNOWN) {
			String avatarTmpFileName = IMAGE_AVATAR + fileName + "_tmp." + extension;

			String avatarFinalFileName = IMAGE_AVATAR + fileName + "." + extension;

			uploadUtils.saveUploadedFile(avatarTmpFileName);

			// OK, time to check and process the avatar size
			int maxWidth = SystemGlobals.getIntValue(ConfigKeys.AVATAR_MAX_WIDTH);
			int maxHeight = SystemGlobals.getIntValue(ConfigKeys.AVATAR_MAX_HEIGHT);

			File avatar = new File(avatarTmpFileName); 
			BufferedImage imageOriginal = null;
			try {
				imageOriginal = ImageIO.read(avatar);
			} 
			catch (IOException e) {
				LOGGER.error(e.toString(), e);
			}
			int width = imageOriginal.getWidth(null);
			int height = imageOriginal.getHeight(null);

			if (width > maxWidth || height > maxHeight) {
				if (type == ImageUtils.IMAGE_GIF) {
					type = ImageUtils.IMAGE_PNG;
					extension = "png";
				}
				BufferedImage image = ImageUtils.resizeImage(avatarTmpFileName, type, maxWidth, maxHeight);
				ImageUtils.saveImage(image, avatarFinalFileName, type);
				// Delete the temporary file
				result = avatar.delete();
				if (!result) {
					LOGGER.error("Delete file failed: " + avatar.getName());
				}
			} 
			else {
				result = avatar.renameTo(new File(avatarFinalFileName));
				if (!result) {
					LOGGER.error("Rename file failed: " + avatar.getName());
				}
			}
			user.setAvatar(fileName + "." + extension);
		}
	}

	private UserCommon() {}

}
