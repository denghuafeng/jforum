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
 * Created on 06/08/2007 15:20:23
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.LuceneDAO;
import net.jforum.entities.Post;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class LuceneReindexer
{
	private static final Logger LOGGER = Logger.getLogger(LuceneReindexer.class);
	
	private LuceneSettings settings;
	private LuceneReindexArgs args;
	private boolean recreate;
	
	public LuceneReindexer(LuceneSettings settings, LuceneReindexArgs args, boolean recreate)
	{
		this.settings = settings;
		this.args = args;
		this.recreate = recreate;		
	}
	
	public void startProcess()
	{
		this.reindex();
	}
	
	public void startBackgroundProcess()
	{
		Runnable indexingJob = new Runnable() {		
			public void run() {
				reindex();
			}
		};
		
		SystemGlobals.setValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING, "1");
		
		Thread thread = new Thread(indexingJob);
		thread.start();
	}

	private void reindex()
	{
		try {
			if (recreate) {
				this.settings.createIndexDirectory(SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH));
			}
		}
		catch (IOException e) {
			throw new ForumException(e);
		}
		
		LuceneDAO dao = DataAccessDriver.getInstance().newLuceneDAO();
		
		IndexSearcher searcher = null;
		LuceneSearch luceneSearch = ((LuceneManager)SearchFacade.manager()).luceneSearch();
		LuceneIndexer luceneIndexer = ((LuceneManager)SearchFacade.manager()).luceneIndexer();
		
		int fetchCount = SystemGlobals.getIntValue(ConfigKeys.LUCENE_INDEXER_DB_FETCH_COUNT);
		
		try {
			if (!recreate) {
				searcher = new IndexSearcher(IndexReader.open(this.settings.directory()));
			}			
			
			long processStart = System.currentTimeMillis();
			
			int firstPostId = args.filterByMessage() ? args.getFirstPostId() : dao.firstPostIdByDate(args.getFromDate());
			LOGGER.debug("firstPostId="+firstPostId);					
			int lastPostId = args.filterByMessage()	? args.getLastPostId() : dao.lastPostIdByDate(args.getToDate());
			LOGGER.debug("lastPostId="+lastPostId);	

			int dbFirstPostId = dao.firstPostIdByDate(new Date(0L));
			int dbLastPostId = dao.lastPostIdByDate(new Date());
			LOGGER.debug("dbFirstPostId="+dbFirstPostId);
			LOGGER.debug("dbLastPostId="+dbLastPostId);
			if (args.filterByMessage()) {
				if (firstPostId < dbFirstPostId) {
					firstPostId = dbFirstPostId;
				}
				if (lastPostId > dbLastPostId) {
					lastPostId = dbLastPostId;
				}
			}
			LOGGER.debug("firstPostId="+firstPostId);
			LOGGER.debug("lastPostId="+lastPostId);
			
			int counter = 0;
			int indexTotal = 0;
			long indexRangeStart = System.currentTimeMillis();
			boolean hasMorePosts = true;
			while (hasMorePosts) {
				boolean contextFinished = false;
				
				int toPostId = firstPostId + fetchCount < lastPostId
					? (firstPostId + fetchCount - 1)
					: lastPostId;
				LOGGER.debug("firstPostId="+firstPostId);
				LOGGER.debug("toPostId="+toPostId);	

				try {
					JForumExecutionContext ex = JForumExecutionContext.get();
					JForumExecutionContext.set(ex);
					
					List<Post> l = dao.getPostsToIndex(firstPostId, toPostId);

					if (counter >= 5000) {
						long end = System.currentTimeMillis();
						LOGGER.info("Indexed ~5000 documents in " 
							+ (end - indexRangeStart) + " ms (" + indexTotal + " so far)");
						indexRangeStart = end;
						counter = 0;
					}
					
					JForumExecutionContext.finish();
					contextFinished = true;
					
					for (Iterator<Post> iter = l.iterator(); iter.hasNext(); ) {
						if ("0".equals(SystemGlobals.getValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING))) {
							hasMorePosts = false;							
							break;
						}
						
						Post post = iter.next();
						
						if (!recreate && args.avoidDuplicatedRecords()) {
							if (luceneSearch.findDocumentByPostId(post.getId()) != null) {
								continue;
							}
						}

						luceneIndexer.batchCreate(post);
						
						counter++;
						indexTotal++;
					}

					firstPostId += fetchCount;
					hasMorePosts = hasMorePosts && toPostId < lastPostId;
				}
				finally {
					if (!contextFinished) {
						JForumExecutionContext.finish();
					}
				}
			}
			
			long end = System.currentTimeMillis();
			
			LOGGER.info("**** Total: " + (end - processStart) + " ms");
		}
		catch (IOException e) {
			throw new ForumException(e);
		}
		finally {
			SystemGlobals.setValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING, "0");

			luceneIndexer.flushRAMDirectory();
			
			if (searcher != null) {
				try { searcher.close(); }
				catch (Exception e) { LOGGER.error(e.getMessage(), e); }
			}
		}
	}
}
