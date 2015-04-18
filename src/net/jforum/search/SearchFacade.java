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
 * Created on Mar 11, 2005 12:01:47 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.util.ArrayList;

import net.jforum.entities.Post;
import net.jforum.exceptions.SearchInstantiationException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public final class SearchFacade
{
	private static final Logger LOGGER = Logger.getLogger(SearchFacade.class);
	private static LuceneManager searchManager;

	public static void init()
	{
		if (!isSearchEnabled()) {
			LOGGER.info("Search indexing is disabled. Will try to create a LuceneSearch "
				+ "instance for runtime configuration changes");
		}

		final String clazz = SystemGlobals.getValue(ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION);

		if (clazz == null || "".equals(clazz)) {
			LOGGER.info(ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION + " is not defined. Skipping.");
		}
		else {
			try {
				searchManager = (LuceneManager)Class.forName(clazz).newInstance();
			}
			catch (Exception e) {
				LOGGER.warn(e.toString(), e);
				throw new SearchInstantiationException("Error while tring to start the search manager: " + e);
			}

			searchManager.init();
		}
	}

	public static void create(final Post post)
	{
		if (isSearchEnabled()) {
			searchManager.create(post);
		}
	}

	public static void update(final Post post) 
	{
		if (isSearchEnabled()) {
			searchManager.update(post);
		}
	}

	public static SearchResult<Post> search(final SearchArgs args, int userID)
	{
		return isSearchEnabled()
			? searchManager.search(args, userID)
			: new SearchResult<Post>(new ArrayList<Post>(), 0);
	}

	private static boolean isSearchEnabled()
	{
		return SystemGlobals.getBoolValue(ConfigKeys.SEARCH_INDEXING_ENABLED);
	}

	public static void delete(final Post post)
	{
		if (isSearchEnabled()) {
			searchManager.delete(post);
		}
	}

	public static LuceneManager manager()
	{
		return searchManager;
	}

	private SearchFacade() {}
}
