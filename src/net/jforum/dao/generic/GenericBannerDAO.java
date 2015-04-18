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
 * Created on Mar 6, 2003 / 11:09:34 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.entities.Banner;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Samuel Yung
 * @version $Id$
 */
public class GenericBannerDAO extends AutoKeys implements net.jforum.dao.BannerDAO
{
	public Banner selectById(final int bannerId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		Banner banner = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BannerDAO.selectById"));
			pstmt.setInt(1, bannerId);

			resultSet = pstmt.executeQuery();

			banner = new Banner();
			if (resultSet.next()) {
				banner = this.getBanner(resultSet);
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}

		return banner;
	}

	public List<Banner> selectAll()
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BannerDAO.selectAll"));
			final List<Banner> list = new ArrayList<Banner>();

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getBanner(resultSet));
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

	protected Banner getBanner(final ResultSet resultSet) throws SQLException
	{
		final Banner banner = new Banner();

		banner.setId(resultSet.getInt("banner_id"));
		banner.setName(resultSet.getString("banner_name"));
		banner.setPlacement(resultSet.getInt("banner_placement"));
		banner.setDescription(resultSet.getString("banner_description"));
		banner.setClicks(resultSet.getInt("banner_clicks"));
		banner.setViews(resultSet.getInt("banner_views"));
		banner.setUrl(resultSet.getString("banner_url"));
		banner.setWeight(resultSet.getInt("banner_weight"));
		banner.setActive(resultSet.getInt("banner_active") == 1);
		banner.setComment(resultSet.getString("banner_comment"));
		banner.setType(resultSet.getInt("banner_type"));
		banner.setWidth(resultSet.getInt("banner_width"));
		banner.setHeight(resultSet.getInt("banner_height"));

		return banner;
	}

	public boolean canDelete(final int bannerId)
	{
		boolean result = true;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BannerDAO.canDelete"));
			pstmt.setInt(1, bannerId);

			resultSet = pstmt.executeQuery();
			if (!resultSet.next() || resultSet.getInt("total") < 1) {
				result = false;
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

	public void delete(final int bannerId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BannerDAO.delete"));
			pstmt.setInt(1, bannerId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	public void update(final Banner banner)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BannerDAO.update"));
			setBannerParam(pstmt, banner);
			pstmt.setInt(13, banner.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	public int addNew(final Banner banner)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = this.getStatementForAutoKeys("BannerDAO.addNew");
			setBannerParam(pstmt, banner);
			final int id = this.executeAutoKeysQuery(pstmt);

			banner.setId(id);
			return id;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	protected void setBannerParam(final PreparedStatement pstmt, final Banner banner) throws SQLException
	{
		pstmt.setString(1, banner.getName());
		pstmt.setInt(2, banner.getPlacement());
		pstmt.setString(3, banner.getDescription());
		pstmt.setInt(4, banner.getClicks());
		pstmt.setInt(5, banner.getViews());
		pstmt.setString(6, banner.getUrl());
		pstmt.setInt(7, banner.getWeight());
		pstmt.setInt(8, banner.isActive() ? 1 : 0);
		pstmt.setString(9, banner.getComment());
		pstmt.setInt(10, banner.getType());
		pstmt.setInt(11, banner.getWidth());
		pstmt.setInt(12, banner.getHeight());
	}

	public List<Banner> selectActiveBannerByPlacement(final int placement)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BannerDAO.selectActiveBannerByPlacement"));
			pstmt.setInt(1, placement);

			final List<Banner> list = new ArrayList<Banner>();

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getBanner(resultSet));
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
}
