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
 * Created on Mar 28, 2003 / 22:57:43 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Post;
import net.jforum.exceptions.DatabaseException;
import net.jforum.repository.ForumRepository;
import net.jforum.search.SearchFacade;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @author Vanessa Sabino
 * @version $Id$
 */
public class GenericPostDAO extends AutoKeys implements net.jforum.dao.PostDAO
{
	/**
	 * @see net.jforum.dao.PostDAO#selectById(int)
	 */
	public Post selectById(int postId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("PostModel.selectById"));
			pstmt.setInt(1, postId);

			rs = pstmt.executeQuery();

			Post post = new Post();

			if (rs.next()) {
				post = this.makePost(rs);
			}

			return post;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	protected Post makePost(ResultSet rs) throws SQLException
	{
		Post post = new Post();
		post.setId(rs.getInt("post_id"));
		post.setTopicId(rs.getInt("topic_id"));
		post.setForumId(rs.getInt("forum_id"));
		post.setUserId(rs.getInt("user_id"));

		Timestamp postTime = rs.getTimestamp("post_time");
		post.setTime(new Date(postTime.getTime()));
		post.setUserIp(rs.getString("poster_ip"));
		post.setBbCodeEnabled(rs.getInt("enable_bbcode") > 0);
		post.setHtmlEnabled(rs.getInt("enable_html") > 0);
		post.setSmiliesEnabled(rs.getInt("enable_smilies") > 0);
		post.setSignatureEnabled(rs.getInt("enable_sig") > 0);
		try {
			post.setViewOnline(rs.getInt("user_viewonline") > 0);
		} catch (Exception ex) { /* may not be there */ }
		post.setEditCount(rs.getInt("post_edit_count"));

		Timestamp editTime = rs.getTimestamp("post_edit_time");
		post.setEditTime(editTime != null ? new Date(editTime.getTime()) : null);

		post.setSubject(rs.getString("post_subject"));
		post.setText(this.getPostTextFromResultSet(rs));
		post.setPostUsername(rs.getString("username"));
		post.hasAttachments(rs.getInt("attach") > 0);
		post.setModerate(rs.getInt("need_moderate") == 1);

		post.setKarma(DataAccessDriver.getInstance().newKarmaDAO().getPostKarma(post.getId()));

		return post;
	}

	/**
	 * Utility method to read the post text from the result set. This method may be useful when
	 * using some "non-standard" way to store text, like oracle does when using (c|b)lob
	 *
	 * @param rs The result set to fetch data from
	 * @return The post text string
	 * @throws SQLException
	 */
	protected String getPostTextFromResultSet(ResultSet rs) throws SQLException
	{
		return rs.getString("post_text");
	}

	/**
	 * @see net.jforum.dao.PostDAO#delete(Post)
	 */
	public void delete(Post post)
	{
		List<Post> l = new ArrayList<Post>();
		l.add(post);
		this.removePosts(l);
	}

	private void removePosts(List<Post> posts)
	{
		PreparedStatement pstmtPost = null;
		PreparedStatement pstmtText = null;
		UserDAO userDAO = DataAccessDriver.getInstance().newUserDAO();

		try {
			pstmtPost = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("PostModel.deletePost"));

			pstmtText = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("PostModel.deletePostText"));

			for (Iterator<Post> iter = posts.iterator(); iter.hasNext();) {
				Post post = iter.next();

				pstmtPost.setInt(1, post.getId());
				pstmtText.setInt(1, post.getId());

				pstmtText.executeUpdate();
				pstmtPost.executeUpdate();

				SearchFacade.delete(post);
				userDAO.decrementPosts(post.getUserId());
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmtPost);
			DbUtils.close(pstmtText);
		}
	}

	/**
	 * @see net.jforum.dao.PostDAO#deleteByTopic(int)
	 */
	public void deleteByTopic(int topicId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = JForumExecutionContext.getConnection()
				.prepareStatement(SystemGlobals.getSql("PostModel.deleteByTopic"));
			pstmt.setInt(1, topicId);
			rs = pstmt.executeQuery();

			List<Post> posts = new ArrayList<Post>();

			while (rs.next()) {
				Post post = new Post();
				post.setId(rs.getInt("post_id"));
				post.setUserId(rs.getInt("user_id"));

				posts.add(post);
			}

			this.removePosts(posts);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.PostDAO#update(net.jforum.entities.Post)
	 */
	public void update(Post post)
	{
		this.updatePostsTable(post);
		this.updatePostsTextTable(post);

		SearchFacade.update(post);
	}

	protected void updatePostsTextTable(Post post)
	{
		PreparedStatement pstmt = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("PostModel.updatePostText"));
			pstmt.setString(1, post.getText());
			pstmt.setString(2, post.getSubject());
			pstmt.setInt(3, post.getId());

			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	protected void updatePostsTable(Post post)
	{
		PreparedStatement pstmt = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("PostModel.updatePost"));
			pstmt.setInt(1, post.getTopicId());
			pstmt.setInt(2, post.getForumId());
			pstmt.setInt(3, post.isBbCodeEnabled() ? 1 : 0);
			pstmt.setInt(4, post.isHtmlEnabled() ? 1 : 0);
			pstmt.setInt(5, post.isSmiliesEnabled() ? 1 : 0);
			pstmt.setInt(6, post.isSignatureEnabled() ? 1 : 0);
			pstmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
			pstmt.setInt(8, post.getEditCount() + 1);
			pstmt.setString(9, post.getUserIp());
			pstmt.setInt(10, post.getId());

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
	 * @see net.jforum.dao.PostDAO#addNew(net.jforum.entities.Post)
	 */
	public int addNew(Post post)
	{
		try {
			this.addNewPost(post);
			this.addNewPostText(post);

			return post.getId();
		}
		catch (Exception e) {
			throw new DatabaseException(e);
		}
	}

	public void index (Post post)
	{
		SearchFacade.create(post);
	}

	protected void addNewPostText(Post post) throws Exception
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PostModel.addNewPostText"));
			pstmt.setInt(1, post.getId());
			pstmt.setString(2, post.getText());
			pstmt.setString(3, post.getSubject());
			pstmt.executeUpdate();
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	protected void addNewPost(Post post)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = this.getStatementForAutoKeys("PostModel.addNewPost");

			pstmt.setInt(1, post.getTopicId());
			pstmt.setInt(2, post.getForumId());
			pstmt.setLong(3, post.getUserId());
			pstmt.setTimestamp(4, new Timestamp(post.getTime().getTime()));
			pstmt.setString(5, post.getUserIp());
			pstmt.setInt(6, post.isBbCodeEnabled() ? 1 : 0);
			pstmt.setInt(7, post.isHtmlEnabled() ? 1 : 0);
			pstmt.setInt(8, post.isSmiliesEnabled() ? 1 : 0);
			pstmt.setInt(9, post.isSignatureEnabled() ? 1 : 0);
			pstmt.setInt(10, post.isModerationNeeded() ? 1 : 0);

			this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("PostModel.lastGeneratedPostId"));
			int postId = this.executeAutoKeysQuery(pstmt);
			post.setId(postId);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.PostDAO#selectAllByTopic(int)
	 */
	public List<Post> selectAllByTopic(int topicId)
	{
		return this.selectAllByTopicByLimit(topicId, 0, Integer.MAX_VALUE - 1);
	}

	/**
	 * @see net.jforum.dao.PostDAO#selectAllByTopicByLimit(int, int, int)
	 */
	public List<Post> selectAllByTopicByLimit(int topicId, int startFrom, int count)
	{
		List<Post> l = new ArrayList<Post>();

		String sql = SystemGlobals.getSql("PostModel.selectAllByTopicByLimit");

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setInt(1, topicId);
			pstmt.setInt(2, startFrom);
			pstmt.setInt(3, count);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				l.add(this.makePost(rs));
			}

			return l;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.PostDAO#selectByUserByLimit(int, int, int)
	 */
	public List<Post> selectByUserByLimit(int userId, int startFrom, int count)
	{
		String sql = SystemGlobals.getSql("PostModel.selectByUserByLimit");
		sql = sql.replaceAll(":fids:", ForumRepository.getListAllowedForums());

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);

			pstmt.setInt(1, userId);
			pstmt.setInt(2, startFrom);
			pstmt.setInt(3, count);

			rs = pstmt.executeQuery();
			List<Post> l = new ArrayList<Post>();

			while (rs.next()) {
				l.add(this.makePost(rs));
			}

			return l;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	public int countUserPosts(int userId)
	{
		int total = 0;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("PostModel.countUserPosts").replaceAll(":fids:",
							ForumRepository.getListAllowedForums()));
			pstmt.setInt(1, userId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				total = rs.getInt(1);
			}

			return total;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.PostDAO#countPreviousPosts(int)
	 */
	public int countPreviousPosts(int postId)
	{
		int total = 0;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("PostModel.countPreviousPosts"));
			pstmt.setInt(1, postId);
			pstmt.setInt(2, postId);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				total = rs.getInt(1);
			}

			return total;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	public List<Post> selectLatestByForumForRSS(int forumId, int limit)
	{
		List<Post> l = new ArrayList<Post>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PostModel.selectLatestByForumForRSS"));
			pstmt.setInt(1, forumId);
			pstmt.setInt(2, limit);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				Post post = this.buildPostForRSS(rs);
				l.add(post);
			}

		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}

		return l;
	}

	public List<Post> selectLatestForRSS(int limit) {
		List<Post> l = new ArrayList<Post>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PostModel.selectLatestForRSS"));
			pstmt.setInt(1, limit);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				Post post = this.buildPostForRSS(rs);
				l.add(post);
			}

		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}

		return l;
	}

	public List<Post> selectHotForRSS(int limit)
	{
		List<Post> l = new ArrayList<Post>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("PostModel.selectHotForRSS"));
			pstmt.setInt(1, limit);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				Post post = this.buildPostForRSS(rs);
				l.add(post);
			}

		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}

		return l;
	}

	protected Post buildPostForRSS(ResultSet rs) throws SQLException
	{
		Post post = new Post();

		post.setId(rs.getInt("post_id"));
		post.setSubject(rs.getString("subject"));
		post.setText(this.getPostTextFromResultSet(rs));
		post.setTopicId(rs.getInt("topic_id"));
		post.setForumId(rs.getInt("forum_id"));
		post.setUserId(rs.getInt("user_id"));
		post.setPostUsername(rs.getString("username"));
		post.setTime(new Date(rs.getTimestamp("post_time").getTime()));

		return post;
	}
}
