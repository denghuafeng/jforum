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
 * Created on Feb 1, 2005 7:30:35 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class DefaultCacheEngine implements CacheEngine
{
	private transient Map<String, Object> cache;
	
	/**
	 * @see net.jforum.cache.CacheEngine#init()
	 */
	public void init()
	{
		this.cache = new HashMap<String, Object>();
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#stop()
	 */
	public void stop() 
	{
		this.cache.clear();
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#add(java.lang.String, java.lang.Object)
	 */
	public void add(final String key, final Object value)
	{
		this.cache.put(key, value);
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#add(java.lang.String, java.lang.String, java.lang.Object)
	 */
	public void add(final String fqn, final String key, final Object value)
	{
		Map<String, Object> map = (Map<String, Object>)this.cache.get(fqn);
		if (map == null) {
			map = new HashMap<String, Object>();
		}

		map.put(key, value);
		this.cache.put(fqn, map);
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#get(java.lang.String, java.lang.String)
	 */
	public Object get(final String fqn, final String key)
	{
		final Map<String, Object> map = (Map<String, Object>)this.cache.get(fqn);
		if (map == null) {
			return null;
		}
		
		return map.get(key);
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#get(java.lang.String)
	 */
	public Object get(final String fqn)
	{
		return this.cache.get(fqn);
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#getValues(java.lang.String)
	 */
	public Collection<Object> getValues(final String fqn)
	{
		final Map<String, Object> map = (Map<String, Object>)this.cache.get(fqn);
		if (map == null) {
			return new ArrayList<Object>();
		}
		
		return map.values();
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#remove(java.lang.String, java.lang.String)
	 */
	public void remove(final String fqn, final String key)
	{
		final Map<String, Object> map = (Map<String, Object>)this.cache.get(fqn);
		if (map != null) {
			map.remove(key);
		}
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#remove(java.lang.String)
	 */
	public void remove(final String fqn)
	{
		this.cache.remove(fqn);
	}
}
