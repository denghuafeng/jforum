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
 * Created on Mar 29, 2003 / 1:15:50 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.jforum.exceptions.ForumException;

/**
 * Hashes a string using MD5 or SHA-512
 * 
 * @author Rafael Steil
 */

public final class Hash 
{
	/**
	 * Hashes a string using MD5
	 * 
	 * @param str String to hash
	 * @return Hashed String
	 * @throws NoSuchAlgorithmException
	 */
	public static String md5(final String str)
	{
		return crypt(str, "MD5");
	}

	/**
	 * Hashes a string using SHA-512
	 * 
	 * @param str String to hash
	 * @return Hashed String
	 * @throws NoSuchAlgorithmException
	 */
	public static String sha512(final String str)
	{
		return crypt(str, "SHA-512");
	}

	private static String crypt(final String str, final String algo)
	{
		if (str == null || str.length() == 0) {
			throw new IllegalArgumentException("String to encrypt cannot be null or zero length");
		}

		final StringBuilder hexString = new StringBuilder();

		try {
			final MessageDigest msgDigest = MessageDigest.getInstance(algo);
			msgDigest.update(str.getBytes());
			final byte[] hash = msgDigest.digest();

			for (int i = 0; i < hash.length; i++) {
				if ((0xff & hash[i]) < 0x10) {
					hexString.append('0').append(Integer.toHexString((0xFF & hash[i])));
				}
				else {
					hexString.append(Integer.toHexString(0xFF & hash[i]));
				}
			}
		}
		catch (NoSuchAlgorithmException e) {
			throw new ForumException(e);
		}

		return hexString.toString();
	}

	private Hash() {}
}
