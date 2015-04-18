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
 * Created on 25/07/2007 19:36:18
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jforum.entities.Post;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.PostCommon;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class ContentSearchOperation extends SearchOperation
{
    private transient List<Post> results = new ArrayList<Post>();
	
	public SearchResult<Post> performSearch(final SearchArgs args, int userID)
	{
        final SearchResult<Post> searchResult =
				(args.getKeywords().length > 0 || args.getUserIds().length > 0)
				? SearchFacade.search(args, userID)
				: new SearchResult<Post>(new ArrayList<Post>(), 0);

		this.results = searchResult.getRecords();

		return searchResult;
	}
	
	public void prepareForDisplay()
	{
		for (final Iterator<Post> iter = this.results.iterator(); iter.hasNext(); ) {
			PostCommon.preparePostForDisplay(iter.next());
		}
	}

	public List<Post> getResults()
	{
		return this.results;
	}

	public int totalRecords()
	{
		return this.results.size();
	}

	public String viewTemplate()
	{
		return TemplateKeys.SEARCH_SEARCH;
	}
	
	public int extractForumId(final Object value)
	{
		return ((Post)value).getForumId();
	}
}
