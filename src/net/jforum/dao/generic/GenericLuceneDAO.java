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
 * Created on 24/07/2007 10:27:23
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.LuceneDAO;
import net.jforum.entities.Post;
import net.jforum.exceptions.DatabaseException;
import net.jforum.search.SearchPost;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericLuceneDAO implements LuceneDAO
{
	/**
	 * @see net.jforum.dao.LuceneDAO#getPostsToIndex(int, int)
	 */
	public List<Post> getPostsToIndex(int fromPostId, int toPostId)
	{
		List<Post> l = new ArrayList<Post>();
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("SearchModel.getPostsToIndexForLucene"));
			
			pstmt.setInt(1, fromPostId);
			pstmt.setInt(2, toPostId);
			
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				l.add(this.makePost(rs));
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
	
	/**
	 * @see net.jforum.dao.LuceneDAO#firstPostIdByDate(java.util.Date)
	 */
	public int firstPostIdByDate(Date date) 
	{
		return this.getPostIdByDate(date, SystemGlobals.getSql("SearchModel.firstPostIdByDate"));
	}
	
	/**
	 * @see net.jforum.dao.LuceneDAO#lastPostIdByDate(java.util.Date)
	 */
	public int lastPostIdByDate(Date date) 
	{
		return this.getPostIdByDate(date, SystemGlobals.getSql("SearchModel.lastPostIdByDate"));
	}
	
	private int getPostIdByDate(Date date, String query)
	{
		int postId = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(query);
			
			pstmt.setTimestamp(1, new Timestamp(date.getTime()));
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				postId = rs.getInt(1);
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
		
		return postId;
	}
	
	/**
	 * @see net.jforum.dao.LuceneDAO#getPostsData(int[])
	 */
	public List<Post> getPostsData(int[] postIds)
	{
		if (postIds.length == 0) {
			return new ArrayList<Post>();
		}
		
		List<Post> l = new ArrayList<Post>();
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			String sql = SystemGlobals.getSql("SearchModel.getPostsDataForLucene");
			sql = sql.replaceAll(":posts:", this.buildInClause(postIds));
			
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				Post post = this.makePost(rs);
				post.setPostUsername(rs.getString("username"));
				
				l.add(post);
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}

		return resortByPostId(postIds, l);
	}

	/**
	 * SearchModel.getPostsDataForLucene returns the posts in the order they are
	 * stored in the database which may not be the same as the order original requested.
	 * This method resorts them in the original order returned by Lucene.
	 * 
	 * @param postIds
	 * @param l
	 * @return
	 */
	private List<Post> resortByPostId (int[] postIds, List<Post> posts) {
		Map<Integer, Post> postsById = new HashMap<Integer, Post>(postIds.length);
		for (Post post : posts) {
			postsById.put(post.getId(), post);
		}

		List<Post> result = new ArrayList<Post>();
		for (int postId : postIds) {
			Post post = postsById.get(postId);

			// shouldn't be null, but just in case there is no match
			if ( post != null) {
				result.add(post);
			}
		}
		return result;
	}

	private String buildInClause(int[] postIds)
	{
		StringBuilder sb = new StringBuilder(128);
		
		for (int i = 0; i < postIds.length - 1; i++) {
			sb.append(postIds[i]).append(',');
		}
		
		sb.append(postIds[postIds.length - 1]);
		
		return sb.toString();
	}
	
	private Post makePost(ResultSet rs) throws SQLException
	{
		Post post = new SearchPost();
		
		post.setId(rs.getInt("post_id"));
		post.setForumId(rs.getInt("forum_id"));
		post.setTopicId(rs.getInt("topic_id"));
		post.setUserId(rs.getInt("user_id"));
		post.setTime(new Date(rs.getTimestamp("post_time").getTime()));
		post.setText(this.readPostTextFromResultSet(rs));
		post.setBbCodeEnabled(rs.getInt("enable_bbcode") == 1);
		post.setSmiliesEnabled(rs.getInt("enable_smilies") == 1);
		post.hasAttachments(rs.getInt("attach") == 1);

		String subject = rs.getString("post_subject");
		
		if (StringUtils.isBlank(subject)) {
			subject = rs.getString("topic_title");
		}
		
		post.setSubject(subject);
		
		return post;
	}
	
	protected String readPostTextFromResultSet(ResultSet rs) throws SQLException
	{
		return rs.getString("post_text");
	}
}
