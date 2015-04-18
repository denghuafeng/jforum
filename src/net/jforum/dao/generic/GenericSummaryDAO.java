/* Copyright (c) JForum Team
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
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.SummaryDAO;
import net.jforum.entities.Post;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Franklin Samir (franklin (at) portaljava [dot] com)
 * @version $Id$
 */
public class GenericSummaryDAO extends AutoKeys implements SummaryDAO
{
	private static final Logger LOGGER = Logger.getLogger(GenericSummaryDAO.class);
	
	/**
	 * @see net.jforum.dao.SummaryDAO#selectLastPosts(Date, Date)
	 */
	public List<Post> selectLastPosts(Date firstDate, Date lastDate)
	{
		String query = SystemGlobals.getSql("SummaryDAO.selectPosts");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(query);
			pstmt.setTimestamp(1, new Timestamp(firstDate.getTime()));
			pstmt.setTimestamp(2, new Timestamp(lastDate.getTime()));

			List<Post> posts = new ArrayList<Post>();
			rs = pstmt.executeQuery();

			while (rs.next()) {
				posts.add(this.fillPost(rs));
			}

			return posts;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}

	private Post fillPost(ResultSet rs) throws SQLException
	{
		Post post = new Post();

		post.setId(rs.getInt("post_id"));
		post.setTopicId(rs.getInt("topic_id"));
		post.setForumId(rs.getInt("forum_id"));
		post.setUserId(rs.getInt("user_id"));
		Timestamp postTime = rs.getTimestamp("post_time");
		post.setTime(postTime);
		post.setSubject(rs.getString("post_subject"));
		post.setText(this.getPostTextFromResultSet(rs));
		post.setPostUsername(rs.getString("username"));

		post.setKarma(DataAccessDriver.getInstance().newKarmaDAO().getPostKarma(post.getId()));

		LOGGER.debug("Add to Weekly Summary: post.id="+ post.getId() +" post.subject="+ post.getSubject());
		
		return post;
	}

	public List<String> listRecipients()
	{
		String query = SystemGlobals.getSql("SummaryDAO.selectAllRecipients");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(query);
			rs = pstmt.executeQuery();

			List<String> recipients = new ArrayList<String>();
			String mail = null;
			while (rs.next()) {
				mail = rs.getString("user_email");
				LOGGER.debug("user_email=<" + mail + ">");
				if (mail != null && !"".equals(mail.trim())) {
					recipients.add(mail);
					LOGGER.debug("recipients add " + mail);
				}
			}

			return recipients;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}
	
	protected String getPostTextFromResultSet(ResultSet rs) throws SQLException
	{
		return rs.getString("post_text");
	}
}
