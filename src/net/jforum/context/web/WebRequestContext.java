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
 * Created on Mar 16, 2003 / 1:31:30 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.context.web;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import net.jforum.UrlPattern;
import net.jforum.UrlPatternCollection;
import net.jforum.context.RequestContext;
import net.jforum.context.SessionContext;
import net.jforum.exceptions.MultipartHandlingException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Rafael Steil
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public class WebRequestContext extends HttpServletRequestWrapper implements RequestContext
{
	private static final String MODULE = "module";
	private static final String ACTION = "action";
	
	private transient final Map<String, Object> query;
	
	/**
	 * Default constructor.
	 * 
	 * @param superRequest Original <code>HttpServletRequest</code> instance
	 * @throws IOException
	 */
	public WebRequestContext(final HttpServletRequest superRequest) throws IOException
	{
		super(superRequest);

		this.query = new HashMap<String, Object>();
		boolean isMultipart = false;
		
		final String requestType = superRequest.getMethod().toUpperCase();
		final String contextPath = superRequest.getContextPath();
		String requestUri = this.extractRequestUri(superRequest.getRequestURI(), contextPath);
		final String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
		final String servletExtension = SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
		
		final boolean isPost = "POST".equals(requestType);
		final boolean isGet = !isPost;		

		final boolean isQueryStringEmpty = (superRequest.getQueryString() == null 
			|| superRequest.getQueryString().length() == 0);
		
		if (isGet && isQueryStringEmpty && requestUri.endsWith(servletExtension)) {
			superRequest.setCharacterEncoding(encoding);
			this.parseFriendlyURL(requestUri, servletExtension);
		}
		else if (isPost) {
			isMultipart = ServletFileUpload.isMultipartContent(new ServletRequestContext(superRequest));
			
			if (isMultipart) {
			    this.handleMultipart(superRequest, encoding);
			}
		}
		
		if (!isMultipart) {
			final boolean isAjax = "XMLHttpRequest".equals(superRequest.getHeader("X-Requested-With"));
			
			if (isAjax) {
				// Ajax requests are *usually* sent using application/x-www-form-urlencoded; charset=UTF-8.
				// In JForum, we assume this as always true.
				superRequest.setCharacterEncoding("UTF-8");
			}
			else {
				superRequest.setCharacterEncoding(encoding);
			}
			
			String containerEncoding = SystemGlobals.getValue(ConfigKeys.DEFAULT_CONTAINER_ENCODING);
			
			if (isPost) { 
				containerEncoding = encoding;
			}
			for (final Enumeration<String> enumeration = superRequest.getParameterNames(); enumeration.hasMoreElements(); ) {
				final String name = (String)enumeration.nextElement();
				
				final String[] values = superRequest.getParameterValues(name);
				
				if (values != null && values.length > 1) {
					for (int i = 0; i < values.length; i++) {
						this.addParameter(name, new String(values[i].getBytes(containerEncoding), encoding));
					}
				}
				else {					
					final String value = superRequest.getParameter(name);
					final String containerApp = SystemGlobals.getValue("container.app");
					final String containerVersion = SystemGlobals.getValue("container.version");					
					if (!"Apache Tomcat".equals(containerApp) || "4".equals(containerVersion) || "8".equals(containerVersion)) {
						this.addParameter(name, value);
			        } else {			            
			            this.addParameter(name, new String(value.getBytes(containerEncoding), encoding));
			        }
				}
			}
			
			if (this.getModule() == null && this.getAction() == null) {
				final int index = requestUri.indexOf('?');
				
				if (index > -1) {
					requestUri = requestUri.substring(0, index);
				}
				
				this.parseFriendlyURL(requestUri, servletExtension);
			}
		}
	}

	/**
	 * @param requestUri
	 * @param servletExtension
	 */
	private void parseFriendlyURL(final String requestUri, final String servletExtension) 
	{
		if (requestUri.length() < servletExtension.length()) {
			return;
		}
		final String uri = requestUri.substring(0, requestUri.length() - servletExtension.length());
		final String[] urlModel = uri.split("/");
		
		final int moduleIndex = 1;
		final int actionIndex = 2;
		int baseLen = 3;
		
		UrlPattern url = null;
		
		if (urlModel.length >= baseLen) {
			// <moduleName>.<actionName>.<numberOfParameters>
			StringBuilder stringBuffer = new StringBuilder(64)
				.append(urlModel[moduleIndex])
				.append('.')
				.append(urlModel[actionIndex])
				.append('.')
				.append(urlModel.length - baseLen);
			
			url = UrlPatternCollection.findPattern(stringBuffer.toString());
		}

		if (url == null) {
			this.addOrReplaceParameter(MODULE, null);
			this.addOrReplaceParameter(ACTION, null);				
		}
		else {			
			if (url.getSize() >= urlModel.length - baseLen) {
				for (int i = 0; i < url.getSize(); i++) {
					this.addParameter(url.getVars()[i], urlModel[i + baseLen]);
				}
			}
			
			this.addOrReplaceParameter(MODULE, urlModel[moduleIndex]);
			this.addOrReplaceParameter(ACTION, urlModel[actionIndex]);
		}
	}

    public SessionContext getSessionContext(final boolean create) {
        return new WebSessionContext(this.getSession(true));
    }

    public SessionContext getSessionContext() {
        return new WebSessionContext(this.getSession());
    }

    /**
	 * @param superRequest HttpServletRequest
	 * @param encoding String
	 * @throws UnsupportedEncodingException
	 */
	private void handleMultipart(final HttpServletRequest superRequest, final String encoding) throws UnsupportedEncodingException
	{
		String tmpPath = new StringBuilder(256)
		    .append(SystemGlobals.getApplicationPath())
		    .append('/')
		    .append(SystemGlobals.getValue(ConfigKeys.TMP_DIR))
		    .toString();
		
		File tmpDir = new File(tmpPath);
		boolean success = false;

		try {
			if (!tmpDir.exists()) {
				success = tmpDir.mkdirs();
			}
		}
		catch (Exception e) {
			// We won't log it because the directory
			// creation failed for some reason - a SecurityException
			// or something else. We don't care about it, as the
			// code below tries to use java.io.tmpdir
		}
		
		if (!success) {
			tmpPath = System.getProperty("java.io.tmpdir");
			tmpDir = new File(tmpPath);
		}
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(100 * 1024, tmpDir));
		upload.setHeaderEncoding(encoding);

		try {
			List<FileItem> items = upload.parseRequest(superRequest);
			
			for (Iterator<FileItem> iter = items.iterator(); iter.hasNext(); ) {
				FileItem item = (FileItem)iter.next();
			
				if (item.isFormField()) {
					this.addParameter(item.getFieldName(), item.getString(encoding));
				}
				else {
					if (item.getSize() > 0) {
						// We really don't want to call addParameter(), as 
						// there should not be possible to have multiple
						// values for a InputStream data
						this.query.put(item.getFieldName(), item);
					}
				}
			}
		}
		catch (FileUploadException e) {
			throw new MultipartHandlingException("Error while processing multipart content: " + e);
		}
	}
	
	/**
	 * @see javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
	 */
	public String[] getParameterValues(final String name) 
	{
		Object value = this.getObjectParameter(name);
		
		if (value instanceof String) {
			return new String[] { (String)value };
		}
		
		List<?> list = (List<?>)value;
		
		return list == null
			? super.getParameterValues(name)
			: list.toArray(new String[list.size()]);
	}
	
	private String extractRequestUri(final String requestUri, final String contextPath)
	{
		String uri = requestUri;
		
		// First, remove the context path from the requestUri, 
		// so we can work only with the important stuff
		if (contextPath != null && contextPath.length() > 0) {
			uri = requestUri.substring(contextPath.length(), requestUri.length());
		}
		
		// Remove the "jsessionid" (or similar) from the URI
		// Probably this is not the right way to go, since we're
		// discarding the value...
		int index = uri.indexOf(';');
		
		if (index > -1) {
			int lastIndex = uri.indexOf('?', index);
			
			if (lastIndex == -1) {
				lastIndex = uri.indexOf('&', index);
			}
			
			if (lastIndex == -1) {
				uri = uri.substring(0, index);
			}
			else {
				String part1 = uri.substring(0, index);
				uri = part1 + uri.substring(lastIndex);
			}
		}
		
		return uri;
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     * @param name String
	 * @return String
	 */
	public String getParameter(final String name) 
	{
		return (String)this.query.get(name);
	}

	/**
	 * Gets a parameter that is a number.
	 * A call to <code>Integer#parseInt(String)</code> is made
	 * to do the conversion
	 * @param name The parameter name to get the value
	 * @return int
	 */
	public int getIntParameter(final String name)
	{
		return Integer.parseInt(this.getParameter(name));
	}
	
	/**
	 * Gets some request parameter as <code>Object</code>.
	 * This method may be used when you have to get some value
	 * of a <i>multipart/form-data</i> request, like a image
	 * of file. <br>
	 * 
	 * @param name String
	 * @return Object
	 */
	public Object getObjectParameter(final String name)
	{
		return this.query.get(name);
	}
	
	public void addParameter(final String name, final Object value)
	{
		if (MODULE.equals(name) || ACTION.equals(name)) {
			addOrReplaceParameter(name, value);
			return;
		}
		if (this.query.containsKey(name)) {
			Object currentValue = this.getObjectParameter(name);
			List<Object> list;
			
			if (currentValue instanceof List<?>) {
				list = (List<Object>)currentValue;				
			}
			else {
				list = new ArrayList<Object>();
				list.add(currentValue);
			}
			
			list.add(value);
			this.query.put(name, list);	
		}
		else {			
			this.query.put(name, value);
		}
	}
	
	public void addOrReplaceParameter(final String name, final Object value)
	{
		this.query.put(name, value);
	}
	
	/**
	 * Gets the <i>action</i> of the current request.
	 * 
	 * An <i>Action</i> is the parameter name which specifies
	 * what next action should be done by the system. It may be
	 * add or edit a post, editing the groups, whatever. In the URL, the
	 * Action can the represented in two forms:
	 * <p>
	 * <blockquote>
	 * <code>
	 * http://www.host.com/webapp/servletName?module=groups&action=list
	 * </code>
	 * </blockquote>
	 * <p>
	 * or
	 * <p>
	 * <blockquote>
	 * <code>
	 * http://www.host.com/webapp/servletName/groups/list
	 * </code>
	 * </blockquote>
	 * <p>
	 * In both situations, the action's name is "list".
	 * 
	 * @return String representing the action name
	 */
	public String getAction()
	{
		return this.getParameter(ACTION);
	}
	
	public void changeAction(final String newAction)
	{	
		this.addOrReplaceParameter(ACTION, newAction);
	}
	
	/**
	 * Gets the <i>module</i> of the current request.
	 * 
	 * A <i>Module</i> is the parameter name which specifies
	 * what module the user is requesting. It may be the group
	 * administration, the topics or anything else configured module.
	 *In the URL, the Module can the represented in two forms:
	 * <p>
	 * <blockquote>
	 * <code>
	 * http://www.host.com/webapp/servletName?module=groups&action=list
	 * </code>
	 * </blockquote>
	 * <p>
	 * or
	 * <p>
	 * <blockquote>
	 * <code>
	 * http://www.host.com/webapp/servletName/groups/list
	 * </code>
	 * </blockquote>
	 * <p>
	 * In both situations, the module's name is "groups".
	 * 
	 * @return String representing the module name
	 */
	public String getModule()
	{
		return this.getParameter(MODULE);
	}
	
	public Object getObjectRequestParameter(final String parameter)
	{
		return this.query.get(parameter);
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequestWrapper#getContextPath()
	 */
	public String getContextPath() 
	{
		String contextPath = super.getContextPath();
		String proxiedContextPath = SystemGlobals.getValue(ConfigKeys.PROXIED_CONTEXT_PATH);
		
		if (StringUtils.isNotEmpty(proxiedContextPath)) {
			contextPath = proxiedContextPath;
		}
		
		return contextPath;
	}
	
	/**
	 * @see javax.servlet.ServletRequestWrapper#getRemoteAddr()
	 */
	public String getRemoteAddr()
	{
		// We look if the request is forwarded
		// If it is not call the older function.
        String ip = super.getHeader("x-forwarded-for");
        
        if (ip == null) {
        	ip = super.getRemoteAddr();
        }
        else {
        	// Process the IP to keep the last IP (real ip of the computer on the net)
            StringTokenizer tokenizer = new StringTokenizer(ip, ",");

            // Ignore all tokens, except the last one
            for (int i = 0; i < tokenizer.countTokens() -1 ; i++) {
            	tokenizer.nextElement();
            }
            
            ip = tokenizer.nextToken().trim();
            
            if ("".equals(ip)) {
            	ip = null;
            }
        }
        
        // If the ip is still null, we put 0.0.0.0 to avoid null values
        if (ip == null) {
        	ip = "0.0.0.0";
        }
        
        return ip;
	}
}
