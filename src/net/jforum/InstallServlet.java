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
 * Created on 27/08/2004 - 18:12:26
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.csrfguard.CsrfGuard;

import net.jforum.context.ForumContext;
import net.jforum.context.JForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.context.web.WebRequestContext;
import net.jforum.context.web.WebResponseContext;
import net.jforum.exceptions.ExceptionWriter;
import net.jforum.repository.ModulesRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class InstallServlet extends JForumBaseServlet
{
    private static final long serialVersionUID = 959359188496986295L;

    /** 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public void service(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException
    {
        try {
            final String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
            req.setCharacterEncoding(encoding);

            // Request
            final RequestContext request = new WebRequestContext(req);
            final ResponseContext response = new WebResponseContext(res);

            request.setCharacterEncoding(encoding);

            final JForumExecutionContext executionContext = JForumExecutionContext.get();

            final ForumContext forumContext = new JForumContext(
                                                                request.getContextPath(),
                                                                SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION),
                                                                request,
                                                                response,
                                                                false
                );

            executionContext.setForumContext(forumContext);

            // Assigns the information to user's thread 
            JForumExecutionContext.set(executionContext);

            if (SystemGlobals.getBoolValue(ConfigKeys.INSTALLED)) {
                JForumExecutionContext.setRedirect(request.getContextPath() 
                                                   + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
            }
            else {
                // Context
                final SimpleHash context = JForumExecutionContext.getTemplateContext();
                context.put("contextPath", req.getContextPath());
                context.put("serverName", req.getServerName());
                context.put("templateName", "default");
                context.put("serverPort", Integer.toString(req.getServerPort()));
                context.put("I18n", I18n.getInstance());
                context.put("encoding", encoding);
                context.put("extension", SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
                context.put("JForumContext", forumContext);
                context.put("version", SystemGlobals.getValue(ConfigKeys.VERSION));	
                CsrfGuard csrfGuard = CsrfGuard.getInstance();
                context.put("OWASP_CSRFTOKEN", csrfGuard.getTokenValue(req));

                // Module and Action
                final String moduleClass = ModulesRepository.getModuleClass(request.getModule());

                context.put("moduleName", request.getModule());
                context.put("action", request.getAction());

                final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), encoding));

                try {
                    if (moduleClass != null) {
                        // Here we go, baby
                        final Command command = (Command)Class.forName(moduleClass).newInstance();
                        final Template template = command.process(request, response, context);

                        if (JForumExecutionContext.getRedirectTo() == null) {
                            response.setContentType("text/html; charset=" + encoding);

                            template.process(context, out);
                            out.flush();
                        }
                    } else {
                        throw new Exception(request.getModule() + " module not found.\nAdd \"install=net.jforum.view.install.InstallAction\" to modulesMapping.properties if you want to reinstall JForum.");
                    }
                }
                catch (Exception e) {
                    response.setContentType("text/html; charset=" + encoding);
                    new ExceptionWriter().handleExceptionData(e, out, request);
                }
            }

            final String redirectTo = JForumExecutionContext.getRedirectTo();

            if (redirectTo != null) {
                response.sendRedirect(response.encodeRedirectURL(redirectTo));
            }
        }
        finally {
            JForumExecutionContext.finish();
        }
    }
}
