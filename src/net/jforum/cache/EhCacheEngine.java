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
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.jforum.util.preferences.SystemGlobals;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
/**
 * The rest of the application seems to make some invalid assumptions about how
 * things are cached. Those assumptions might be benign, but it is hard to tell
 * without deep testing. Until this is finished the JBossCacheEngine should be 
 * configured in a local mode.
 *
 * Created on Oct 11, 2005 
 *
 * @author Jake Fear
 * @author Andowson Chang
 * @version $Id$
 */
public class EhCacheEngine implements CacheEngine {

	private static final Logger LOGGER = Logger.getLogger(EhCacheEngine.class);
	
	private transient CacheManager manager;
	
	public void init() {
		try {
			manager = CacheManager.create(SystemGlobals.getValue("ehcache.cache.properties"));
		} catch (CacheException ce) {
			LOGGER.error("EhCache could not be initialized", ce);
			throw ce;
		}
	}

	public void stop() {
		manager.shutdown();
	}

	public void add(final String key, final Object value) {
		add(DUMMY_FQN, key, value);
	}

	public void add(final String fqn, final String key, final Object value) {
		try {
			if (!manager.cacheExists(fqn)) {
				LOGGER.debug("cache "+ fqn +" doesn't exist, add one");
				manager.addCache(fqn);
			}
			final Cache cache = manager.getCache(fqn);
			final Element element = new Element(key, value);
			if (cache != null) {
				cache.put(element);
			}
		} catch (IllegalStateException ie) {
           manager.addCache(fqn);
           final Cache cache = manager.getCache(fqn);
           final Element element = new Element(key, value);
           if (cache != null) {
				cache.put(element);
			}
        } catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
	}

	public Object get(final String fqn, final String key) {
		try {
			if (!manager.cacheExists(fqn)) {				
			    manager.addCache(fqn);
				LOGGER.debug("cache " + fqn + " doesn't exist and returns null");
				return null;
			}
			final Cache cache = manager.getCache(fqn);
			final Element element = cache.get(key);
			if (element != null) {
				return element.getValue();
			} 
			LOGGER.debug("cache " + fqn + " exists but " + key + " returns null");
			return null;
		} catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
	}

	public Object get(final String fqn) {	
		try {
			if (!manager.cacheExists(fqn)) {
				manager.addCache(fqn);
				LOGGER.debug("cache " + fqn + " doesn't exist and returns null");
				return null;
			}
			final Cache cache = manager.getCache(fqn);
			return cache.getAllWithLoader(cache.getKeys(), null);
		} catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
				
	}

	public Collection<Object> getValues(final String fqn) {
		try {
			if (!manager.cacheExists(fqn)) {
				//manager.addCache(fqn);
				LOGGER.debug("cache " + fqn + " doesn't exist and returns empty collection");
				return new ArrayList<Object>();
			}
			final Cache cache = manager.getCache(fqn);
			final List<Object> values = new ArrayList<Object>();
			final List<?> keys = cache.getKeys();
			
			for (final Iterator<?> iter = keys.iterator(); iter.hasNext(); ) {
				final Element element = cache.get(iter.next());
				if (element == null) {
					LOGGER.debug("element is null");
				} else {					
					values.add(element.getValue());
				}
			}
			
			LOGGER.debug("return:" + values);

			return values;
		} catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
	}

	public void remove(final String fqn, final String key) {
		try {
			final Cache cache = manager.getCache(fqn);

			if (cache != null) {
				cache.remove(key);
			}
		} catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
	}

	public void remove(final String fqn) {
		try {
			if (manager.cacheExists(fqn)) {
				//manager.removeCache(fqn);
                manager.getCache(fqn).flush();
			}
		} catch (Exception ce) {
			LOGGER.error(ce);
			throw new CacheException(ce);
		}
	}

}
