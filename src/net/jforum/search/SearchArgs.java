/*
 * Copyright (c) JForum Team
 * All rights reserved.

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
 * Created on 25/02/2004 - 19:16:25
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.User;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class SearchArgs 
{
	private String keywords;
	private int userId = -1;
	private String username;
	private String orderDir = "DESC";
	private String orderBy = "relevance";
	private int forumId;
	private int initialRecord;
	private String searchDate;
	private Date fromDate;
	private Date toDate;
	private MatchType matchType = MatchType.ALL_KEYWORDS;
	private String searchIn = "ALL";

	private boolean groupByForum = false;

	public static enum MatchType {
		ALL_KEYWORDS,
		ANY_KEYWORDS,
		EXACT_PHRASE,
		RAW_KEYWORDS
	}

	/**
	 * set the matching type - default to all words if unknown string is passed
	 * @param matchType
	 */
	public void setMatchType(String matchType)
	{
		if (matchType == null) {
			this.matchType = MatchType.ALL_KEYWORDS;
		} else if ("any".equals(matchType.toLowerCase())) {
			this.matchType = MatchType.ANY_KEYWORDS;
		} else if ("raw".equals(matchType.toLowerCase())) {
			this.matchType = MatchType.RAW_KEYWORDS;
		} else if ("phrase".equals(matchType.toLowerCase())) {
			this.matchType = MatchType.EXACT_PHRASE;
		} else {
			/* not set or null like for new messagesSearch */
			this.matchType = MatchType.ALL_KEYWORDS;
		}
	}

	public boolean isMatchAll() {
		return matchType.equals(MatchType.ALL_KEYWORDS);
	}

	public boolean isMatchAny() {
		return matchType.equals(MatchType.ANY_KEYWORDS);
	}

	public boolean isMatchExact() {
		return matchType.equals(MatchType.EXACT_PHRASE);
	}

	public boolean isMatchRaw() {
		return matchType.equals(MatchType.RAW_KEYWORDS);
	}

	public void setDateRange(Date fromDate, Date toDate)
	{
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public Date getFromDate()
	{
		return this.fromDate;
	}

	public Date getToDate()
	{
		return this.toDate;
	}

	public String getSearchDate() {
		return formatNullOrTrim(searchDate);
	}

	public void setSearchDate(String searchDate) {
		this.searchDate = searchDate;
	}

	public int fetchCount()
	{
		return SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);
	}

	public void startFetchingAtRecord(int initialRecord)
	{
		this.initialRecord = initialRecord;
	}

	public int startFrom()
	{
		return this.initialRecord;
	}

	public void setKeywords(String keywords)
	{
		this.keywords = keywords;
	}

	public void setUsername (String username)
	{
		this.username = username;
	}

	// -----------------------------------------------------------------

	/**
	 * Return member ids to query.<br />
	 * 1) If user searching by keyword and not member, return empty array.
	 * <br />
	 * 2) If user searching by user id, return array with single element <br />
	 * 3) If user searching by display name, return array with all non-negative
	 * elements. (Lucene doesn't allow searches for negative numbers without
	 * custom code. Since negative numbers are only test ids (and
	 * anonymous/admin), the search is not present for negative ids.
	 */
	public int[] getUserIds() {
		int[] userIds;
		if (userId > 0) {
			userIds = new int[] { userId };
		} else if (username != null && username.trim().length() > 0) {
			userIds = getUserIdsForName();
		} else {
			userIds = new int[0];
		}
		return userIds;
	}

	private int[] getUserIdsForName() {
		List<User> users = DataAccessDriver.getInstance().newUserDAO().findByName(getUsername(), false);
		users = removeNegativeIds(users);
		int length = users.size();
		int[] userIds = new int[length];
		for (int i = 0; i < length; i++) {
			userIds[i] = users.get(i).getId();
		}
		return userIds;
	}

	/*
	 * Lucene doesn't handle negative numbers. Since the only negative ids are
	 * test ids (or anonymous or admin), real users won't be searching for them.
	 */
	private List<User> removeNegativeIds(List<User> list) {
		List<User> result = new ArrayList<User>(list.size());
		for (User user : list) {
			if (user.getId() >= 0) {
				result.add(user);
			}
		}
		return result;
	}

	public void setUserId(String userId) {
		int id = -1;
		if (userId != null && userId.trim().length() > 0) {
			try {
				id = Integer.parseInt(userId.trim());
			} catch (NumberFormatException nfex) {
				// id is already -1, no need to do anything about this
			}
		}

		this.userId = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setForumId(int forumId)
	{
		this.forumId = forumId;
	}

	public void setUserID(int userID)
	{
		this.userId = userID;
	}

	public void setOrderBy(String orderBy)
	{
		this.orderBy = orderBy;
	}

	public void setOrderDir(String orderDir)
	{
		if (orderDir != null && (orderDir.equals("ASC") || orderDir.equals("DESC")))
			this.orderDir = orderDir;
	}

	public boolean isGroupByForum() {
		return groupByForum;
	}

	public void setGroupByForum(boolean groupByForum) {
		this.groupByForum = groupByForum;
	}

	public String[] getKeywords()
	{
		if (this.keywords == null || this.keywords.trim().length() == 0) {
			return new String[] {};
		}

		return this.keywords.trim().split(" ");
	}

	public String rawKeywords()
	{
		return formatNullOrTrim(this.keywords);
	}

	public String getUsername()
	{
		return this.username;
	}

	public int getForumId()
	{
		return this.forumId;
	}

	public boolean isOrderDirectionDescending() {
		return "DESC".equals(orderDir);
	}

	public String getOrderBy()
	{
		return this.orderBy;
	}

	// -----------------------------------------------------------------

	public String getSearchIn() {
		return formatNullOrTrim(searchIn);
	}

	public void setSearchIn(String searchIn) {
		this.searchIn = searchIn;
	}

	public boolean shouldLimitSearchToSubject() {
		return "SUBJECT".equals(searchIn);
	}

	// -----------------------------------------------------------------

	private String formatNullOrTrim(String value) {
		if (value == null) {
			return "";
		}
		return value.trim();
	}
}
