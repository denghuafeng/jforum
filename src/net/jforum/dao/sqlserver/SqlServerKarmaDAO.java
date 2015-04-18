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
 * Created on Feb 17, 2005
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.sqlserver;

import java.util.Date;
import java.util.List;

import net.jforum.dao.generic.GenericKarmaDAO;
import net.jforum.entities.User;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Franklin S. Dattein (<a href="mailto:franklin@portaljava.com">franklin@portaljava.com</a>)
 * @version $Id$
 */
public class SqlServerKarmaDAO extends GenericKarmaDAO
{
	/**
	 * @see net.jforum.dao.KarmaDAO#getMostRatedUserByPeriod(int, java.util.Date, java.util.Date, String) 
	 */
	public List<User> getMostRatedUserByPeriod(final int start, final Date firstPeriod, final Date lastPeriod, final String orderField)
    {
		final StringBuilder stringBuffer = new StringBuilder(SystemGlobals.getSql("GenericModel.selectByLimit")).append(" ").append(start).append(" ").
		append(SystemGlobals.getSql("KarmaModel.getMostRatedUserByPeriod")).
		append(" ORDER BY ").append(orderField).append(" DESC");
		final String sql = stringBuffer.toString();
		return super.getMostRatedUserByPeriod(sql, firstPeriod, lastPeriod);
	}
}
