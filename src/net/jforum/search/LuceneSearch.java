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
 * Created on 18/07/2007 22:05:37
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jforum.entities.Post;
import net.jforum.exceptions.SearchException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class LuceneSearch implements NewDocumentAdded
{
	private static final Logger LOGGER = Logger.getLogger(LuceneSearch.class);

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();

	private IndexSearcher searcher;
	private LuceneSettings settings;
	private LuceneContentCollector collector;

	public LuceneSearch(LuceneSettings settings, LuceneContentCollector collector)
	{
		this.settings = settings;
		this.collector = collector;

		this.openSearch();
	}

	public void newDocumentAdded() {		
		try {
			write.lock();
			if (searcher != null) {
                searcher.close();
            }
            // re-open a new searcher
			openSearch();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			write.unlock();
		}
	}

	/**
	 * @return the search result
	 */
	public SearchResult<Post> search(SearchArgs args, int userId)
	{
		return this.performSearch(args, this.collector, null, userId);
	}

	public Document findDocumentByPostId (int postId) {
		Document doc = null;
		
		try {
			read.lock();
			TopDocs results = searcher.search(new TermQuery(
					new Term(SearchFields.Keyword.POST_ID, String.valueOf(postId))), null, 1);
			ScoreDoc[] hits = results.scoreDocs;
			for (ScoreDoc hit : hits) {
				doc = this.searcher.doc(hit.doc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			read.unlock();
		}

		return doc;
	}

	private SearchResult<Post> performSearch(SearchArgs args, LuceneContentCollector resultCollector, Filter filter, int userId)
	{
		SearchResult<Post> result;

		try {
			read.lock();
			StringBuilder criteria = new StringBuilder(256);

			this.filterByForum(args, criteria);
			this.filterByUser(args, criteria, userId);
			this.filterByKeywords(args, criteria);
			this.filterByDateRange(args, criteria);

			LOGGER.info("criteria=["+criteria.toString()+"]");

			if (criteria.length() == 0) {
				result =  new SearchResult<Post>(new ArrayList<Post>(), 0);
			} else {
				Query query = new QueryParser(LuceneSettings.version, SearchFields.Indexed.CONTENTS, this.settings.analyzer()).parse(criteria.toString());
				
				final int limit = SystemGlobals.getIntValue(ConfigKeys.SEARCH_RESULT_LIMIT);
				TopFieldDocs tfd = searcher.search(query, filter, limit, getSorter(args));
				ScoreDoc[] docs = tfd.scoreDocs;
				int numDocs = tfd.totalHits;
				if (numDocs > 0) {
					result = new SearchResult<Post>(resultCollector.collect(args, docs, query), numDocs);
				} else {
					result = new SearchResult<Post>(new ArrayList<Post>(), 0);
				}

				LOGGER.info("hits="+numDocs);
			}
		} catch (Exception e) {

			throw new SearchException(e);
		} finally {
			read.unlock();
		}

		return result;
	}

	// only options are relevance and date
	private Sort getSorter (SearchArgs args) {
		Sort sort;

		SortField forumGroupingSortField = new SortField(SearchFields.Keyword.FORUM_ID, SortField.INT, false);
		SortField dateSortField = new SortField(SearchFields.Keyword.DATE, SortField.LONG, args.isOrderDirectionDescending());

		if ("time".equals(args.getOrderBy())) {
			// sort by date
			if (args.isGroupByForum()) {
				sort = new Sort(new SortField[] { forumGroupingSortField, dateSortField });
			} else {
				sort = new Sort(new SortField[] { dateSortField });
			}
		} else {
			// sort by relevance
			if (args.isGroupByForum()) {
				sort = new Sort(new SortField[] { forumGroupingSortField, SortField.FIELD_SCORE });
			} else {
				sort = new Sort(new SortField[] { SortField.FIELD_SCORE });
			}
		}

		return sort;
	}

	private void filterByDateRange(SearchArgs args, StringBuilder criteria)
	{
		if (args.getFromDate() != null) {
			if (criteria.length() > 0) {
				criteria.append(" AND ");
			}
			criteria.append('(')
				.append(SearchFields.Keyword.DATE)
				.append(": [")
				.append(this.settings.formatDateTime(args.getFromDate()))
				.append(" TO ")
				.append(this.settings.formatDateTime(args.getToDate()))
				.append("])");
		}
	}

	private void filterByUser (SearchArgs args, StringBuilder criteria, int userID) {
		int[] userIds = args.getUserIds();

		// if searching by user id (as opposed to solely by keyword)
		if (userIds.length > 0) {

			// By default, Lucene can't handle boolean queries with more than 1024 clauses.
			// Instead of raising the limit, we ask the user to give more information.
			if (userIds.length > 1000) {
				throw new RuntimeException("This first name/last name combination matches too many users. Please be more specific.");
			}

			/*
			 if (args.shouldLimitSearchToTopicStarted()) {			 
				// just looking for topics started by this user
				criteria.append("+(").append(SearchFields.Keyword.IS_FIRST_POST).append(":true) ");
			} else {
				// if searching for all posts by a member, we have
				// the option of filtering by those I started
				if (args.isTopicsIstarted()) {
					criteria.append("+(")
							.append(SearchFields.Keyword.TOPIC_STARTER_ID)
							.append(':')
							.append(userID<0 ? "\\" : "")
							.append(userID)
							.append(')');
				}
			}*/

			StringBuilder query = new StringBuilder();
			for (int i = 0; i < userIds.length; i++) {
				if (i > 0) {
					query.append(" OR ");
				}
				query.append(SearchFields.Keyword.USER_ID).append(':').append(userIds[i]);
			}
			criteria.append("+(").append(query.toString()).append(')');
		}
	}

	private void filterByKeywords(SearchArgs args, StringBuilder criteria)
	{
		LOGGER.info("searching for: " + args.rawKeywords());
		if (args.rawKeywords().length() > 0) {
			if (args.isMatchRaw()) {
				if (criteria.length() >0) {
					criteria.append(" AND ");
				}

				criteria.append('(');

				if (args.shouldLimitSearchToSubject()) {
					// subject only
					criteria.append(SearchFields.Indexed.SUBJECT).append(':').append(args.rawKeywords());
				} else {
					// contents and subject 
					criteria.append(SearchFields.Indexed.CONTENTS).append(':').append(args.rawKeywords());
					criteria.append(" OR ").append(SearchFields.Indexed.SUBJECT).append(':').append(args.rawKeywords());
				}

				criteria.append(')');
			} else if (args.isMatchExact()) {
				String escapedKeywords = "\"" + QueryParser.escape(args.rawKeywords()) + "\"";

				criteria.append("+(");

				if (args.shouldLimitSearchToSubject()) {
					// subject only
					criteria.append(SearchFields.Indexed.SUBJECT).append(':').append(escapedKeywords);
				} else {
					// contents and subject 
					criteria.append(SearchFields.Indexed.CONTENTS).append(':').append(escapedKeywords);
					criteria.append(" OR ").append(SearchFields.Indexed.SUBJECT).append(':').append(escapedKeywords);
				}

				criteria.append(')');
			} else {
				String[] keywords = this.analyzeKeywords(args.rawKeywords());

				if (keywords.length != 0) {
					if (criteria.length() > 0) {
						criteria.append(" AND ");
					}
					criteria.append("+(");
			// for Porter stemming it's problematic to analyze (and potentially alter) the keywords twice
			if (settings.analyzer() instanceof PorterStandardAnalyzer)
				keywords = args.rawKeywords().split("\\s");

					for (int i = 0; i < keywords.length; i++) {
						if (keywords[i].trim().length() == 0)
							continue;

						if (args.isMatchAll()) {
							criteria.append("+");
						}

						String escapedKeywords = QueryParser.escape(keywords[i]);

						criteria.append('(');
						if (args.shouldLimitSearchToSubject()) {
							// subject only
							criteria.append(SearchFields.Indexed.SUBJECT).append(':').append(escapedKeywords);
						} else {
							// contents and subject 
							criteria.append(SearchFields.Indexed.CONTENTS).append(':').append(escapedKeywords);
							criteria.append(" OR ").append(SearchFields.Indexed.SUBJECT).append(':').append(escapedKeywords);
						}
						criteria.append(')');
					}

					criteria.append(')');
				}
			}
		}
	}

	private void filterByForum(SearchArgs args, StringBuilder criteria)
	{
		if (args.getForumId() > 0) {
			criteria.append("+(")
				.append(SearchFields.Keyword.FORUM_ID)
				.append(':')
				.append(args.getForumId())
				.append(')');
		}
	}

	private String[] analyzeKeywords(String contents)
	{
		try {
			TokenStream stream = this.settings.analyzer().tokenStream(SearchFields.Indexed.CONTENTS, new StringReader(contents));
			stream.addAttribute(CharTermAttribute.class);
			List<String> tokens = new ArrayList<String>();

			stream.reset();
			while (stream.incrementToken()) {
				CharTermAttribute token = stream.getAttribute(CharTermAttribute.class);
				if (token == null) {
					break;
				}
				tokens.add(token.toString());
			}

			return tokens.toArray(new String[tokens.size()]);
		}
		catch (IOException e) {
			throw new SearchException(e);
		}
	}

	private void openSearch()
	{
		try {
			this.searcher = new IndexSearcher(IndexReader.open(this.settings.directory()));
		}
		catch (IOException e) {
			throw new SearchException(e.toString(), e);
		}
	}
}
