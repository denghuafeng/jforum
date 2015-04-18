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
 * Created on 18/07/2007 17:18:41
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Attachment;
import net.jforum.entities.AttachmentInfo;
import net.jforum.entities.Post;
import net.jforum.exceptions.SearchException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class LuceneIndexer
{
	private static final Logger LOGGER = Logger.getLogger(LuceneIndexer.class);

	private LuceneSettings settings;
	private Directory ramDirectory;
	private IndexWriter ramWriter;
	private int ramNumDocs;
	private List<NewDocumentAdded> newDocumentAddedList = new ArrayList<NewDocumentAdded>();

	private boolean indexAttachments = SystemGlobals.getBoolValue(ConfigKeys.LUCENE_INDEX_ATTACHMENTS);
	private AttachmentDAO attachDAO;
	private String attachDir = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR);

	public LuceneIndexer(final LuceneSettings settings)
	{
		this.settings = settings;
		this.createRAMWriter();
		this.attachDAO = DataAccessDriver.getInstance().newAttachmentDAO();
	}

	public void watchNewDocuDocumentAdded(NewDocumentAdded newDoc)
	{
		this.newDocumentAddedList.add(newDoc);
	}

	public void batchCreate(final Post post)
	{
		synchronized (LOGGER) {
			try {
				final Document document = this.createDocument(post);
				this.ramWriter.addDocument(document);
				this.flushRAMDirectoryIfNecessary();
			}
			catch (IOException e) {
				throw new SearchException(e);
			}
		}
	}

	private void createRAMWriter()
	{
		try {
			if (this.ramWriter != null) {
				this.ramWriter.close();
			}

			this.ramDirectory = new RAMDirectory();
			final IndexWriterConfig conf = new IndexWriterConfig(LuceneSettings.version, this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			this.ramWriter = new IndexWriter(this.ramDirectory, conf);
			this.ramNumDocs = SystemGlobals.getIntValue(ConfigKeys.LUCENE_INDEXER_RAM_NUMDOCS);
		}
		catch (IOException e) {
			throw new SearchException(e);
		}
	}

	private void flushRAMDirectoryIfNecessary()
	{
		if (this.ramWriter.maxDoc() >= this.ramNumDocs) {
			this.flushRAMDirectory();
		}
	}

	public void flushRAMDirectory()
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(LuceneSettings.version, this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);
				this.ramWriter.commit();
				this.ramWriter.close();
				writer.addIndexes(new Directory[] { this.ramDirectory });
				writer.forceMergeDeletes();

				this.createRAMWriter();
			}
			catch (IOException e) {
				throw new SearchException(e);
			}
			finally {
				if (writer != null) {
					try { 
						writer.commit(); 
						writer.close();

						this.notifyNewDocumentAdded();
					}
					catch (Exception e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}
		}
	}

	public void create(final Post post)
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(LuceneSettings.version, this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);

				final Document document = this.createDocument(post);
				writer.addDocument(document);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Indexed " + document);
				}
			}
			catch (Exception e) {
				LOGGER.error(e.toString(), e);
			}
			finally {
				if (writer != null) {
					try {
						writer.commit();
						writer.close();

						this.notifyNewDocumentAdded();
					}
					catch (Exception e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}
		}
	}

	public void update(final Post post)
	{
		if (this.performDelete(post)) {
			this.create(post);
		}
	}

	private Document createDocument(final Post post)
	{
		Document doc = new Document();

		doc.add(new Field(SearchFields.Keyword.POST_ID, String.valueOf(post.getId()), Store.YES, Index.NOT_ANALYZED));
		doc.add(new Field(SearchFields.Keyword.FORUM_ID, String.valueOf(post.getForumId()), Store.YES, Index.NOT_ANALYZED));
		doc.add(new Field(SearchFields.Keyword.TOPIC_ID, String.valueOf(post.getTopicId()), Store.YES, Index.NOT_ANALYZED));
		doc.add(new Field(SearchFields.Keyword.USER_ID, String.valueOf(post.getUserId()), Store.YES, Index.NOT_ANALYZED));
		doc.add(new Field(SearchFields.Keyword.DATE, this.settings.formatDateTime(post.getTime()), Store.YES, Index.NOT_ANALYZED));

		doc.add(new Field(SearchFields.Indexed.SUBJECT, post.getSubject(), Store.NO, Index.ANALYZED));
		doc.add(new Field(SearchFields.Indexed.CONTENTS, post.getText(), Store.NO, Index.ANALYZED));

		if (indexAttachments && post.hasAttachments()) {
			for (Attachment att : attachDAO.selectAttachments(post.getId())) {
				AttachmentInfo info = att.getInfo();
				doc.add(new Field(SearchFields.Indexed.CONTENTS, info.getComment(), Field.Store.NO, Field.Index.ANALYZED));

				File f = new File(attachDir + File.separatorChar + info.getPhysicalFilename());
				LOGGER.debug("indexing "+f.getName());
				InputStream is = null;
				try {
					Metadata metadata = new Metadata();
					metadata.set(Metadata.RESOURCE_NAME_KEY, f.getName());
					is = new FileInputStream(f);
					Parser parser = new AutoDetectParser();
					ContentHandler handler = new BodyContentHandler(-1);
					//-1 disables the character size limit; otherwise only the first 100.000 characters are indexed
					ParseContext context = new ParseContext();
					context.set(Parser.class, parser);

					Set<String> textualMetadataFields = new HashSet<String>();
					textualMetadataFields.add(TikaCoreProperties.TITLE.getName());
					textualMetadataFields.add(TikaCoreProperties.COMMENTS.getName());
					textualMetadataFields.add(TikaCoreProperties.KEYWORDS.getName());
					textualMetadataFields.add(TikaCoreProperties.DESCRIPTION.getName());
					textualMetadataFields.add(TikaCoreProperties.KEYWORDS.getName());

					parser.parse(is, handler, metadata, context);

					doc.add(new Field(SearchFields.Indexed.CONTENTS, handler.toString(), Field.Store.NO, Field.Index.ANALYZED));

					String[] names = metadata.names();
					for (int j=0; j<names.length; j++) {
						String value = metadata.get(names[j]);

						if (textualMetadataFields.contains(names[j])) {
							doc.add(new Field(SearchFields.Indexed.CONTENTS, value, Field.Store.NO, Field.Index.ANALYZED));
						}
					}
				} catch (Exception ex) {
					LOGGER.info("error indexing "+f.getName()+": " + ex.getMessage());
				} finally {
					try {
						is.close();
					} catch (Exception e) { 
						LOGGER.error("error  closing FileInputStream " +f.getName() + ": " + e.getMessage());
					}
				}
			}
		}

		return doc;
	}

	private void notifyNewDocumentAdded()
	{
		for (Iterator<NewDocumentAdded> iter = this.newDocumentAddedList.iterator(); iter.hasNext(); ) {
			iter.next().newDocumentAdded();
		}
	}

	public void delete(final Post post)
	{
		this.performDelete(post);
	}

	private boolean performDelete(final Post post)
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;
			boolean status = false;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(LuceneSettings.version, this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);
				writer.deleteDocuments(new Term(SearchFields.Keyword.POST_ID, String.valueOf(post.getId())));
				status = true;
			}
			catch (IOException e) {
				LOGGER.error(e.toString(), e);
			}
			finally {
				if (writer != null) {
					try {
						writer.commit();
						writer.close();
						this.flushRAMDirectory();
					}
					catch (IOException e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}

			return status;
		}
	}
}
