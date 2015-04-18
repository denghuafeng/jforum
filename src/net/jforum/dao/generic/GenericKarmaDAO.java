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
 * Created on Jan 11, 2005 11:22:19 PM
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
import net.jforum.entities.Karma;
import net.jforum.entities.KarmaStatus;
import net.jforum.entities.User;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericKarmaDAO implements net.jforum.dao.KarmaDAO
{
	/**
	 * @see net.jforum.dao.KarmaDAO#addKarma(net.jforum.entities.Karma)
	 */
	public void addKarma(final Karma karma)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("KarmaModel.add"));
			pstmt.setInt(1, karma.getPostId());
			pstmt.setInt(2, karma.getPostUserId());
			pstmt.setInt(3, karma.getFromUserId());
			pstmt.setInt(4, karma.getPoints());
			pstmt.setInt(5, karma.getTopicId());
			pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
			pstmt.executeUpdate();

			this.updateUserKarma(karma.getPostUserId());
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.KarmaDAO#getUserKarma(int)
	 */
	public KarmaStatus getUserKarma(final int userId)
	{
		final KarmaStatus status = new KarmaStatus();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("KarmaModel.getUserKarma"));
			pstmt.setInt(1, userId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				status.setKarmaPoints(Math.round(resultSet.getDouble("user_karma")));
			}

			return status;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.KarmaDAO#updateUserKarma(int)
	 */
	public void updateUserKarma(final int userId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("KarmaModel.getUserKarmaPoints"));
			pstmt.setInt(1, userId);

			int totalRecords = 0;
			double totalPoints = 0;
			resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				final int points = resultSet.getInt("points");
				final int votes = resultSet.getInt("votes");

				totalPoints += ((double) points / votes);
				totalRecords++;
			}

			resultSet.close();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("KarmaModel.updateUserKarma"));

			double karmaPoints = totalPoints / totalRecords;

			if (Double.isNaN(karmaPoints)) {
				karmaPoints = 0;
			}

			pstmt.setDouble(1, karmaPoints);
			pstmt.setInt(2, userId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}	

	/**
	 * @see net.jforum.dao.KarmaDAO#update(net.jforum.entities.Karma)
	 */
	public void update(final Karma karma)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("KarmaModel.update"));
			pstmt.setInt(1, karma.getPoints());
			pstmt.setInt(2, karma.getId());
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
	 * @see net.jforum.dao.KarmaDAO#getPostKarma(int)
	 */
	public KarmaStatus getPostKarma(final int postId)
	{
		final KarmaStatus karma = new KarmaStatus();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("KarmaModel.getPostKarma"));
			pstmt.setInt(1, postId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				karma.setKarmaPoints(Math.round(resultSet.getDouble(1)));
			}

			return karma;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

    /**
     * @see net.jforum.dao.KarmaDAO#deletePostKarma(int)
     */
    public void deletePostKarma(final int postId)
    {
        PreparedStatement pstmt = null;
        try {
        	pstmt = JForumExecutionContext.getConnection()
                    .prepareStatement(SystemGlobals.getSql("KarmaModel.deletePostKarma"));
        	pstmt.setInt(1, postId);
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
	 * @see net.jforum.dao.KarmaDAO#userCanAddKarma(int, int)
	 */
	public boolean userCanAddKarma(final int userId, final int postId)
	{
		boolean status = true;

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("KarmaModel.userCanAddKarma"));
			pstmt.setInt(1, postId);
			pstmt.setInt(2, userId);

			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				status = resultSet.getInt(1) < 1;
			}

			return status;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.KarmaDAO#getUserVotes(int, int)
	 */
	public Map<Integer, Integer> getUserVotes(final int topicId, final int userId)
	{
		final Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("KarmaModel.getUserVotes"));
			pstmt.setInt(1, topicId);
			pstmt.setInt(2, userId);

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				map.put(Integer.valueOf(resultSet.getInt("post_id")), Integer.valueOf(resultSet.getInt("points")));
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

	public void getUserTotalKarma(final User user)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("KarmaModel.getUserTotalVotes"));
			pstmt.setInt(1, user.getId());

			resultSet = pstmt.executeQuery();

			user.setKarma(new KarmaStatus());

			if (resultSet.next()) {
				user.getKarma().setTotalPoints(resultSet.getInt("points"));
				user.getKarma().setVotesReceived(resultSet.getInt("votes"));
			}

			if (user.getKarma().getVotesReceived() != 0) {
				// prevetns division by zero.
				user.getKarma().setKarmaPoints(user.getKarma().getTotalPoints() / (double)user.getKarma().getVotesReceived());
			}
			this.getVotesGiven(user);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	private void getVotesGiven(final User user)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("KarmaModel.getUserGivenVotes"));
			pstmt.setInt(1, user.getId());

			resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				user.getKarma().setVotesGiven(resultSet.getInt("votes"));
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
	 * @see net.jforum.dao.KarmaDAO#getMostRatedUserByPeriod(int, java.util.Date, java.util.Date,
	 *      String)
	 */
	public List<User> getMostRatedUserByPeriod(final int start, final Date firstPeriod, final Date lastPeriod, final String orderField)
	{
		String sql = SystemGlobals.getSql("KarmaModel.getMostRatedUserByPeriod");
		sql = new StringBuilder(sql).append(" ORDER BY ").append(orderField).append(" DESC").toString();

		return this.getMostRatedUserByPeriod(sql, firstPeriod, lastPeriod);
	}

	/**
	 * 
	 * @param sql String
	 * @param firstPeriod Date
	 * @param lastPeriod Date
	 * @return List
	 */
	protected List<User> getMostRatedUserByPeriod(final String sql, final Date firstPeriod, final Date lastPeriod)
	{
		if (firstPeriod.after(lastPeriod)) {
			throw new DatabaseException("First Date needs to be before the Last Date");
		}

		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setTimestamp(1, new Timestamp(firstPeriod.getTime()));
			pstmt.setTimestamp(2, new Timestamp(lastPeriod.getTime()));

			resultSet = pstmt.executeQuery();
			return this.fillUser(resultSet);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected List<User> fillUser(final ResultSet resultSet) throws SQLException
	{
		final List<User> usersAndPoints = new ArrayList<User>();
		KarmaStatus karma = null;
		while (resultSet.next()) {
			final User user = new User();
			karma = new KarmaStatus();
			karma.setTotalPoints(resultSet.getInt("total"));
			karma.setVotesReceived(resultSet.getInt("votes_received"));
			karma.setKarmaPoints(resultSet.getDouble("user_karma"));
			karma.setVotesGiven(resultSet.getInt("votes_given"));
			user.setUsername(resultSet.getString("username"));
			user.setId(resultSet.getInt("user_id"));
			user.setKarma(karma);
			usersAndPoints.add(user);
		}
		return usersAndPoints;
	}
}
