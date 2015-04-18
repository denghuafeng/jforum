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
 * Created on Jan 13, 2005 11:42:54 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.jforum.exceptions.CacheException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class JBossCacheEngine implements CacheEngine
{
	private static final Logger LOGGER = Logger.getLogger(JBossCacheEngine.class);
	private transient Cache<String, Object> cache;

	/**
	 * @see net.jforum.cache.CacheEngine#init()
	 */
	public void init()
	{
		try {
			final CacheFactory<String, Object> factory = new DefaultCacheFactory<String, Object>();			
			this.cache = factory.createCache(SystemGlobals.getValue(ConfigKeys.JBOSS_CACHE_PROPERTIES));
			
			this.cache.addCacheListener(new JBossCacheListener());
		}
		catch (Exception e) {
			throw new CacheException("Error while trying to configure jboss-cache: " + e);
		}
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#stop()
	 */
	public void stop()
	{		
		this.cache.stop();
		this.cache.destroy();
	}

	/**
	 * @see net.jforum.cache.CacheEngine#add(java.lang.String, java.lang.Object)
	 */
	public void add(final String key, final Object value)
	{
		this.add(CacheEngine.DUMMY_FQN, key, value);
	}

	/**
	 * @see net.jforum.cache.CacheEngine#add(java.lang.String, java.lang.String, java.lang.Object)
	 */
	public void add(final String fqn, final String key, final Object value)
	{
		try {
			if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
			    this.cache.put(Fqn.fromString(fqn), key, value);
			}
		}
		catch (Exception e) {
			throw new CacheException("Error while adding a new entry to the cache: " + e);			
		}
	}

	/**
	 * @see net.jforum.cache.CacheEngine#get(java.lang.String, java.lang.String)
	 */
	public Object get(final String fqn, final String key)
	{		
		Object value = null;
		
		try {
			if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
			    value = this.cache.get(Fqn.fromString(fqn), key);
			}
		}
		catch (Exception e) {
			throw new CacheException("Error while trying to get an entry from the cache: " + e);
		}
		
		return value;
	}

	/**
	 * @see net.jforum.cache.CacheEngine#get(java.lang.String)
	 */
	public Object get(final String fqn)
	{
		Object value = null;
		
		try {
			if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
			    value = this.cache.getData(Fqn.fromString(fqn));
			}
		}
		catch (Exception e) {
			throw new CacheException("Error while trying to get an entry from the cache: " + e);
		}
		
		return value;
	}
	
	/**
	 * @see net.jforum.cache.CacheEngine#getValues(java.lang.String)
	 */
	public Collection<Object> getValues(final String fqn)
	{		
		Map<String, Object> map = null;
		if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
			map = this.cache.getData(Fqn.fromString(fqn));
		}		 
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
		try {
			if (key == null) {
				remove(fqn);				
			}
			else {
				if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
				    this.cache.remove(Fqn.fromString(fqn), key);
				}
			}
		}
		catch (Exception e) {
			LOGGER.warn("Error while removing a FQN from the cache: " + e);
		}
	}

	/**
	 * @see net.jforum.cache.CacheEngine#remove(java.lang.String)
	 */
	public void remove(final String fqn)
	{
		try {
			if (this.cache.getCacheStatus() != CacheStatus.DESTROYED) {
			    this.cache.removeNode(Fqn.fromString(fqn));
			}
		}
		catch (Exception e) {
			LOGGER.warn("Error while removing a FQN from the cache: " + e);
		}
	}

}
