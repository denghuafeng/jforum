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
 * Created on Jan 17, 2005 4:36:30 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jforum.JForumExecutionContext;
import net.jforum.entities.Attachment;
import net.jforum.entities.AttachmentExtension;
import net.jforum.entities.AttachmentExtensionGroup;
import net.jforum.entities.AttachmentInfo;
import net.jforum.entities.QuotaLimit;
import net.jforum.entities.TopDownloadInfo;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericAttachmentDAO extends AutoKeys implements net.jforum.dao.AttachmentDAO
{
	/**
	 * @see net.jforum.dao.AttachmentDAO#addQuotaLimit(net.jforum.entities.QuotaLimit)
	 */
	public void addQuotaLimit(final QuotaLimit limit)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.addQuotaLimit"));
			pstmt.setString(1, limit.getDescription());
			pstmt.setInt(2, limit.getSize());
			pstmt.setInt(3, limit.getType());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#updateQuotaLimit(net.jforum.entities.QuotaLimit)
	 */
	public void updateQuotaLimit(final QuotaLimit limit)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.updateQuotaLimit"));
			pstmt.setString(1, limit.getDescription());
			pstmt.setInt(2, limit.getSize());
			pstmt.setInt(3, limit.getType());
			pstmt.setInt(4, limit.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#cleanGroupQuota()
	 */
	public void cleanGroupQuota()
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.deleteGroupQuota"));
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#setGroupQuota(int, int)
	 */
	public void setGroupQuota(final int groupId, final int quotaId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.setGroupQuota"));
			pstmt.setInt(1, groupId);
			pstmt.setInt(2, quotaId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#removeQuotaLimit(int)
	 */
	public void removeQuotaLimit(final int id)
	{
		this.removeQuotaLimit(new String[] { Integer.toString(id) });
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#removeQuotaLimit(java.lang.String[])
	 */
	public void removeQuotaLimit(final String[] ids)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.removeQuotaLimit"));

			for (int i = 0; i < ids.length; i++) {
				pstmt.setInt(1, Integer.parseInt(ids[i]));
				pstmt.executeUpdate();
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectQuotaLimit()
	 */
	public List<QuotaLimit> selectQuotaLimit()
	{
		final List<QuotaLimit> list = new ArrayList<QuotaLimit>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectQuotaLimit"));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getQuotaLimit(resultSet));
			}

			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectQuotaLimit()
	 */
	public QuotaLimit selectQuotaLimitByGroup(final int groupId)
	{
		QuotaLimit quotaLimit = null;

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectQuotaLimitByGroup"));
			pstmt.setInt(1, groupId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				quotaLimit = this.getQuotaLimit(resultSet);
			}
			return quotaLimit;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectGroupsQuotaLimits()
	 */
	public Map<Integer, Integer> selectGroupsQuotaLimits()
	{
		final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectGroupsQuotaLimits"));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				map.put(Integer.valueOf(resultSet.getInt("group_id")), Integer.valueOf(resultSet.getInt("quota_limit_id")));
			}

			return map;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected QuotaLimit getQuotaLimit(final ResultSet resultSet) throws SQLException
	{
		final QuotaLimit quotaLimit = new QuotaLimit();
		quotaLimit.setDescription(resultSet.getString("quota_desc"));
		quotaLimit.setId(resultSet.getInt("quota_limit_id"));
		quotaLimit.setSize(resultSet.getInt("quota_limit"));
		quotaLimit.setType(resultSet.getInt("quota_type"));

		return quotaLimit;
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#addExtensionGroup(net.jforum.entities.AttachmentExtensionGroup)
	 */
	public void addExtensionGroup(final AttachmentExtensionGroup aeg)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.addExtensionGroup"));
			pstmt.setString(1, aeg.getName());
			pstmt.setInt(2, aeg.isAllow() ? 1 : 0);
			pstmt.setString(3, aeg.getUploadIcon());
			pstmt.setInt(4, aeg.getDownloadMode());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#removeExtensionGroups(java.lang.String[])
	 */
	public void removeExtensionGroups(final String[] ids)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.removeExtensionGroups"));

			for (int i = 0; i < ids.length; i++) {
				pstmt.setInt(1, Integer.parseInt(ids[i]));
				pstmt.executeUpdate();
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectExtensionGroups()
	 */
	public List<AttachmentExtensionGroup> selectExtensionGroups()
	{
		final List<AttachmentExtensionGroup> list = new ArrayList<AttachmentExtensionGroup>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectExtensionGroups"));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getExtensionGroup(resultSet));
			}

			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#extensionsForSecurity()
	 */
	public Map<String, Boolean> extensionsForSecurity()
	{
		final Map<String, Boolean> map = new HashMap<String, Boolean>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.extensionsForSecurity"));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				int allow = resultSet.getInt("group_allow");
				if (allow == 1) {
					allow = resultSet.getInt("allow");
				}

				map.put(resultSet.getString("extension"), Boolean.valueOf(allow == 1));
			}

			return map;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#updateExtensionGroup(net.jforum.entities.AttachmentExtensionGroup)
	 */
	public void updateExtensionGroup(final AttachmentExtensionGroup aeg)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.updateExtensionGroups"));
			pstmt.setString(1, aeg.getName());
			pstmt.setInt(2, aeg.isAllow() ? 1 : 0);
			pstmt.setString(3, aeg.getUploadIcon());
			pstmt.setInt(4, aeg.getDownloadMode());
			pstmt.setInt(5, aeg.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	protected AttachmentExtensionGroup getExtensionGroup(final ResultSet resultSet) throws SQLException
	{
		final AttachmentExtensionGroup aeg = new AttachmentExtensionGroup();
		aeg.setId(resultSet.getInt("extension_group_id"));
		aeg.setName(resultSet.getString("name"));
		aeg.setUploadIcon(resultSet.getString("upload_icon"));
		aeg.setAllow(resultSet.getInt("allow") == 1);
		aeg.setDownloadMode(resultSet.getInt("download_mode"));

		return aeg;
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#addExtension(net.jforum.entities.AttachmentExtension)
	 */
	public void addExtension(final AttachmentExtension extension)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.addExtension"));
			pstmt.setInt(1, extension.getExtensionGroupId());
			pstmt.setString(2, extension.getComment());
			pstmt.setString(3, extension.getUploadIcon());
			pstmt.setString(4, extension.getExtension().toLowerCase());
			pstmt.setInt(5, extension.isAllow() ? 1 : 0);
			pstmt.executeUpdate();
		}
		catch (SQLException ex) {
			throw new DatabaseException(ex);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#removeExtensions(java.lang.String[])
	 */
	public void removeExtensions(final String[] ids)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.removeExtension"));
			for (int i = 0; i < ids.length; i++) {
				pstmt.setInt(1, Integer.parseInt(ids[i]));
				pstmt.executeUpdate();
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectExtensions()
	 */
	public List<AttachmentExtension> selectExtensions()
	{
		final List<AttachmentExtension> list = new ArrayList<AttachmentExtension>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectExtensions"));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getExtension(resultSet));
			}

			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#updateExtension(net.jforum.entities.AttachmentExtension)
	 */
	public void updateExtension(final AttachmentExtension extension)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.updateExtension"));
			pstmt.setInt(1, extension.getExtensionGroupId());
			pstmt.setString(2, extension.getComment());
			pstmt.setString(3, extension.getUploadIcon());
			pstmt.setString(4, extension.getExtension().toLowerCase());
			pstmt.setInt(5, extension.isAllow() ? 1 : 0);
			pstmt.setInt(6, extension.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectExtension(java.lang.String)
	 */
	public AttachmentExtension selectExtension(final String extension)
	{
		return this.searchExtension(SystemGlobals.getValue(ConfigKeys.EXTENSION_FIELD), extension);
	}

	private AttachmentExtension selectExtension(final int extensionId)
	{
		return this.searchExtension("extension_id", Integer.valueOf(extensionId));
	}

	private AttachmentExtension searchExtension(final String paramName, final Object paramValue)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			String sql = SystemGlobals.getSql("AttachmentModel.selectExtension");
			sql = sql.replaceAll("\\$field", paramName);

			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setObject(1, paramValue);

			AttachmentExtension extension = new AttachmentExtension();

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				extension = this.getExtension(resultSet);
			}
			else {
				extension.setUnknown(true);
			}

			return extension;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected AttachmentExtension getExtension(final ResultSet resultSet) throws SQLException
	{
		final AttachmentExtension extension = new AttachmentExtension();
		extension.setAllow(resultSet.getInt("allow") == 1);
		extension.setComment(resultSet.getString("description"));
		extension.setExtension(resultSet.getString("extension"));
		extension.setExtensionGroupId(resultSet.getInt("extension_group_id"));
		extension.setId(resultSet.getInt("extension_id"));

		String icon = resultSet.getString("upload_icon");
		if (icon == null || icon.equals("")) {
			icon = resultSet.getString("group_icon");
		}

		extension.setUploadIcon(icon);

		return extension;
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#addAttachment(net.jforum.entities.Attachment)
	 */
	public void addAttachment(final Attachment attachment)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = this.getStatementForAutoKeys("AttachmentModel.addAttachment");
			pstmt.setInt(1, attachment.getPostId());
			pstmt.setInt(2, attachment.getPrivmsgsId());
			pstmt.setInt(3, attachment.getUserId());

			this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("AttachmentModel.lastGeneratedAttachmentId"));
			final int id = this.executeAutoKeysQuery(pstmt);
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.addAttachmentInfo"));
			pstmt.setInt(1, id);
			pstmt.setString(2, attachment.getInfo().getPhysicalFilename());
			pstmt.setString(3, attachment.getInfo().getRealFilename());
			pstmt.setString(4, attachment.getInfo().getComment());
			pstmt.setString(5, attachment.getInfo().getMimetype());
			pstmt.setLong(6, attachment.getInfo().getFilesize());
			pstmt.setTimestamp(7, new Timestamp(attachment.getInfo().getUploadTimeInMillis()));
			pstmt.setInt(8, 0);
			pstmt.setInt(9, attachment.getInfo().getExtension().getId());
			pstmt.executeUpdate();

			this.updatePost(attachment.getPostId(), 1);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	protected void updatePost(final int postId, final int count)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.updatePost"));
			pstmt.setInt(1, count);
			pstmt.setInt(2, postId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#removeAttachment(int, int)
	 */
	public void removeAttachment(final int id, final int postId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.removeAttachmentInfo"));
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.removeAttachment"));
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.countPostAttachments"));
			pstmt.setInt(1, postId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				this.updatePost(postId, resultSet.getInt(1));
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#updateAttachment(net.jforum.entities.Attachment)
	 */
	public void updateAttachment(final Attachment attachment)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.updateAttachment"));
			pstmt.setString(1, attachment.getInfo().getComment());
			pstmt.setInt(2, attachment.getInfo().getDownloadCount());
			pstmt.setInt(3, attachment.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectAttachments(int)
	 */
	public List<Attachment> selectAttachments(final int postId)
	{
		final List<Attachment> list = new ArrayList<Attachment>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectAttachments"));
			pstmt.setInt(1, postId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getAttachment(resultSet));
			}

			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected Attachment getAttachment(final ResultSet resultSet) throws SQLException
	{
		final Attachment attachment = new Attachment();
		attachment.setId(resultSet.getInt("attach_id"));
		attachment.setPostId(resultSet.getInt("post_id"));
		attachment.setPrivmsgsId(resultSet.getInt("privmsgs_id"));

		final AttachmentInfo attachmentInfo = new AttachmentInfo();
		attachmentInfo.setComment(resultSet.getString("description"));
		attachmentInfo.setDownloadCount(resultSet.getInt("download_count"));
		attachmentInfo.setFilesize(resultSet.getLong("filesize"));
		attachmentInfo.setMimetype(resultSet.getString("mimetype"));
		attachmentInfo.setPhysicalFilename(resultSet.getString("physical_filename"));
		attachmentInfo.setRealFilename(resultSet.getString("real_filename"));
		attachmentInfo.setUploadTime(new Date(resultSet.getTimestamp("upload_time").getTime()));
		attachmentInfo.setExtension(this.selectExtension(resultSet.getInt("extension_id")));

		attachment.setInfo(attachmentInfo);

		return attachment;
	}

	/**
	 * @see net.jforum.dao.AttachmentDAO#selectAttachmentById(int)
	 */
	public Attachment selectAttachmentById(final int attachId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			Attachment attachment = null;

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectAttachmentById"));
			pstmt.setInt(1, attachId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				attachment = this.getAttachment(resultSet);
			}

			return attachment;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	public boolean isPhysicalDownloadMode(final int extensionGroupId)
	{
		boolean result = true;

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.isPhysicalDownloadMode"));

			pstmt.setInt(1, extensionGroupId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				result = (resultSet.getInt("download_mode") == 2);
			}

			return result;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	public List<TopDownloadInfo> selectTopDownloads(int limit) {
		final List<TopDownloadInfo> list = new ArrayList<TopDownloadInfo>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {			
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("AttachmentModel.selectTopDownloadsByLimit"));
			pstmt.setInt(1, limit);
			
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				TopDownloadInfo tdi = new TopDownloadInfo();
				tdi.setForumId(resultSet.getInt("forum_id"));
				tdi.setForumName(resultSet.getString("forum_name"));
				tdi.setTopicId(resultSet.getInt("topic_id"));
				tdi.setTopicTitle(resultSet.getString("topic_title"));
				tdi.setAttachId(resultSet.getInt("attach_id"));
				tdi.setRealFilename(resultSet.getString("real_filename"));
				tdi.setFilesize(resultSet.getLong("filesize"));
				tdi.setDownloadCount(resultSet.getInt("download_count"));
				list.add(tdi);
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(resultSet, pstmt);
		}
		return list;
	}
}
