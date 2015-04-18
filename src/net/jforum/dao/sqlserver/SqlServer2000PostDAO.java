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
 * Created on 24/05/2004 / 12:04:11
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.sqlserver;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.generic.GenericPostDAO;
import net.jforum.entities.Post;
import net.jforum.exceptions.DatabaseException;
import net.jforum.repository.ForumRepository;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Andre de Andrade da Silva (<a href="mailto:andre.de.andrade@gmail.com">andre.de.andrade@gmail.com</a>)
 * @author Dirk Rasmussen (<a href="mailto:d.rasmussen@bevis.de">d.rasmussen@bevis.de</a>)
 * @author Andowson Chang
 * @version $Id$
 */
public class SqlServer2000PostDAO extends GenericPostDAO
{
	/**
	 * @see net.jforum.dao.PostDAO#selectAllByTopicByLimit(int, int, int)
	 */
	public List<Post> selectAllByTopicByLimit(int topicId, int startFrom, int count)
	{
		List<Post> l = new ArrayList<Post>();

		String sql = SystemGlobals.getSql("PostModel.selectAllByTopicByLimit");
		sql = sql.replaceAll("%d", String.valueOf(startFrom + count));
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setInt(1, topicId);

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
		sql = sql.replaceAll("%d", String.valueOf(startFrom + count));
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);			
			pstmt.setInt(1, userId);

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

    public List<Post> selectLatestByForumForRSS(int forumId, int limit) 
    {
        List<Post> l = new ArrayList<Post>();

        String sql = SystemGlobals.getSql("PostModel.selectLatestByForumForRSS");
        sql = sql.replaceAll("%d", String.valueOf(limit));
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
            pstmt.setInt(1, forumId);
            
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

    public List<Post> selectLatestForRSS(int limit) 
    {
        List<Post> l = new ArrayList<Post>();

        String sql = SystemGlobals.getSql("PostModel.selectLatestForRSS");
        sql = sql.replaceAll("%d", String.valueOf(limit));
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
            
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

        String sql = SystemGlobals.getSql("PostModel.selectHotForRSS");
        sql = sql.replaceAll("%d", String.valueOf(limit));
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
            
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
}
