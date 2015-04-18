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
 * Created on 30/03/2003 / 02:37:20
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.ForumStats;
import net.jforum.entities.LastPostInfo;
import net.jforum.entities.ModeratorInfo;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @author Vanessa Sabino
 * @author socialnetwork@gmail.com, adding "watch forum" methods. 
 * @version $Id$
 */
public class GenericForumDAO extends AutoKeys implements net.jforum.dao.ForumDAO
{
	private static final Logger LOGGER = Logger.getLogger(GenericForumDAO.class);

	/**
	 * @see net.jforum.dao.ForumDAO#selectById(int)
	 */
	public Forum selectById(final int forumId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.selectById"));
			pstmt.setInt(1, forumId);

			resultSet = pstmt.executeQuery();

			Forum forum = new Forum();

			if (resultSet.next()) {
				forum = this.fillForum(resultSet);
			}
			return forum;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected Forum fillForum(final ResultSet resultSet) throws SQLException
	{
		final Forum forum = new Forum();

		forum.setId(resultSet.getInt("forum_id"));
		forum.setIdCategories(resultSet.getInt("categories_id"));
		forum.setName(resultSet.getString("forum_name"));
		forum.setDescription(resultSet.getString("forum_desc"));
		forum.setOrder(resultSet.getInt("forum_order"));
		forum.setTotalTopics(resultSet.getInt("forum_topics"));
		forum.setLastPostId(resultSet.getInt("forum_last_post_id"));
		forum.setModerated(resultSet.getInt("moderated") > 0);
		forum.setTotalPosts(this.countForumPosts(forum.getId()));

		return forum;
	}

	protected int countForumPosts(final int forumId)
	{
		int count = 0;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.countForumPosts"));
			pstmt.setInt(1, forumId);
			resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
			
			return count;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#selectAll()
	 */
	public List<Forum> selectAll()
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.selectAll"));
			final List<Forum> list = new ArrayList<Forum>();

			resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				list.add(this.fillForum(resultSet));
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
	 * @see net.jforum.dao.ForumDAO#setOrderUp(Forum, Forum)
	 */
	public Forum setOrderUp(final Forum forum, final Forum related)
	{
		return this.changeForumOrder(forum, related);
	}

	/**
	 * @see net.jforum.dao.ForumDAO#setOrderDown(Forum, Forum)
	 */
	public Forum setOrderDown(final Forum forum, final Forum related)
	{
		return this.changeForumOrder(forum, related);
	}

	private Forum changeForumOrder(final Forum forum, final Forum related)
	{
		final int tmpOrder = related.getOrder();
		related.setOrder(forum.getOrder());
		forum.setOrder(tmpOrder);

		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("ForumModel.setOrderById"));
			pstmt.setInt(1, forum.getOrder());
			pstmt.setInt(2, forum.getId());
			pstmt.executeUpdate();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("ForumModel.setOrderById"));
			pstmt.setInt(1, related.getOrder());
			pstmt.setInt(2, related.getId());
			pstmt.executeUpdate();

			return this.selectById(forum.getId());
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#delete(int)
	 */
	public void delete(final int forumId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.delete"));
			pstmt.setInt(1, forumId);

			pstmt.executeUpdate();
			
			final GroupSecurityDAO groupSecurity = DataAccessDriver.getInstance().newGroupSecurityDAO();
			groupSecurity.deleteForumRoles(forumId);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#update(net.jforum.entities.Forum)
	 */
	public void update(final Forum forum)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.update"));

			pstmt.setInt(1, forum.getCategoryId());
			pstmt.setString(2, forum.getName());
			pstmt.setString(3, forum.getDescription());
			pstmt.setInt(4, forum.isModerated() ? 1 : 0);
			pstmt.setInt(5, forum.getId());

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
	 * @see net.jforum.dao.ForumDAO#addNew(net.jforum.entities.Forum)
	 */
	public int addNew(final Forum forum)
	{
		// Gets the higher order
		PreparedStatement pOrder = null;
		ResultSet resultSet = null;
		try {
			pOrder = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.getMaxOrder"));
			resultSet = pOrder.executeQuery();

			if (resultSet.next()) {
				forum.setOrder(resultSet.getInt(1) + 1);
			}

			resultSet.close();
			pOrder.close();

			pOrder = this.getStatementForAutoKeys("ForumModel.addNew");

			pOrder.setInt(1, forum.getCategoryId());
			pOrder.setString(2, forum.getName());
			pOrder.setString(3, forum.getDescription());
			pOrder.setInt(4, forum.getOrder());
			pOrder.setInt(5, forum.isModerated() ? 1 : 0);

			this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("ForumModel.lastGeneratedForumId"));
			final int forumId = this.executeAutoKeysQuery(pOrder);

			forum.setId(forumId);
			return forumId;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pOrder);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#setLastPost(int, int)
	 */
	public void setLastPost(final int forumId, final int postId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.updateLastPost"));

			pstmt.setInt(1, postId);
			pstmt.setInt(2, forumId);

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
	 * @see net.jforum.dao.ForumDAO#incrementTotalTopics(int, int)
	 */
	public void incrementTotalTopics(final int forumId, final int count)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.incrementTotalTopics"));
			pstmt.setInt(1, count);
			pstmt.setInt(2, forumId);
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
	 * @see net.jforum.dao.ForumDAO#decrementTotalTopics(int, int)
	 */
	public void decrementTotalTopics(final int forumId, final int count)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.decrementTotalTopics"));
			pstmt.setInt(1, count);
			pstmt.setInt(2, forumId);
			pstmt.executeUpdate();
			pstmt.close();

			// If there are no more topics, then clean the
			// last post id information
			final int totalTopics = this.getTotalTopics(forumId);
			if (totalTopics < 1) {
				this.setLastPost(forumId, 0);
			}
			
			// Fix Forum's total_topics field value
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.fixTotalTopics"));
			pstmt.setInt(1, totalTopics);
			pstmt.setInt(2, forumId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	private LastPostInfo getLastPostInfo(final int forumId, boolean origTryFix)
	{
		boolean tryFix = origTryFix;
		final LastPostInfo lpi = new LastPostInfo();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("ForumModel.lastPostInfo"));
			pstmt.setInt(1, forumId);

			resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				lpi.setUsername(resultSet.getString("username"));
				lpi.setUserId(resultSet.getInt("user_id"));

				lpi.setPostDate(resultSet.getTimestamp("post_time"));
				lpi.setPostId(resultSet.getInt("post_id"));
				lpi.setTopicId(resultSet.getInt("topic_id"));
				lpi.setPostTimeMillis(resultSet.getTimestamp("post_time").getTime());
				lpi.setTopicReplies(resultSet.getInt("topic_replies"));

				lpi.setHasInfo(true);
				lpi.setTitle(resultSet.getString("topic_title"));

				// Check if the topic is consistent
				TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
				Topic topic = topicDao.selectById(lpi.getTopicId());

				if (topic.getId() == 0) {
					// Hm, that's not good. Try to fix it
					topicDao.fixFirstLastPostId(lpi.getTopicId());
				}

				tryFix = false;
			}
			else if (tryFix) {
				resultSet.close();
				pstmt.close();

				int postId = this.getMaxPostId(forumId);

				pstmt = JForumExecutionContext.getConnection().prepareStatement(
						SystemGlobals.getSql("ForumModel.latestTopicIdForfix"));
				pstmt.setInt(1, forumId);
				resultSet = pstmt.executeQuery();

				if (resultSet.next()) {
					int topicId;
					topicId = resultSet.getInt("topic_id");

					resultSet.close();
					pstmt.close();

					// Topic
					pstmt = JForumExecutionContext.getConnection().prepareStatement(
							SystemGlobals.getSql("ForumModel.fixLatestPostData"));
					pstmt.setInt(1, postId);
					pstmt.setInt(2, topicId);
					pstmt.executeUpdate();
					pstmt.close();

					// Forum
					pstmt = JForumExecutionContext.getConnection().prepareStatement(
							SystemGlobals.getSql("ForumModel.fixForumLatestPostData"));
					pstmt.setInt(1, postId);
					pstmt.setInt(2, forumId);
					pstmt.executeUpdate();
				}
			}

			return (tryFix ? this.getLastPostInfo(forumId, false) : lpi);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#getLastPostInfo(int)
	 */
	public LastPostInfo getLastPostInfo(final int forumId)
	{
		return this.getLastPostInfo(forumId, true);
	}

	/**
	 * @see net.jforum.dao.ForumDAO#getModeratorList(int)
	 */
	public List<ModeratorInfo> getModeratorList(final int forumId)
	{
		List<ModeratorInfo> list = new ArrayList<ModeratorInfo>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.getModeratorList"));
			pstmt.setInt(1, forumId);

			resultSet = pstmt.executeQuery();
			
			while (resultSet.next()) {
				ModeratorInfo moderatorInfo = new ModeratorInfo();
				moderatorInfo.setId(resultSet.getInt("id"));
				moderatorInfo.setName(resultSet.getString("name"));

				// avoid duplicate user_id
				boolean notExists = true;
				for (ModeratorInfo mi : list) {
					if (mi.getId() == moderatorInfo.getId()) {
						notExists = false;
						break;
					}
				}
				if (notExists) {
					list.add(moderatorInfo);
				}
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
	 * @see net.jforum.dao.ForumDAO#getTotalMessages()
	 */
	public int getTotalMessages()
	{
		int totalMessages = 0;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.totalMessages"));
			resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				totalMessages = resultSet.getInt("total_messages");
			}

			return totalMessages;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#getTotalTopics(int)
	 */
	public int getTotalTopics(final int forumId)
	{
		int total = 0;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.getTotalTopics"));
			pstmt.setInt(1, forumId);
			resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				total = resultSet.getInt(1);
			}

			return total;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#getMaxPostId(int)
	 */
	public int getMaxPostId(final int forumId)
	{
		int id = -1;

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("ForumModel.getMaxPostId"));
			pstmt.setInt(1, forumId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				id = resultSet.getInt("post_id");
			}

			return id;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#moveTopics(java.lang.String[], int, int)
	 */
	public void moveTopics(final String[] topics, final int fromForumId, final int toForumId)
	{
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		try {
			pstmt1 = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.moveTopics"));
			pstmt2 = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("PostModel.setForumByTopic"));

			pstmt1.setInt(1, toForumId);
			pstmt1.setInt(2, fromForumId);
			
			pstmt2.setInt(1, toForumId);

			TopicDAO tdao = DataAccessDriver.getInstance().newTopicDAO();

			Forum forum = this.selectById(toForumId);

			for (int i = 0; i < topics.length; i++) {
				int topicId = Integer.parseInt(topics[i]);
				pstmt1.setInt(3, topicId);
				pstmt2.setInt(2, topicId);

				pstmt1.executeUpdate();
				pstmt2.executeUpdate();

				tdao.setModerationStatusByTopic(topicId, forum.isModerated());
			}

			this.decrementTotalTopics(fromForumId, topics.length);
			this.incrementTotalTopics(toForumId, topics.length);

			this.setLastPost(fromForumId, this.getMaxPostId(fromForumId));
			this.setLastPost(toForumId, this.getMaxPostId(toForumId));
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt1);
			DbUtils.close(pstmt2);
		}
	}

	/**
	 * @see net.jforum.dao.ForumDAO#checkUnreadTopics(int, long)
	 */
	public List<Topic> checkUnreadTopics(final int forumId, final long lastVisit)
	{
		final List<Topic> list = new ArrayList<Topic>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.checkUnreadTopics"));
			pstmt.setInt(1, forumId);
			pstmt.setTimestamp(2, new Timestamp(lastVisit));

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				Topic topic = new Topic();
				topic.setId(resultSet.getInt("topic_id"));
				topic.setTime(new Date(resultSet.getTimestamp(1).getTime()));

				list.add(topic);
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
	 * @see net.jforum.dao.ForumDAO#setModerated(int, boolean)
	 */
	public void setModerated(final int categoryId, final boolean status)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("ForumModel.setModerated"));
			pstmt.setInt(1, status ? 1 : 0);
			pstmt.setInt(2, categoryId);
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
	 * @see net.jforum.dao.ForumDAO#getBoardStatus()
	 */
	public ForumStats getBoardStatus()
	{
		ForumStats forumStats = new ForumStats();
		forumStats.setPosts(this.getTotalMessages());

		Connection conn = JForumExecutionContext.getConnection();

		// Total Users
		Statement stmt = null;
		ResultSet resultSet = null;

		try {
			stmt = conn.createStatement();
			resultSet = stmt.executeQuery(SystemGlobals.getSql("UserModel.totalUsers"));
			resultSet.next();
			forumStats.setUsers(resultSet.getInt(1));
			resultSet.close();
			stmt.close();

			// Total Topics
			stmt = conn.createStatement();
			resultSet = stmt.executeQuery(SystemGlobals.getSql("TopicModel.totalTopics"));
			resultSet.next();
			forumStats.setTopics(resultSet.getInt(1));
			resultSet.close();
			stmt.close();

			// Posts per day
			double postPerDay = 0;
			
			// Topics per day
			double topicPerDay = 0;
			
			// user per day
			double userPerDay = 0;

			stmt = conn.createStatement();
			resultSet = stmt.executeQuery(SystemGlobals.getSql("ForumModel.statsFirstPostTime"));
			if (resultSet.next()) {

				Timestamp firstTime = resultSet.getTimestamp(1);
				if (resultSet.wasNull()) {
					firstTime = null;
				}
				resultSet.close();
				stmt.close();

				Date today = new Date();

				postPerDay = firstTime == null ? 0 : (double)forumStats.getPosts() / this.daysUntilToday(today, firstTime);

				if (forumStats.getPosts() > 0 && postPerDay < 1) {
					postPerDay = 1;
				}

				topicPerDay = firstTime == null ? 0 : (double)forumStats.getTopics() / this.daysUntilToday(today, firstTime);

				// Users per day
				stmt = conn.createStatement();
				resultSet = stmt.executeQuery(SystemGlobals.getSql("ForumModel.statsFirstRegisteredUserTime"));
				if (resultSet.next()) {
					firstTime = resultSet.getTimestamp(1);
					if (resultSet.wasNull()) {
						firstTime = null;
					}
				}

				userPerDay = firstTime == null ? 0 : (double)forumStats.getUsers() / this.daysUntilToday(today, firstTime);
			}

			forumStats.setPostsPerDay(postPerDay);
			forumStats.setTopicsPerDay(topicPerDay);
			forumStats.setUsersPerDay(userPerDay);

			return forumStats;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, stmt);
		}
	}

	private int daysUntilToday(final Date today, final Date from)
	{
		int days = (int) ((today.getTime() - from.getTime()) / (24 * 60 * 60 * 1000));
		return days == 0 ? 1 : days;
	}

	/**
	 * This code is written by looking at GenericTopicDAO.java
	 * 
	 * @see net.jforum.dao.ForumDAO#notifyUsers(Forum)
	 */
	public List<User> notifyUsers(final Forum forum)
	{
		int posterId = SessionFacade.getUserSession().getUserId();
		int anonUser = SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID);

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.notifyUsers"));

			pstmt.setInt(1, forum.getId());
			pstmt.setInt(2, posterId); // don't notify the poster
			pstmt.setInt(3, anonUser); // don't notify the anonymous user

			resultSet = pstmt.executeQuery();
			final List<User> users = new ArrayList<User>();
			
			while (resultSet.next()) {
				final User user = new User();

				user.setId(resultSet.getInt("user_id"));
				user.setEmail(resultSet.getString("user_email"));
				user.setUsername(resultSet.getString("username"));
				user.setLang(resultSet.getString("user_lang"));				
				user.setNotifyText(resultSet.getInt("user_notify_text") == 1);

				users.add(user);
			}
			
			return users;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}

	}

	public void subscribeUser(final int forumId, final int userId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.subscribeUser"));

			pstmt.setInt(1, forumId);
			pstmt.setInt(2, userId);

			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}

	}

	public boolean isUserSubscribed(int forumId, int userId)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.isUserSubscribed"));

			pstmt.setInt(1, forumId);
			pstmt.setInt(2, userId);

			rs = pstmt.executeQuery();

			return rs.next();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	public void removeSubscription(int forumId, int userId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.removeSubscription"));
			pstmt.setInt(1, forumId);
			pstmt.setInt(2, userId);

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
	 * Remove all subscriptions on a forum, such as when a forum is locked. It is not used now.
	 * 
	 * @param forumId int
	 */
	public void removeSubscriptionByForum(int forumId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("ForumModel.removeSubscriptionByForum"));
			pstmt.setInt(1, forumId);

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
	 * @see net.jforum.dao.ForumDAO#discoverForumId(java.lang.String)
	 */
	public int discoverForumId(String listEmail)
	{
		int forumId = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("ForumModel.discoverForumId"));
			pstmt.setString(1, listEmail);
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				forumId = rs.getInt(1);
			}
		}
		catch (SQLException e) {
			LOGGER.error(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}

		return forumId;
	}

    /**
    * Returns all forums that are watched by a given user.
    * @param userId The user id
    */
    public List<Map<String, Object>> selectWatchesByUser(int userID) {
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        PreparedStatement p = null;
        ResultSet rs = null;
        try {
			p = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("ForumModel.selectWatchesByUser"));
            p.setInt(1, userID);
            rs = p.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("id", rs.getInt("forum_id"));
                m.put("forumName", rs.getString("forum_name"));
                l.add(m);
            }
            return l;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            DbUtils.close(p);
        }
    }
}
