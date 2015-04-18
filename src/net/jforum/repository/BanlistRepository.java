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
 * Created on 10/12/2006 19:12:49
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.BanlistDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Banlist;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class BanlistRepository implements Cacheable
{
	private static final Logger LOGGER = Logger.getLogger(BanlistRepository.class);
	private static CacheEngine cache;
	private static final String FQN = "banlist";
	private static final String BANLIST = "banlistCollection";
	private static boolean empty = false;
	
	/**
	 * @see net.jforum.cache.Cacheable#setCacheEngine(net.jforum.cache.CacheEngine)
	 */
	public void setCacheEngine(CacheEngine engine)
	{
		BanlistRepository.setEngine(engine);
	}
	
	private static void setEngine(CacheEngine engine) 
	{
		cache = engine;
	}
	
	public static boolean shouldBan(Banlist ban) {
		boolean status = false;
				
		Map<Integer, Banlist> map = banlist();
		if (map.isEmpty() && !empty) {
			loadBanlist();
			map = banlist();
		}
		for (Banlist current: map.values()) {			
			if (current.matches(ban)) {
				status = true;
				break;
			}
		}
		
		return status;
	}

	public static void add(Banlist ban)
	{
		Map<Integer, Banlist> map = banlist();
		map.put(Integer.valueOf(ban.getId()), ban);
		
		cache.add(FQN, BANLIST, map);
		if (empty) {
			empty = false;
		}
	}
	
	public static void remove(int banlistId)
	{
		Map<Integer, Banlist> map = banlist();
		
		Integer key = Integer.valueOf(banlistId);
		
		if (map.containsKey(key)) {
			map.remove(key);
		}
		
		cache.add(FQN, BANLIST, map);
		if (map.isEmpty()) {
			empty = true;
		}
	}
	
	private static Map<Integer, Banlist> banlist()
	{
		Map<Integer, Banlist> map = (Map<Integer, Banlist>)cache.get(FQN, BANLIST);
        
		if (map == null) {
			map = new HashMap<Integer, Banlist>();
		}
		
		return map;
	}
	
	public static void loadBanlist() 
	{
		BanlistDAO dao = DataAccessDriver.getInstance().newBanlistDAO();
		List<Banlist> list = dao.selectAll();
		
		if (list.size() == 0) {
			empty = true;
		} else {
			for (Banlist ban: list) {
				BanlistRepository.add(ban);			
			}
		}
		LOGGER.debug("Loading banlist from DAO");
	}
}
