/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
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
 * Created on Jan 16, 2005 12:47:31 PM
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
import net.jforum.entities.Bookmark;
import net.jforum.entities.BookmarkType;
import net.jforum.exceptions.DatabaseException;
import net.jforum.exceptions.InvalidBookmarkTypeException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericBookmarkDAO implements net.jforum.dao.BookmarkDAO
{
	/**
	 * @see net.jforum.dao.BookmarkDAO#add(net.jforum.entities.Bookmark)
	 */
	public void add(final Bookmark bookmark)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BookmarkModel.add"));
			pstmt.setInt(1, bookmark.getUserId());
			pstmt.setInt(2, bookmark.getRelationId());
			pstmt.setInt(3, bookmark.getRelationType());
			pstmt.setInt(4, bookmark.isPublicVisible() ? 1 : 0);
			pstmt.setString(5, bookmark.getTitle());
			pstmt.setString(6, bookmark.getDescription());
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
	 * @see net.jforum.dao.BookmarkDAO#update(net.jforum.entities.Bookmark)
	 */
	public void update(final Bookmark bookmark)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BookmarkModel.update"));
			pstmt.setInt(1, bookmark.isPublicVisible() ? 1 : 0);
			pstmt.setString(2, bookmark.getTitle());
			pstmt.setString(3, bookmark.getDescription());
			pstmt.setInt(4, bookmark.getId());
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
	 * @see net.jforum.dao.BookmarkDAO#remove(int)
	 */
	public void remove(final int bookmarkId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("BookmarkModel.remove"));
			pstmt.setInt(1, bookmarkId);
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
	 * @see net.jforum.dao.BookmarkDAO#selectByUser(int, int)
	 */
	public List<Bookmark> selectByUser(final int userId, final int relationType)
	{
		List<Bookmark> list = null;
		
		if (relationType == BookmarkType.FORUM) {
			list = this.getForums(userId);
		}
		else if (relationType == BookmarkType.TOPIC) {
			list = this.getTopics(userId);
		}
		else if (relationType == BookmarkType.USER) {
			list = this.getUsers(userId);
		}
		else {
			throw new InvalidBookmarkTypeException("The type " + relationType + " is not a valid bookmark type");
		}
		return list;
	}

	/**
	 * @see net.jforum.dao.BookmarkDAO#selectByUser(int)
	 */
	public List<Bookmark> selectByUser(final int userId)
	{
		final List<Bookmark> list = new ArrayList<Bookmark>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectAllFromUser"));
			pstmt.setInt(1, userId);
			pstmt.setInt(2, userId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				Bookmark bookmark = this.getBookmark(resultSet);
				bookmark.setForumId(resultSet.getInt("forum_id"));
				list.add(bookmark);
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
	 * @see net.jforum.dao.BookmarkDAO#selectById(int)
	 */
	public Bookmark selectById(final int bookmarkId)
	{
		Bookmark bookmark = null;

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectById"));
			pstmt.setInt(1, bookmarkId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				bookmark = this.getBookmark(resultSet);
			}

			return bookmark;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.BookmarkDAO#selectForUpdate(int, int, int)
	 */
	public Bookmark selectForUpdate(final int relationId, final int relationType, final int userId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectForUpdate"));
			pstmt.setInt(1, relationId);
			pstmt.setInt(2, relationType);
			pstmt.setInt(3, userId);

			Bookmark bookmark = null;
			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				bookmark = this.getBookmark(resultSet);
			}

			return bookmark;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected List<Bookmark> getUsers(final int userId)
	{
		final List<Bookmark> list = new ArrayList<Bookmark>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectUserBookmarks"));
			pstmt.setInt(1, userId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final Bookmark bookmark = this.getBookmark(resultSet);

				if (bookmark.getTitle() == null || "".equals(bookmark.getTitle())) {
					bookmark.setTitle(resultSet.getString("username"));
				}

				list.add(bookmark);
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

	protected List<Bookmark> getTopics(final int userId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			final List<Bookmark> list = new ArrayList<Bookmark>();
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectTopicBookmarks"));
			pstmt.setInt(1, userId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final Bookmark bookmark = this.getBookmark(resultSet);

				if (bookmark.getTitle() == null || "".equals(bookmark.getTitle())) {
					bookmark.setTitle(resultSet.getString("topic_title"));
				}

				list.add(bookmark);
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

	protected List<Bookmark> getForums(final int userId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			final List<Bookmark> list = new ArrayList<Bookmark>();
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("BookmarkModel.selectForumBookmarks"));
			pstmt.setInt(1, userId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final Bookmark bookmark = this.getBookmark(resultSet);

				if (bookmark.getTitle() == null || "".equals(bookmark.getTitle())) {
					bookmark.setTitle(resultSet.getString("forum_name"));
				}

				if (bookmark.getDescription() == null || "".equals(bookmark.getDescription())) {
					bookmark.setDescription(resultSet.getString("forum_desc"));
				}

				list.add(bookmark);
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

	protected Bookmark getBookmark(final ResultSet resultSet) throws SQLException
	{
		final Bookmark bookmark = new Bookmark();
		bookmark.setId(resultSet.getInt("bookmark_id"));
		bookmark.setDescription(resultSet.getString("description"));
		bookmark.setPublicVisible(resultSet.getInt("public_visible") == 1);
		bookmark.setRelationId(resultSet.getInt("relation_id"));
		bookmark.setTitle(resultSet.getString("title"));
		bookmark.setDescription(resultSet.getString("description"));
		bookmark.setUserId(resultSet.getInt("user_id"));
		bookmark.setRelationType(resultSet.getInt("relation_type"));

		return bookmark;
	}
}
