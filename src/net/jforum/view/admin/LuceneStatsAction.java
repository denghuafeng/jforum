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
 * Created on 23/07/2007 15:14:27
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.exceptions.ForumException;
import net.jforum.repository.ForumRepository;
import net.jforum.search.LuceneManager;
import net.jforum.search.LuceneReindexArgs;
import net.jforum.search.LuceneReindexer;
import net.jforum.search.LuceneSettings;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class LuceneStatsAction extends AdminCommand
{
	static {
		SystemGlobals.setValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING, "0");
	}

	/**
	 * @see net.jforum.Command#list()
	 */
	public void list()
	{
		IndexReader reader = null;

		try {
			File indexDir = new File(SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH));
			Directory fsDir = FSDirectory.open(indexDir);

			this.setTemplateName(TemplateKeys.SEARCH_STATS_LIST);
			boolean isInformationAvailable = true;

			try {
				reader = IndexReader.open(fsDir);
			} catch (IOException e) {
				isInformationAvailable = false;
			}

			this.context.put("isInformationAvailable", isInformationAvailable);
			this.context.put("indexExists", IndexReader.indexExists(fsDir));
			this.context.put("currentlyIndexing", "1".equals(SystemGlobals.getValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING)));

			if (isInformationAvailable) {
				this.context.put("isLocked", IndexWriter.isLocked(fsDir));
				this.context.put("isUpToDate", reader.isCurrent());
				this.context.put("indexLocation", indexDir.getAbsolutePath());
				this.context.put("totalMessages", Integer.valueOf(ForumRepository.getTotalMessages()));
				this.context.put("indexVersion", Long.valueOf(reader.getVersion()));
				this.context.put("numberOfDocs", Integer.valueOf(reader.numDocs()));
				this.context.put("numberDeletedDocs", Integer.valueOf(reader.numDeletedDocs()));
				this.context.put("refCount", Integer.valueOf(reader.getRefCount()));
				long uniqueTermCount = 0;
				for (IndexReader r : reader.getSequentialSubReaders()) {
					if (r != null)
						uniqueTermCount += r.getUniqueTermCount();
				}
				this.context.put("uniqueTermCount", uniqueTermCount);
			}
		}
		catch (IOException e) {
			throw new ForumException(e);
		}
		finally {
			if (reader != null) {
				try { reader.close(); }
				catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	public void createIndexDirectory() throws Exception
	{
		this.settings().createIndexDirectory(
			SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH));
		this.list();
	}
	
	public void reconstructIndexFromScratch()
	{
		LuceneReindexArgs args = this.buildReindexArgs();
		boolean recreate = "recreate".equals(this.request.getParameter("indexOperationType"));
		
		LuceneReindexer reindexer = new LuceneReindexer(this.settings(), args, recreate);
		reindexer.startBackgroundProcess();
		
		this.list();
	}
	
	public void cancelIndexing()
	{
		SystemGlobals.setValue(ConfigKeys.LUCENE_CURRENTLY_INDEXING, "0");
		this.list();
	}
	
	public void luceneNotEnabled()
	{
		this.setTemplateName(TemplateKeys.SEARCH_STATS_NOT_ENABLED);
	}
	
	public Template process(RequestContext request, ResponseContext response, SimpleHash context)
	{
		if (!this.isSearchEngineLucene()) {
			this.enableIgnoreAction();
			this.luceneNotEnabled();
		}
		
		return super.process(request, response, context);
	}
	
	private boolean isSearchEngineLucene()
	{
		return LuceneManager.class.getName()
			.equals(SystemGlobals.getValue(ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION))
			|| this.settings() == null;
	}
	
	private LuceneSettings settings()
	{
		return (LuceneSettings)SystemGlobals.getObjectValue(ConfigKeys.LUCENE_SETTINGS);
	}
	
	private LuceneReindexArgs buildReindexArgs()
	{
		Date fromDate = this.buildDateFromRequest("from");
		Date toDate = this.buildDateFromRequest("to");
		
		int firstPostId = 0;
		int lastPostId = 0;
		
		if (StringUtils.isNotEmpty(this.request.getParameter("firstPostId"))) {
			firstPostId = this.request.getIntParameter("firstPostId");
		}
		
		if (StringUtils.isNotEmpty(this.request.getParameter("lastPostId"))) {
			lastPostId = this.request.getIntParameter("lastPostId");
		}
		
		return new LuceneReindexArgs(fromDate, toDate, firstPostId, 
			lastPostId, "yes".equals(this.request.getParameter("avoidDuplicatedRecords")), 
			this.request.getIntParameter("type"));
	}
	
	private Date buildDateFromRequest(String prefix)
	{
		String day = this.request.getParameter(prefix + "Day");
		String month = this.request.getParameter(prefix + "Month");
		String year = this.request.getParameter(prefix + "Year");
		
		String hour = this.request.getParameter(prefix + "Hour");
		String minutes = this.request.getParameter(prefix + "Minutes");
		
		Date date = null;
		
		if (StringUtils.isNotEmpty(day) 
			&& StringUtils.isNotEmpty(month) 
			&& StringUtils.isNotEmpty(year) 
			&& StringUtils.isNotEmpty(hour) 
			&& StringUtils.isNotEmpty(minutes))
		{
			date = new GregorianCalendar(Integer.parseInt(year), 
				Integer.parseInt(month) - 1, 
				Integer.parseInt(year), 
				Integer.parseInt(hour), 
				Integer.parseInt(minutes), 0).getTime();
		}
		
		return date;
	}
}
