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
 * Created on Oct 16, 2011 / 12:04:35 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.security;

import java.io.IOException;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import org.apache.log4j.Logger;

/**
 * StopForumSpam 
 * @author Andowson Chang
 *
 */
public class StopForumSpam {
	private static final Logger LOGGER = Logger.getLogger(StopForumSpam.class);
	private static final String baseURL = SystemGlobals.getValue(ConfigKeys.STOPFORUMSPAM_API_URL);
	
	public static boolean checkIp(String ip) {		
		String url = baseURL + "ip=" + ip;		
		return getResult(url);
	}
	
	public static boolean checkEmail(String email) {
		String url = baseURL + "email=" + email;		
		return getResult(url);
	}
	
	private static boolean getResult(String url) {
		Element root = getXmlRootElement(url);
		String appears = (root != null) ? root.getChildTextTrim("appears") : null;
		return "yes".equals(appears);
	}
	
	public static Element getXmlRootElement(String url) {
		try {
			SAXBuilder xparser = new SAXBuilder();
			Document doc = xparser.build(url);
			Element root = doc.getRootElement();
			return root;
		} catch (JDOMException e) {
            // indicates a well-formedness error
			LOGGER.error("The result XML is not well-formed." + e.getMessage());
			LOGGER.error("url="+url);
		} catch (IOException ioe) {
			LOGGER.error("Oh no!...IOException" + ioe.getMessage());
		}
		return null;
	}
}
