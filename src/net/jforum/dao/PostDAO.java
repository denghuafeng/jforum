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
 * Created on Feb 23, 2003 / 2:49:48 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao;

import java.util.List;

import net.jforum.entities.Post;

/**
 * Model interface for {@link net.jforum.entities.Post}.
 * This interface defines methods which are expected to be
 * implemented by a specific data access driver. The intention is
 * to provide all functionality needed to update, insert, delete and
 * select some specific data.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public interface PostDAO 
{
	/**
	 * Gets a specific <code>Post</code>.
	 * 
	 * @param postId The Post ID to search
	 * @return <code>Post</code>object containing all the information
	 */
	Post selectById(int postId) ;
		
	/**
	 * Delete a Post.
	 * 
	 * @param post Post The Post to delete
	 */
	void delete(Post post) ;
	
	/**
	 * Updates a Post.
	 * 
	 * @param post Reference to a <code>Post</code> object to update
	 */
	void update(Post post) ;
	
	/**
	 * Adds a new Post.
	 * 
	 * @param post Post Reference to a valid and configured <code>Post</code> object
	 * @return The new ID
	 */
	int addNew(Post post) ;
		
	/**
	 * Adds the post, and optionally its attachments, to the Lucene index.
	 * 
	 * @param post Post Reference to a valid and configured <code>Post</code> object
	 */
	void index (Post post) ;

	/**
	 * Selects all messages related to a specific topic. 
	 * 
	 * @param topicId The topic ID 
	 * @param startFrom The count position to start fetching
	 * @param count The total number of records to retrieve
	 * @return <code>ArrayList</code> containing all records found. Each entry of the <code>ArrayList</code> is a {@link net.jforum.entities.Post} object
	 */
	List<Post> selectAllByTopicByLimit(int topicId, int startFrom, int count) ;

    /**
	 * Selects all posts associated to a specific user and belonging to 
	 * given forums
	 * @param userId int User ID.
	 * @param startFrom int
	 * @param count int
	 * @return  List
	 */
	List<Post> selectByUserByLimit(int userId,int startFrom, int count) ;

    /**
     * Count user posts.
     * @param userId int
     * @return int
     */
	int countUserPosts(int userId) ;

	/**
	 * Selects all messages related to a specific topic. 
	 * 
	 * @param topicId The topic ID 
	 * @return <code>ArrayList</code> containing all records found. Each entry of the <code>ArrayList</code> is a {@link net.jforum.entities.Post} object
	 */
	List<Post> selectAllByTopic(int topicId) ;
	
	/**
	 * Delete all posts related to the given topic
	 * 
	 * @param topicId int
	 */
	void deleteByTopic(int topicId) ;

	/**
	 * Count how many previous posts there are before the given post id
	 * @param postId int
	 * @return int
	 */
	int countPreviousPosts(int postId) ;
	
	List<Post> selectLatestByForumForRSS(int forumId, int limit) ;
	
	List<Post> selectLatestForRSS(int limit) ;
	
	List<Post> selectHotForRSS(int limit) ;
}
