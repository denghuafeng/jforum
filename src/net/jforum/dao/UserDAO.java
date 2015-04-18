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
 * Created on Feb 19, 2003 / 8:56:56 PM
 * The JForum Project
 * http://www.jforum.net 
 */
package net.jforum.dao;

import java.util.List;

import net.jforum.entities.User;

/**
 * Model interface for {@link net.jforum.entities.User}.
 * This interface defines methods which are expected to be
 * implemented by a specific data access driver. The intention is
 * to provide all functionality needed to update, insert, delete and
 * select some specific data.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public interface UserDAO 
{
	/**
	 * Gets a specific <code>User</code>.
	 * 
	 * @param userId The User ID to search
	 * @return <code>User</code>object containing all the information
	 * @see #selectAll
	 */
	User selectById(int userId) ;
	
	/**
	 * Gets a specific <code>User</code>.
	 * 
	 * @param username The User name to search
	 * @return <code>User</code> object containing all the information
	 * or <code>null</code> if no data was found. 
	 * @see #selectAll
	 */
	User selectByName(String username) ;
	
	/**
	 * Gets all users
	 * 
	 * @return an <code>ArrayList</code> with the users. Each entry is a <code>User</code> object
	 */
	List<User> selectAll() ;
	
	/**
	 * Gets all users with your Karma.
	 * 
	 * @return a <code>List</code> with the users. Each entry is a <code>User</code> object
	 * (with the KarmaStatus populated).
	 */
	List<User> selectAllWithKarma() ;
	
	/**
	 * Gets all users from a specific group.
	 * 
	 * @param groupId The group id
	 * @param start The index position to start fetching
	 * @param count The total number of records to fetch
	 * @return <code>List</code> with the users. Each entry is a <code>User</code> object
	 */
	List<User> selectAllByGroup(int groupId, int start, int count) ;
	
	/**
	 * Gets all users
	 *
	 * @param startFrom Index to start fetching from
	 * @param count Number of records to retrieve
	 * @return an <code>ArrayList</code> with the users. Each entry is a <code>User</code> object
	 * (with the KarmaStatus populated).
	 */
	List<User> selectAllWithKarma(int startFrom, int count) ;
	
	/**
	 * Finds a user by matching an input string. 
	 * 
	 * @param input The username to search. May be part of the username. 
	 * The method will match all users who have the input string as 
	 * part of their usernames.
	 * @param exactMath Set to <code>true</code> to get the user data related to 
	 * the username passed as argument, and set it to <code>false</code> to 
	 * search all users who match the criteria. 
	 * @return a <code>List</code> with the found users. Each entry is a 
	 * <code>User</code> object, where only the <i>id</i> and <i>username</i>
	 * members are filled.
	 */
	List<User> findByName(String input, boolean exactMath) ;
	
	/**
	 * Gets all users
	 *
	 * @param startFrom Index to start fetching from
	 * @param count Number of records to retrieve
	 * @return an <code>ArrayList</code> with the users. Each entry is a <code>User</code> object
	 */
	List<User> selectAll(int startFrom, int count) ;
	

	/**
	 * Deletes a user.
	 * 
	 * @param userId The user ID to delete
	 * @see #undelete(int)
	 */
	void delete(int userId) ;
	
	/**
	 * Undeletes a user.
	 * The system allows user undeletation because when you 
	 * call {@link #delete(int)} the user isn't physically deleted of the
	 * database, but just marked as deleted. This is done to ensure
	 * data integrity.
	 * 
	 * @param userId The user ID to undelete
	 * @see #delete(int)
	 */
	void undelete(int userId) ;
	
	/**
	 * Updates a user.
	 * 
	 * @param user Reference to a <code>User</code> object to update
	 */
	void update(User user) ;
	
	/**
	 * Adds a new User.
	 * After successfully persisting the data, this method
	 * <b>should</b> call <code>user.setId(theNewId);</code>, as well
	 * return the new user id. 
	 * @param user Reference to a valid and configured <code>User</code> object
	 * @return The new user id
	 */
	int addNew(User user) ;

	/**
	 * Adds a new user with a predefined user id
	 * 
	 * (added by Pieter for external login support)
	 * @param user Reference to a valid and configured <code>User</code> object, with the user id already set
	 */
	void addNewWithId(User user)  ;
	
	/**
	 * Set the active status.
	 * a user with the active status equals to false cannot be considered
	 * a "official", "fully registered" user until its status is set to true. This is
	 * interesting when you want to request user confirmation about registrations,
	 * for example
	 * 
	 * @param userId The user ID to change the status
	 * @param active <code>true</code> or <code>false</code>
	 */
	void setActive(int userId, boolean active) ;
	
	/**
	 * Sets the ranking.
	 * 
	 * @param userId The user ID
	 * @param rankingId int
	 */
	void setRanking(int userId, int rankingId) ;
	
	/**
	 * Increments the number of posts of the user.
	 * 
	 * @param userId The user ID to increment the number of posts
	 */
	void incrementPosts(int userId) ;
	
	/**
	 * Decrement the number of posts of some user.
	 * 
	 * @param userId The user ID do decrement the number of posts.
	 */
	void decrementPosts(int userId) ;
	
	/**
	 * Gets some piece of information of the last user registered
	 * 
	 * @return <code>HashMap</code> containing the information. The map
	 * has two keys:<br>
	 * <li><b>userName</b>: The username
	 * <li><b>userId</b>: The user's ID 
	 */
	User getLastUserInfo() ;
	
	/**
	 * Gets the total number of users
	 * 
	 * @return The total number of users
	 */
	int getTotalUsers() ;
	
	/**
	 * Gets the total number of users of some group.
	 * 
	 * @param groupId The group id
	 * @return The total number of users
	 */
	int getTotalUsersByGroup(int groupId) ;
	
	/**
	 * whether the user is locked or not.
	 *
     * @param  user_id  int
	 * @return boolean
	 */
	boolean isDeleted(int user_id) ;
	
	/***
	 * Checks the existence of some username.
	 * This method is used to ensure that will not be two equal usernames in the database.
	 * 
	 * @param username The username to verify
	 * @return <code>true</code> or <code>false</code>, if the user was found or not, respectively
	 */
	boolean isUsernameRegistered(String username) ;
	
	/**
	 * Validates the user login.
	 * 
	 * @param username The username
	 * @param password The password
	 * @return The user object if the provided information was corret, <code>null</code> if the information was invalid 
	 */
	User validateLogin(String username, String password) ;
	
	/**
	 * Associate the user to the group
	 * 
	 * @param userId The user id 
	 * @param groupId The group id to associate to
	 */
	void addToGroup(int userId, int[] groupId) ;
	
	/**
	 * Remove the user from the group
	 * 
	 * @param userId The user id
	 * @param groupId The group id to remove the user from
	 */
	void removeFromGroup(int userId, int[] groupId) ;
	
	/**
	 * Stores the "lost password" security hash, that was generated
	 * when the user asked the system to get a reminder of his password. 
	 * This hash is used to ensure the information supplied.  
	 * 
	 * @param email The user email
	 * @param hash The hash to store.
	 */
	void writeLostPasswordHash(String email, String hash) ;
	
	/**
	 * Validate the provided security hash against the data stored in our system.
	 * 
	 * @param email The user email
	 * @param hash The supplied security hash
	 * @return <code>true</code> if the data matches ok, of <code>false</code> if it is invalid
	 */
	boolean validateLostPasswordHash(String email, String hash) ;
	
	/**
	 * Writes a new password for the user. 
	 * 
	 * @param password The new password
	 * @param email The user email
	 */
	void saveNewPassword(String password, String email) ;
	
	/**
	 * Gets the username related to the email
	 * 
	 * @param email The email to search for the username
	 * @return The username, if found, or an empty <code>String</code>
	 */
	String getUsernameByEmail(String email) ;

    /**
    * Finds a user by its email address.  If multiple found, it returns the first one.
    *
    * @param email
    *            the email address to search
    * @return the user instance if a match is found, or null otherwise
    */
    public User findByEmail(String email);

    /**
    * Returns the number of users who have the specified email address.<p>
    *
    * While for a new installation this should always return 0 or 1, it is
    * quite possible for the JavaRanch database to return a great many more,
    * since the old forum software did not have a unique constraint on email
    * addresses (which caused it's own problems - the new way is much nicer).
    * However since there can be duplicates (in particular, there are 44,994
    * "no_address_provided@" users), this will allow us to process
    * users in batches (and not bring the system down!).
    *
    * @param email the email address we will shortly be looking up.
    * @return the number of users who have that email address.
    */
    public int getTotalUsersWithEmail(String email);

    /**
    * Finds all users with matching e-mails. Users migrated from UBB may have
    * the same e-mail.
    *
    * @param email the email address we are looking up
    * @param start the offset to the 1st record we want to return from all matches
    * @param count the number of records we want to return
    * @return a List of User records matching that email address
    */
    public List<User> findAllUsersByEmail(String email, int start, int count);

    /**
     * Finds all users with matching ip address. SQL style wildcards allowed
     * (e.g. "192.168.1.%").
     *
     * @param ip the IP address we want to look up.
     * @param start the offset to the 1st record we want to return from all matches
     * @param count the number of records we want to return
     * @return a list of users who have posted via that address.
     */
    public List<User> findAllUsersByIp(String ip, int start, int count);

    /**
     * Counts the number of users who have used the specified IP address.
     *
     * @param ip the IP address we want to look up.
     * @return the number of users who have used that IP address
     */
    public int getTotalUsersByIp(String ip);

	/**
	 * Validate if the activated key matches the one in the database
	 * 
	 * @param userId Which user to validate the activation key?
	 * @param hash The activation key
	 * @return <code>true</code> if the data matches ok, of <code>false</code> if it is invalid
	 */
	boolean validateActivationKeyHash(int userId , String hash) ;

	/**
	 * Set user account to active
	 * 
	 * @param userId Which user account to set active?
	 */
	void writeUserActive(int userId) ;
	
	/**
	 * Updates only the username. 
	 * This method generally will be used in implementations
	 * of <code>net.jforum.drivers.external.LoginAuthenticator</code> to 
	 * update usernames which changed in the external source and therefore
	 * should be updated in jforum's users table. 
	 * 
	 * @param userId The user's id related to the username to update
	 * @param username The new username to write
	 */
	void updateUsername(int userId, String username) ;
	
	/**
	 * Check if the username passed as argument is different of
	 * the username existent in the database. 
	 * 
	 * @param userId The user's id to work with
	 * @param usernameToCheck The username to compare with the existing
	 * one in <i>jforum_users</i>
	 * @return <code>true</code> if the usernames are different.
	 */
	boolean hasUsernameChanged(int userId, String usernameToCheck) ;
	
	/**
	 * Saves the user-specific security hash to the database
	 * @param userId the user id to save
	 * @param hash the security hash
	 */
	void saveUserAuthHash(int userId, String hash) ;
	
	/**
	 * Retrieves the auth hash from the database
	 * @param userId int
	 * @return String
	 */
	String getUserAuthHash(int userId) ;
	
	/**
	 * Returns a list of users that haven't yet activated their accounts
	 * @return ArrayList of pending activation users
	 */
	List<User> pendingActivations() ;
}
