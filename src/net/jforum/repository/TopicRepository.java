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
 * Created on 05/04/2004 - 20:11:44
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Topic;
import net.jforum.entities.TopicTypeComparator;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * Repository for the last n topics for each forum.
 * 
 * @author Rafael Steil
 * @author James Yong
 * @version $Id$
 */
public class TopicRepository implements Cacheable {
	private static final String FQN = "topics";
	private static final String RECENT = "recent";
	private static final String HOTTEST = "hottest";
	private static final String FQN_FORUM = FQN + "/byforum";
	private static final String RELATION = "relation";
	private static final String FQN_LOADED = FQN + "/loaded";
	private static final Comparator<Topic> TYPE_COMPARATOR = new TopicTypeComparator();

	private static CacheEngine cache;

	private static final Object MUTEX_RECENT = new Object();
	private static final Object MUTEX_HOTTEST = new Object();
	private static final Object MUTEX_FQN_FORUM = new Object();
	
	private static int maxRecentTopics = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);
	private static int maxHottestTopics = SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS);

	/**
	 * @see net.jforum.cache.Cacheable#setCacheEngine(net.jforum.cache.CacheEngine)
	 */
	public void setCacheEngine(CacheEngine engine) {
		TopicRepository.setEngine(engine);
	}

	private static void setEngine(CacheEngine engine) {
		cache = engine;
	}

	public static boolean isLoaded(int forumId) {
		return "1".equals(cache.get(FQN_LOADED, Integer.toString(forumId)));
	}

	/**
	 * Add topic to the FIFO stack
	 * 
	 * @param topic
	 *            The topic to add to stack
	 */
	public static void pushTopic(Topic topic) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			int limit = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);

			LinkedList<Topic> latestList = (LinkedList<Topic>) cache.get(FQN, RECENT);
			if (latestList == null || latestList.isEmpty()) {
				latestList = new LinkedList<Topic>(loadMostRecentTopics());
			}

			latestList.remove(topic);
			latestList.addFirst(topic);

			while (latestList.size() > limit) {
				latestList.removeLast();
			}
			synchronized (MUTEX_RECENT) {
				cache.add(FQN, RECENT, latestList);
			}

			limit = SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS);

			LinkedList<Topic> hottestList = (LinkedList<Topic>) cache.get(FQN, HOTTEST);
			if (hottestList == null || hottestList.isEmpty()) {
				hottestList = new LinkedList<Topic>(loadHottestTopics());
			}

			if (!hottestList.contains(topic)) {
				hottestList.addLast(topic);
			} else {
				int index = hottestList.indexOf(topic);
				hottestList.remove(index);
				while (index > 0) {
					Topic preTopic = (Topic) hottestList.get(index - 1);
					if (preTopic.getTotalViews() < topic.getTotalViews()) {
						index--;
					} else {
						break;
					}
				}
				hottestList.add(index, topic);
			}

			while (hottestList.size() > limit) {
				hottestList.removeLast();
			}
			synchronized (MUTEX_HOTTEST) {
				cache.add(FQN, HOTTEST, hottestList);
			}
		}
	}

	/**
	 * Get all cached recent topics.
	 * 
	 */
	public static List<Topic> getRecentTopics() {
		List<Topic> latestList = (List<Topic>) cache.get(FQN, RECENT);
		int limit = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);		

		if (limit != maxRecentTopics || latestList == null || latestList.isEmpty()
				|| !SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			latestList = loadMostRecentTopics();
			maxRecentTopics = limit;
		}

		return new ArrayList<Topic>(latestList);
	}

	/**
	 * Get all cached hottest topics.
	 * 
	 */
	public static List<Topic> getHottestTopics() {
		List<Topic> hottestList = (List<Topic>) cache.get(FQN, HOTTEST);
		int limit = SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS);

		if (limit != maxHottestTopics || hottestList == null || hottestList.isEmpty()
				|| !SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			hottestList = loadHottestTopics();
			maxHottestTopics = limit;
		}

		return new ArrayList<Topic>(hottestList);
	}

	/**
	 * Add recent topics to the cache
	 */
	public static List<Topic> loadMostRecentTopics() {
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		int limit = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);

		List<Topic> latestList = tm.selectRecentTopics(limit);
		synchronized (MUTEX_RECENT) {
			cache.add(FQN, RECENT, new LinkedList<Topic>(latestList));
		}

		return latestList;
	}

	/**
	 * Add hottest topics to the cache
	 */
	public static List<Topic> loadHottestTopics() {
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		int limit = SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS);

		List<Topic> hottestList = tm.selectHottestTopics(limit);
		synchronized (MUTEX_HOTTEST) {
			cache.add(FQN, HOTTEST, new LinkedList<Topic>(hottestList));
		}

		return hottestList;
	}

	/**
	 * Add topics to the cache
	 * 
	 * @param forumId
	 *            The forum id to which the topics are related
	 * @param topics
	 *            The topics to add
	 */
	public static void addAll(int forumId, List<Topic> topics) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (MUTEX_FQN_FORUM) {
				cache.add(FQN_FORUM, Integer.toString(forumId),
						new LinkedList<Topic>(topics));

				Map<Integer, Integer> m = (Map<Integer, Integer>) cache.get(FQN, RELATION);

				if (m == null) {
					m = new HashMap<Integer, Integer>();
				}

				Integer fId = Integer.valueOf(forumId);

				for (Iterator<Topic> iter = topics.iterator(); iter.hasNext();) {
					Topic topic = (Topic) iter.next();

					m.put(Integer.valueOf(topic.getId()), fId);
				}

				cache.add(FQN, RELATION, m);
				cache.add(FQN_LOADED, Integer.toString(forumId), "1");
			}
		}
	}

	/**
	 * Clears the cache
	 * 
	 * @param forumId
	 *            The forum id to clear the cache
	 */
	public static void clearCache(int forumId) {
		synchronized (MUTEX_FQN_FORUM) {
			cache.add(FQN_FORUM, Integer.toString(forumId),
					new LinkedList<Topic>());
			cache.remove(FQN, RELATION);
		}
	}

	/**
	 * Adds a new topic to the cache
	 * 
	 * @param topic
	 *            The topic to add
	 */
	public static void addTopic(Topic topic) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return;
		}

		synchronized (MUTEX_FQN_FORUM) {
			String forumId = Integer.toString(topic.getForumId());
			LinkedList<Topic> forumTopicsList = (LinkedList<Topic>) cache.get(FQN_FORUM, forumId);

			if (forumTopicsList == null) {
				forumTopicsList = new LinkedList<Topic>();
				forumTopicsList.add(topic);
			} else {
				boolean contains = forumTopicsList.contains(topic);

				// If the cache is full, remove the eldest element
				int topicCacheSize = SystemGlobals.getIntValue(ConfigKeys.TOPIC_CACHE_SIZE);
				if (!contains && forumTopicsList.size() + 1 > topicCacheSize) {
					forumTopicsList.removeLast();
				} else if (contains) {
					forumTopicsList.remove(topic);
				}

				forumTopicsList.add(topic);

				Collections.sort(forumTopicsList, TYPE_COMPARATOR);
			}

			cache.add(FQN_FORUM, forumId, forumTopicsList);

			Map<Integer, Integer> m = (Map<Integer, Integer>) cache.get(FQN, RELATION);

			if (m == null) {
				m = new HashMap<Integer, Integer>();
			}

			m.put(Integer.valueOf(topic.getId()), Integer.valueOf(forumId));

			cache.add(FQN, RELATION, m);
		}
	}

	/**
	 * Updates a cached topic
	 * 
	 * @param topic
	 *            The topic to update
	 */
	public static void updateTopic(Topic topic) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (MUTEX_FQN_FORUM) {
				String forumId = Integer.toString(topic.getForumId());
				List<Topic> forumTopicsList = (List<Topic>) cache.get(FQN_FORUM, forumId);

				if (forumTopicsList != null) {
					int index = forumTopicsList.indexOf(topic);

					if (index > -1) {
						forumTopicsList.set(index, topic);
						cache.add(FQN_FORUM, forumId, forumTopicsList);
					}
				}
			}

			synchronized (MUTEX_RECENT) {
				List<Topic> latestList = (List<Topic>) cache.get(FQN, RECENT);

				if (latestList != null) {
					int index = latestList.indexOf(topic);

					if (index > -1) {
						latestList.set(index, topic);
						cache.add(FQN, RECENT, latestList);
					}
				}
			}

			synchronized (MUTEX_HOTTEST) {
				List<Topic> hottestList = (List<Topic>) cache.get(FQN, HOTTEST);

				if (hottestList != null) {
					int index = hottestList.indexOf(topic);
					if (index > -1) {
						hottestList.remove(index);
					}
					while (index > 0) {
						Topic preTopic = (Topic) hottestList.get(index - 1);
						if (preTopic.getTotalViews() < topic.getTotalViews()) {
							index--;
						} else {
							break;
						}
					}
					if (index > -1) {
						hottestList.add(index, topic);
						cache.add(FQN, HOTTEST, hottestList);
					}
				}
			}
		}
	}

	/**
	 * Gets a cached topic.
	 * 
	 * @param topic
	 *            The topic to try to get from the cache. The instance passed as
	 *            argument should have at least the topicId and forumId set
	 * @return The topic instance, if found, or <code>null</code> otherwise.
	 */
	public static Topic getTopic(Topic topic) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return null;
		}

		if (topic.getForumId() == 0) {
			Map<Integer, Integer> m = (Map<Integer, Integer>) cache.get(FQN, RELATION);

			if (m != null) {
				Integer forumId = (Integer) m.get(Integer.valueOf(topic.getId()));

				if (forumId != null) {
					topic.setForumId(forumId.intValue());
				}
			}

			if (topic.getForumId() == 0) {
				return null;
			}
		}

		List<Topic> l = (List<Topic>) cache.get(FQN_FORUM, Integer.toString(topic.getForumId()));

		int index = -1;

		if (l != null) {
			index = l.indexOf(topic);
		}

		return (index == -1 ? null : (Topic) l.get(index));
	}

	/**
	 * Checks if a topic is cached
	 * 
	 * @param topic
	 *            The topic to verify
	 * @return <code>true</code> if the topic is cached, or <code>false</code>
	 *         if not.
	 */
	public static boolean isTopicCached(Topic topic) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return false;
		}

		String forumId = Integer.toString(topic.getForumId());
		List<Topic> list = (List<Topic>) cache.get(FQN_FORUM, forumId);

		return list == null ? false : list.contains(topic);
	}

	/**
	 * Get all cached topics related to a forum.
	 * 
	 * @param forumid
	 *            The forum id
	 * @return <code>ArrayList</code> with the topics.
	 */
	public static List<Topic> getTopics(int forumid) {
		List<Topic> returnList = null;
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (MUTEX_FQN_FORUM) {
				returnList = (List<Topic>) cache.get(FQN_FORUM,	Integer.toString(forumid));				
			}
		}

		if (returnList == null) {
			return new ArrayList<Topic>();
		} else {
			return new ArrayList<Topic>(returnList);
		}
	}
}
