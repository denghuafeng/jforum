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
 * Created on Jan 26, 2005 4:42:44 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.oracle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.jforum.JForumExecutionContext;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;

/**
 * @author Dmitriy Kiriy
 * @version $Id$
 */
public final class OracleUtils
{
	private static final int BUFFER_SIZE = 4096;

	public static String readBlobUTF16BinaryStream(final ResultSet resultSet, final String fieldName) throws SQLException
	{
		try {
			final Blob clob = resultSet.getBlob(fieldName);

			final InputStream inputStream = clob.getBinaryStream();
			final StringBuilder stringBuffer = new StringBuilder();

			int readedBytes;

			do {
				final byte[] bytes = new byte[BUFFER_SIZE];

				readedBytes = inputStream.read(bytes);

				if (readedBytes > 0) {
					final String read = new String(bytes, 0, readedBytes, "UTF-16");
					stringBuffer.append(read);
				}
			} while (readedBytes == BUFFER_SIZE);

			inputStream.close();
			return stringBuffer.toString();
		}
		catch (IOException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * The query should look like:
	 *
	 * SELECT blob_field from any_table WHERE id = ? FOR UPDATE
	 *
	 * BUT KEEP IN MIND:
	 *
	 * When you insert record in previous step, it should go with empty_blob() like:
	 *
	 * INSERT INTO jforum_posts_text ( post_text ) VALUES (EMPTY_BLOB())
	 *
	 * @param query String
	 * @param idForQuery int
	 * @param value String
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void writeBlobUTF16BinaryStream(final String query, final int idForQuery, final String value) throws SQLException, IOException
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		OutputStream blobWriter = null;

		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(query);
			pstmt.setInt(1, idForQuery);

			resultSet = pstmt.executeQuery();
			resultSet.next();
			final Blob text = resultSet.getBlob(1);

			blobWriter = text.setBinaryStream(0L);

			blobWriter.write(value.getBytes("UTF-16"));

			blobWriter.close();
		}
		catch (IOException e) {
			throw new DatabaseException(e);
		}
		finally {
			if (blobWriter != null) {
				blobWriter.close();
			}

			DbUtils.close(resultSet, pstmt);
		}
	}

	private OracleUtils() {}
}