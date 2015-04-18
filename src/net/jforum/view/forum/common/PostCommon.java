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
 * Created on 21/05/2004 - 15:33:36
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.dao.PostDAO;
import net.jforum.entities.Post;
import net.jforum.entities.Smilie;
import net.jforum.repository.BBCodeRepository;
import net.jforum.repository.PostRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.SmiliesRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.util.SafeHtml;
import net.jforum.util.bbcode.BBCode;
import net.jforum.util.bbcode.BBCodeHandler;
import net.jforum.util.bbcode.Substitution;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class PostCommon
{
    private static final Logger log = Logger.getLogger(PostCommon.class);

	public static Post preparePostForDisplay(Post post)
	{
		if (post.getText() == null) {
			return post;
		}

		String text = post.getText();

		if (!post.isHtmlEnabled()) {
			text = text.replaceAll("<", "&lt;");
			text = text.replaceAll(">", "&gt;");
		}

		// Do not remove the trailing blank space, as it would
		// cause some regular expressions to fail
		text = text.replaceAll("\n", "<br /> ");

		SafeHtml safeHtml = new SafeHtml();

		post.setText(safeHtml.makeSafe(text));
		post.setSubject(safeHtml.makeSafe(post.getSubject()));
		processText(post);
		post.setText(safeHtml.ensureAllAttributesAreSafe(post.getText()));
		return post;
	}

	private static void processText(Post post)
	{
		int codeIndex = post.getText().indexOf("[code");
		int codeEndIndex = codeIndex > -1 ? post.getText().indexOf("[/code]") : -1;

		boolean hasCodeBlock = false;

		if (codeIndex == -1 || codeEndIndex == -1 || codeEndIndex < codeIndex) {
			post.setText(prepareTextForDisplayExceptCodeTag(post.getText(),
				post.isBbCodeEnabled(), post.isSmiliesEnabled()));
		}
		else if (post.isBbCodeEnabled() || post.isSmiliesEnabled()) {
			if (post.isBbCodeEnabled()) {
				hasCodeBlock = true;
			}

			int nextStartPos = 0;
			StringBuilder result = new StringBuilder(post.getText().length());

			while (codeIndex > -1 && codeEndIndex > -1 && codeEndIndex > codeIndex) {
				codeEndIndex += "[/code]".length();

				String nonCodeResult = prepareTextForDisplayExceptCodeTag(post.getText().substring(nextStartPos, codeIndex), 
					post.isBbCodeEnabled(), post.isSmiliesEnabled());

				if (hasCodeBlock) {
					String codeResult = parseCode(post.getText().substring(codeIndex, codeEndIndex));
					result.append(nonCodeResult).append(codeResult);
				} else {
					result.append(nonCodeResult).append(post.getText().substring(codeIndex, codeEndIndex));
				}

				nextStartPos = codeEndIndex;
				codeIndex = post.getText().indexOf("[code", codeEndIndex);
				codeEndIndex = codeIndex > -1 ? post.getText().indexOf("[/code]", codeIndex) : -1;
			}

			if (nextStartPos > -1) {
				String nonCodeResult = prepareTextForDisplayExceptCodeTag(post.getText().substring(nextStartPos), 
					post.isBbCodeEnabled(), post.isSmiliesEnabled());

				result.append(nonCodeResult);
			}

			post.setText(result.toString());
		}

		JForumExecutionContext.getTemplateContext().put("hasCodeBlock", hasCodeBlock);
	}

	private static String parseCode(String origText)
	{
		StringBuilder processed = new StringBuilder(origText.length());
		Matcher contentMatcher = Pattern.compile("(\\[code.*?\\])(.*)(\\[/code\\])", Pattern.DOTALL).matcher(origText);
		if (contentMatcher.matches()) {
			StringBuilder contents = new StringBuilder(contentMatcher.group(2));
			ViewCommon.replaceAll(contents, "<br /> ", "\n");
			// XML-like tags
			ViewCommon.replaceAll(contents, "<", "&lt;");
			ViewCommon.replaceAll(contents, ">", "&gt;");
			// Note: there is no replacing for spaces and tabs as
			// we are relying on the JavaScript SyntaxHighlighter library
			// to do it for us
			processed.append(contentMatcher.group(1));
			processed.append(contents);
			processed.append(contentMatcher.group(3));
		} else {
			// probably want to do some logging here...
			return origText; 
		}
		// now apply the regular expressions from the xml-config
		String text = processed.toString();
		for (Iterator<BBCode> iter = BBCodeRepository.getBBCollection().getBbList().iterator(); iter.hasNext();) {
			BBCode bb = iter.next();

			if (bb.getTagName().startsWith("code")) {
				text = text.replaceAll(bb.getRegex(), bb.getReplace());
			}
		}

		// Escape & to &amp;
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("&amp;lt;", "&lt;");
		text = text.replaceAll("&amp;gt;", "&gt;");
		text = text.replaceAll("&amp;quot;", "&quot;");
		text = text.replaceAll("&amp;amp;", "&amp;");

		return text;
	}

    public static String prepareTextForDisplayExceptCodeTag (String text, boolean isBBCodeEnabled, boolean isSmiliesEnabled) {
        if (text == null) {
            return text;
        }
        if (isSmiliesEnabled) {
            text = processSmilies(new StringBuilder(text));
        }
        if (isBBCodeEnabled && text.indexOf('[') > -1 && text.indexOf(']') > -1) {
            for (BBCode bb : BBCodeRepository.getBBCollection().getBbList()) {
                if (!bb.getTagName().startsWith("code")) {
                    if (bb.isRegexpReplace()) {
                        // regular expression text replacement
                        text = text.replaceAll(bb.getRegex(), bb.getReplace());
                    } else {
                        // Java code-based text replacement
                        try {
                            Substitution subst = (Substitution) Class.forName(bb.getClassName()).newInstance();
                            Pattern pat = Pattern.compile(bb.getRegex());
                            Matcher match = pat.matcher(text);
                            // the counter is just in case something goes wrong and the code enters a loop
                            int count = 0;
                            while (match.find() && count < 100) {
                                text = text.substring(0, match.start(0)) + subst.substitute(match.group(1))
                                        + text.substring(match.end(0));
                                match.reset(text);
                                count++;
                            }
                        } catch (Exception ex) {
                            log.error("error handling '" + bb.getTagName() + "' BB code: " + ex.getMessage());
                        }
                    }
                }
            }
        }
        text = parseDefaultRequiredBBCode(text);
        return text;
    }

	public static String parseDefaultRequiredBBCode(String origText)
	{
		String text = origText;
		Collection<BBCode> list = BBCodeRepository.getBBCollection().getAlwaysProcessList();

		for (Iterator<BBCode> iter = list.iterator(); iter.hasNext(); ) {
			BBCode bb = iter.next();
			text = text.replaceAll(bb.getRegex(), bb.getReplace());
		}

		return text;
	}

	/**
	 * Replace the smilies code by the respective URL.
	 * @param origText The text to process
	 * @return the parsed text. Note that the StringBuilder you pass as parameter
	 * will already have the right contents, as the replaces are done on the instance
	 */
    private static String processSmilies(StringBuilder text) {
        List<Smilie> smilies = SmiliesRepository.getSmilies();
        BBCodeHandler bbch = BBCodeRepository.getBBCollection();
        for (Iterator<Smilie> iter = smilies.iterator(); iter.hasNext();) {
            Smilie s = (Smilie) iter.next();
            int pos = 0;
            // The counter is used as prevention, in case
            // the while loop turns into an always true expression, for any reason
            int counter = 0;
            while (pos > -1 && counter++ < 100) {
                pos = text.indexOf(s.getCode(), pos);
                if (pos < 0) {
                    break;
                }
                // check whether this smilie is inside a UBB tag that is locked for smilies;
                // if so, skip until the closing tag
                int idxAfter = text.indexOf("[/", pos);
                if (idxAfter > -1) {
                    int idxBefore = text.substring(0, idxAfter).lastIndexOf("[");
                    if (idxBefore > -1 && idxBefore < pos) {
                        // yes, the smilie is inside of a UBB tag block
                        int idx2 = text.indexOf("]", idxAfter);
                        if (idx2 > -1) {
                            String tag = text.substring(idxAfter + 2, idx2).trim();
                            boolean doContinue = false;
                            // figure out whether this tag is locked for smilies
                            for (BBCode bbc : bbch.getBbList()) {
                                if (tag.equals(bbc.getLockedForSmilies())) {
                                    // log.debug("skipping smilie '" +
                                    // s.getCode() + "' inside of tag:'" + tag + "'");
                                    pos = idx2;
                                    doContinue = true;
                                    break;
                                }
                            }
                            if (doContinue) {
                                continue;
                            }
                        }
                    }
                }
				text.replace(pos, pos + s.getCode().length(), s.getUrl());
            }
        }
        return text.toString();
    }

	public static Post fillPostFromRequest()
	{
		Post post = new Post();
		post.setTime(new Date());

		return fillPostFromRequest(post, false);
	}

	public static Post fillPostFromRequest(Post post, boolean isEdit) 
	{
		RequestContext request = JForumExecutionContext.getRequest();

		post.setSubject(request.getParameter("subject"));
		post.setBbCodeEnabled(request.getParameter("disable_bbcode") == null);
		post.setSmiliesEnabled(request.getParameter("disable_smilies") == null);
		post.setSignatureEnabled(request.getParameter("attach_sig") != null);

		if (!isEdit) {
			post.setUserIp(request.getRemoteAddr());
			post.setUserId(SessionFacade.getUserSession().getUserId());
		}

		boolean htmlEnabled = SecurityRepository.canAccess(SecurityConstants.PERM_HTML_DISABLED, 
			request.getParameter("forum_id"));
		post.setHtmlEnabled(htmlEnabled && request.getParameter("disable_html") == null);

		if (post.isHtmlEnabled()) {
			post.setText(new SafeHtml().makeSafe(request.getParameter("message")));
		}
		else {
			post.setText(request.getParameter("message"));
		}

		return post;
	}

	public static boolean canEditPost(Post post)
	{
		return SessionFacade.isLogged()
			&& (post.getUserId() == SessionFacade.getUserSession().getUserId() || SessionFacade.getUserSession().isModerator(post.getForumId()))
			&& SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_POST_EDIT);
	}

	public static List<Post> topicPosts(PostDAO dao, boolean canEdit, int userId, int topicId, int start, int count)
	{
		boolean needPrepare = true;
		List<Post> posts;

 		if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
 			posts = PostRepository.selectAllByTopicByLimit(topicId, start, count);
 			needPrepare = false;
 		}
 		else {
 			posts = dao.selectAllByTopicByLimit(topicId, start, count);
 		}
 
		List<Post> helperList = new ArrayList<Post>();

		boolean hasCodeBlock = false;
		for (Post post : posts) {
			post.setCanEdit(PostCommon.canEditPost(post));

			helperList.add(needPrepare ? PostCommon.preparePostForDisplay(post) : post);

			if (!hasCodeBlock && post.getText().indexOf("pre name=\"code\"") != -1) {
				hasCodeBlock = true;
			}
		}

		JForumExecutionContext.getTemplateContext().put("hasCodeBlock", hasCodeBlock);

		return helperList;
	}
}
