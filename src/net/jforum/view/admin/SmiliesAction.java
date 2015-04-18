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
 * Created on 13/01/2004 / 18:45:31
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;

import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;

import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.SmilieDAO;
import net.jforum.entities.Smilie;
import net.jforum.repository.SmiliesRepository;
import net.jforum.util.Hash;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.UploadUtils;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 */
public class SmiliesAction extends AdminCommand 
{
	private static final Logger LOGGER = Logger.getLogger(SmiliesAction.class);

	private String processUpload()
	{
		String imgName = "";

		if (this.request.getObjectParameter("smilie_img") != null) {
			FileItem item = (FileItem)this.request.getObjectParameter("smilie_img");
			UploadUtils uploadUtils = new UploadUtils(item);
			String ext = uploadUtils.getExtension().toLowerCase();
			String contentType = item.getContentType();
			LOGGER.info("Uploaded smilie contentType: " + contentType);
			try {
				if ((contentType != null && contentType.contains("image")) || ext.equals("png") || ext.equals("gif") || ext.equals("jpg") || ext.equals("jpeg")) {
					// try to load it as an image; will also accept other formats if passed along
					// with those file extensions (like TIFF and BMP), but that's fine
					ImageIO.read(new ByteArrayInputStream(item.get()));
					imgName = new StringBuilder(Hash.md5(item.getName())).append('.').append(uploadUtils.getExtension()).toString();
					uploadUtils.saveUploadedFile(SystemGlobals.getApplicationPath() 
							+ "/"
							+ SystemGlobals.getValue(ConfigKeys.SMILIE_IMAGE_DIR) 
							+ "/"
							+ imgName);
				} else {
					throw new Exception("Suspect file extension in smilie upload: " + ext);
				}
			} catch (Exception ex) {
				LOGGER.error("Uploaded smilie does not seem to be an image: " + ex.getMessage());
			}
		}

		return imgName;
	}

	public void insert()
	{
		this.setTemplateName(TemplateKeys.SMILIES_INSERT);
		this.context.put("action", "insertSave");
	}

	public void insertSave()
	{
		Smilie s = new Smilie();
		String code = this.request.getParameter("code");
		if (code!=null && !code.trim().equals("")) {
			s.setCode(code);
			String imgName = this.processUpload();
			if (! imgName.trim().equals("")) {
				s.setUrl(SystemGlobals.getValue(ConfigKeys.SMILIE_IMAGE_PATTERN).replaceAll("#IMAGE#", imgName));
				s.setDiskName(imgName);
				DataAccessDriver.getInstance().newSmilieDAO().addNew(s);
				SmiliesRepository.loadSmilies();
			}
		}
		this.list();
	}

	public void edit()
	{
		int id = 1;

		if (this.request.getParameter("id") != null) {
			id = this.request.getIntParameter("id");
		}

		this.setTemplateName(TemplateKeys.SMILIES_EDIT);
		this.context.put("smilie", DataAccessDriver.getInstance().newSmilieDAO().selectById(id));
		this.context.put("action", "editSave");
	}

	public void editSave()
	{
		Smilie s = DataAccessDriver.getInstance().newSmilieDAO().selectById(this.request.getIntParameter("id"));
		s.setCode(this.request.getParameter("code"));
		if (this.request.getObjectParameter("smilie_img") != null) {
			String imgName = this.processUpload();
			if (! imgName.trim().equals("")) {
				s.setUrl(SystemGlobals.getValue(ConfigKeys.SMILIE_IMAGE_PATTERN).replaceAll("#IMAGE#", imgName));
				s.setDiskName(imgName);
			}
		}
		DataAccessDriver.getInstance().newSmilieDAO().update(s);

		SmiliesRepository.loadSmilies();
		this.list();
	}

	public void delete()
	{
		String[] ids = this.request.getParameterValues("id");

		if (ids != null) {
			SmilieDAO dao = DataAccessDriver.getInstance().newSmilieDAO();

			for (int i = 0; i < ids.length; i++) {
				int id = Integer.parseInt(ids[i]);

				Smilie s = dao.selectById(id);
				dao.delete(id);

				File f = new File(SystemGlobals.getApplicationPath() 
						+ "/"
						+ SystemGlobals.getValue(ConfigKeys.SMILIE_IMAGE_DIR) 
						+ "/"
						+ s.getDiskName());

				if (f.exists()) {
					boolean result = f.delete();
					if (result != true) {
						LOGGER.error("Delete file failed: " + f.getName());
					}
				}
			}
		}

		SmiliesRepository.loadSmilies();
		this.list();
	}

	/** 
	 * @see net.jforum.Command#list()
	 */
	public void list()  
	{
		this.context.put("smilies", SmiliesRepository.getSmilies());
		this.setTemplateName(TemplateKeys.SMILIES_LIST);
	}
}
