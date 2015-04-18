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
 * Created on 24/01/2004 17:39:21
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.repository;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.util.bbcode.BBCode;
import net.jforum.util.bbcode.BBCodeHandler;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class BBCodeRepository implements Cacheable
{
	private static CacheEngine cache;
	private static final String FQN = "bbcode";
	private static final String BBCOLLECTION = "bbCollection";
	
	/**
	 * @see net.jforum.cache.Cacheable#setCacheEngine(net.jforum.cache.CacheEngine)
	 */
	public void setCacheEngine(final CacheEngine engine)
	{
		BBCodeRepository.setEngine(engine);
	}	
	
	private static void setEngine(final CacheEngine engine) 
	{
		cache = engine;
	}
	
	public static void setBBCollection(final BBCodeHandler bbCollection)
	{
		cache.add(FQN, BBCOLLECTION, bbCollection);
	}
	
	public static BBCodeHandler getBBCollection()
	{
        BBCodeHandler handler = null;
        if (cache.get(FQN, BBCOLLECTION) != null) {
           handler = (BBCodeHandler)cache.get(FQN, BBCOLLECTION);
        } else {
            // cache flushed this, reload
            handler = new BBCodeHandler().parse();
            BBCodeRepository.setBBCollection(handler);
        }

		return handler;
	}
	
	public static BBCode findByName(final String tagName)
	{
		return getBBCollection().findByName(tagName);
	}
}
