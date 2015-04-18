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
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the
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
package net.jforum.util.bbcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a UBB tag like [javadoc]javax.servlet.http.HttpServletRequest[/javadoc]
 * into a link to the corresponding javadoc page.
 * If no package name is given, then java.lang is assumed.
 *
 * Class names can be <i>versioned</i> if for some reason it makes sense to refer to the
 * javadocs of various API versions. This is done by appending a version ID to the class name:
 * [javadoc]String:5[/javadoc] refers to the javadocs of Java 5, while [javadoc]String:1.4[/javadoc]
 * refers to the javadocs of Java 1.4. Without the version ID, the latest available version is used.
 */

public class JavaDocLink implements Substitution {

	// indicates that there are several URLs for different versions of an API
	private static final String VERSIONED =		"versioned";
	private static final String OTHER =			"all";
	private static final String JSE_KEY =		"JSE";
	private static final String JEE_KEY =		"JEE";
	private static final String JASPER_KEY =	"JASPER";
	private static final String TOMCAT_KEY =	"TOMCAT";

	private static Map<String, String> versionedUrls;

	static {
		versionedUrls = new HashMap<String, String>();

		// JSE; "1.5" and "5" are synonyms
		versionedUrls.put(JSE_KEY+":1.3", "http://download.oracle.com/javase/1.3/docs/api/");
        versionedUrls.put(JSE_KEY+":1.4", "http://download.oracle.com/javase/1.4.2/docs/api/");
        versionedUrls.put(JSE_KEY+":1.5", "http://download.oracle.com/javase/1.5.0/docs/api/");
        versionedUrls.put(JSE_KEY+":5", "http://download.oracle.com/javase/1.5.0/docs/api/");
        versionedUrls.put(JSE_KEY+":6", "http://download.oracle.com/javase/6/docs/api/");
        versionedUrls.put(JSE_KEY+":7", "http://download.oracle.com/javase/7/docs/api/");
        versionedUrls.put(JSE_KEY+":8", "http://download.oracle.com/javase/8/docs/api/");
        versionedUrls.put(JSE_KEY+":"+OTHER, "http://download.oracle.com/javase/8/docs/api/");

		// JEE
		versionedUrls.put(JEE_KEY+":1.2", "http://download.oracle.com/javaee/1.2.1/api/");
        versionedUrls.put(JEE_KEY+":1.3", "http://download.oracle.com/javaee/1.3/api/");
        versionedUrls.put(JEE_KEY+":1.4", "http://download.oracle.com/javaee/1.4/api/");
        versionedUrls.put(JEE_KEY+":5", "http://download.oracle.com/javaee/5/api/");
        versionedUrls.put(JEE_KEY+":6", "http://download.oracle.com/javaee/6/api/");
        versionedUrls.put(JEE_KEY+":7", "http://download.oracle.com/javaee/7/api/");
        versionedUrls.put(JEE_KEY+":"+OTHER, "http://download.oracle.com/javaee/7/api/");

		// Tomcat
		versionedUrls.put(JASPER_KEY+":5.5", "http://tomcat.apache.org/tomcat-5.5-doc/jasper/docs/api/");
        versionedUrls.put(JASPER_KEY+":6", "http://tomcat.apache.org/tomcat-6.0-doc/api/");
        versionedUrls.put(JASPER_KEY+":7", "http://tomcat.apache.org/tomcat-7.0-doc/api/");
        versionedUrls.put(JASPER_KEY+":8", "http://tomcat.apache.org/tomcat-8.0-doc/api/");
        versionedUrls.put(JASPER_KEY+":"+OTHER, "http://tomcat.apache.org/tomcat-8.0-doc/api/");

		versionedUrls.put(TOMCAT_KEY+":5.5", "http://tomcat.apache.org/tomcat-5.5-doc/catalina/docs/api/");
        versionedUrls.put(TOMCAT_KEY+":6", "http://tomcat.apache.org/tomcat-6.0-doc/api/");
        versionedUrls.put(TOMCAT_KEY+":7", "http://tomcat.apache.org/tomcat-7.0-doc/api/");
        versionedUrls.put(TOMCAT_KEY+":8", "http://tomcat.apache.org/tomcat-8.0-doc/api/");
        versionedUrls.put(TOMCAT_KEY+":"+OTHER, "http://tomcat.apache.org/tomcat-8.0-doc/api/");
	}

    // Sun / java.net
    private static String J2SE_URL = VERSIONED+":"+JSE_KEY;
    private static String J2EE_URL = VERSIONED+":"+JEE_KEY;
    private static String JME_URL = "http://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/";
	// JOGL 1.1.1a
    // private static String JOGL_URL = "http://www.coderanch.com/how-to/javadoc/jogl-1.1.1a/";
	// JOGL 2.0
    private static String JOGL_URL = "http://download.java.net/media/jogl/jogl-2.x-docs/";
    private static String JAVA3D_URL = "http://download.java.net/media/java3d/javadoc/1.5.2/";
    private static String JMF_URL = "http://docs.oracle.com/cd/E17802_01/j2se/javase/technologies/desktop/media/jmf/2.1.1/apidocs/";
    private static String JAI_URL = "http://download.java.net/media/jai/javadoc/1.1.3/jai-apidocs/";
    private static String JAVAHELP_URL = "http://www.coderanch.com/how-to/javadoc/javahelp-2.0_05/";
    private static String JAVASPEECH_URL = "http://docs.oracle.com/cd/E17802_01/products/products/java-media/speech/forDevelopers/jsapi-doc/";
    private static String JAVAFX_URL = "http://docs.oracle.com/javafx/2/api/";
    private static String COMMONDOM_URL = "http://docs.oracle.com/javase/8/docs/jre/api/plugin/dom/";
    private static String JERSEY1_URL = "https://jersey.java.net/apidocs/1.18/jersey/";
    private static String JERSEY2_URL = "https://jersey.java.net/apidocs/latest/jersey/";
    private static String COM_SUN_MAIL_URL = "https://javamail.java.net/nonav/docs/api/";
    private static String LWUIT_URL = "http://lwuit.java.net/nonav/javadocs/";
    private static String JAVAXCOMM_URL = "http://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/";
    private static String JAVAXJNLP_URL = "http://docs.oracle.com/javase/8/docs/jre/api/javaws/jnlp/";

    // Apache
    private static String TOMCAT_URL = VERSIONED+":"+TOMCAT_KEY;
    private static String JASPER_URL = VERSIONED+":"+JASPER_KEY;
    private static String LOG4J_URL = "http://logging.apache.org/log4j/docs/api/";
    private static String LUCENE_URL = "http://lucene.apache.org/core/4_10_2/core/";
    private static String POI_URL = "http://poi.apache.org/apidocs/";
    private static String AXIS2_URL = "http://axis.apache.org/axis2/java/core/api/";
    private static String XML_CRYPTO_URL = "http://santuario.apache.org/Java/api/";
    private static String STRUTS1_URL = "http://struts.apache.org/release/1.3.x/apidocs/";
    private static String STRUTS2_URL = "http://struts.apache.org/release/2.3.x/struts2-core/apidocs/";
    private static String XWORK_URL = "http://struts.apache.org/release/2.3.x/xwork-core/apidocs/";
    private static String WICKET_URL = "http://ci.apache.org/projects/wicket/apidocs/6.x/";
    private static String XMLBEANS_URL = "http://xmlbeans.apache.org/docs/2.6.0/reference/";
    private static String TAPESTRY5_URL = "http://tapestry.apache.org/current/apidocs/";
    private static String WSS4J_URL = "http://ws.apache.org/wss4j/apidocs/";
    private static String SHIRO_URL = "http://shiro.apache.org/static/current/apidocs/";

    // Apache Commons
    private static String ACP = "http://commons.apache.org/proper/commons";
    private static String COLLECTIONS_URL = ACP + "-collections/javadocs/api-release/";
    private static String CLI_URL = ACP + "-cli/javadocs/api-release/";
    private static String VALIDATOR_URL = ACP + "-validator/apidocs/";
    private static String PRIMITIVES_URL = ACP + "-primitives/apidocs/";
    private static String MATH_URL = ACP + "-math/javadocs/api-3.3/";
    private static String JEXL_URL = ACP + "-jexl/apidocs/";
    private static String JXPATH_URL = ACP + "-jxpath/apidocs/";
    private static String IO_URL = ACP + "-io/javadocs/api-2.4/";
    private static String FILEUPLOAD_URL = ACP + "-fileupload/apidocs/";
    private static String DIGESTER_URL = ACP + "-digester/apidocs/";
    private static String DBCP_URL = ACP + "-dbcp/apidocs/";
    private static String CONFIGURATION_URL = ACP + "-configuration/apidocs/";
    private static String CODEC_URL = ACP + "-codec/javadocs/api-release/";
    private static String BEANUTILS_URL = ACP + "-beanutils/javadocs/v1.9.2/apidocs/";
	// TODO: should add version 4 of the HttpClients API, but it's split over various packages
	// with no easy pattern to separate: http://hc.apache.org/user-docs.html
    private static String HTTPCLIENT_URL = "http://hc.apache.org/httpclient-3.x/apidocs/";
    private static String NET_URL = ACP + "-net/javadocs/api-3.3/";
    private static String LANG_URL = ACP + "-lang/javadocs/api-release/";
    private static String LOGGING_URL = ACP + "-logging/javadocs/api-1.1/";
    private static String COMPRESS_URL = ACP + "-compress/javadocs/api-1.9/";
    private static String BETWIXT_URL = ACP + "-betwixt/apidocs/";
    private static String POOL_URL = ACP + "-pool/api-2.2/";

    // Other 3rd party
    private static String JUNIT_URL = "http://kentbeck.github.com/junit/javadoc/latest/";
    private static String JUNIT_OLD_URL = "http://www.junit.org/junit/javadoc/4.5/";
    private static String ITEXT2_URL = "http://www.coderanch.com/how-to/javadoc/itext-2.1.7/";
    private static String ITEXT_URL = "http://api.itextpdf.com/";
    private static String PDFBOX_URL = "http://pdfbox.apache.org/docs/1.8.6/javadocs/";
    private static String JFREECHART_URL = "http://www.jfree.org/jfreechart/api/gjdoc/";
    private static String IMAGEJ_URL = "http://rsb.info.nih.gov/ij/developer/api/";
    private static String JWEBUNIT_URL = "http://jwebunit.sourceforge.net/apidocs/";
    private static String XOM_URL = "http://www.xom.nu/apidocs/";
    private static String JCHART2D_URL = "http://jchart2d.sourceforge.net/docs/javadoc/";
    private static String JCIFS_URL = "http://jcifs.samba.org/src/docs/api/";
    private static String STRIPES_URL = "http://stripes.sourceforge.net/docs/current/javadoc/";
    private static String OPENCHART_URL = "http://www.coderanch.com/how-to/javadoc/openchart2-1.4.3/";
    private static String QUICKTIME_URL = "http://www.coderanch.com/how-to/javadoc/qtjavadocs/";
    private static String APPLEJAVA_URL = "http://www.coderanch.com/how-to/javadoc/appledoc/api/";
    private static String ANDROID_URL = "http://developer.android.com/reference/";
    private static String JEXCEL_URL = "http://jexcelapi.sourceforge.net/resources/javadocs/current/docs/";
    private static String MPXJ_URL = "http://mpxj.sourceforge.net/apidocs/";
    private static String HTTPUNIT_URL = "http://httpunit.sourceforge.net/doc/api/";
    private static String HTMLUNIT_URL = "http://htmlunit.sourceforge.net/apidocs/";
    private static String DOM4J_URL = "http://dom4j.sourceforge.net/dom4j-1.6.1/apidocs/";
    private static String JDOM2_URL = "http://www.jdom.org/docs/apidocs/";
    private static String JDOM1_URL = "http://www.jdom.org/docs/apidocs.1.1/";
    private static String SPRING_URL = "http://static.springsource.org/spring/docs/current/javadoc-api/";
    private static String SEAM_URL = "http://docs.jboss.org/seam/3/latest/api/";
    private static String HIBERNATE_URL = "http://docs.jboss.org/hibernate/stable/entitymanager/api/";
    private static String HIBERNATE_SEARCH_URL = "http://docs.jboss.org/hibernate/stable/search/api/";
    private static String HIBERNATE_VALIDATOR_URL = "http://docs.jboss.org/hibernate/stable/validator/api/";
    private static String HIBERNATE_SHARDS_URL = "http://docs.jboss.org/hibernate/stable/shards/api/";
    private static String QUARTZ_URL = "http://www.quartz-scheduler.org/api/2.2.1/";
    private static String OSGI_URL_CORE = "http://www.osgi.org/javadoc/r5/core/";
    private static String OSGI_URL_ENTERPRISE = "http://www.osgi.org/javadoc/r5/enterprise/";
    private static String GOOGLE_GUAVA_URL = "http://guava-libraries.googlecode.com/svn/trunk/javadoc/";
	private static String JAXEN_URL = "http://jaxen.codehaus.org/apidocs/";

    private static String[][] urlMap = new String[][] {
        {"javax.activation", J2EE_URL},
        {"javax.annotation.security", J2EE_URL}, // 6
        {"javax.annotation.sql", J2EE_URL}, // 6
        {"javax.batch", J2EE_URL}, // 7
        {"javax.context", J2EE_URL}, // 6
        {"javax.decorator", J2EE_URL}, // 6
        {"javax.ejb", J2EE_URL},
        {"javax.el", J2EE_URL},
        {"javax.enterprise", J2EE_URL},
        {"javax.event", J2EE_URL}, // 6
        {"javax.faces", J2EE_URL},
        {"javax.inject", J2EE_URL}, // 6
        {"javax.jms", J2EE_URL},
        {"javax.json", J2EE_URL}, // 7
        {"javax.mail", J2EE_URL},
        {"com.sun.mail", COM_SUN_MAIL_URL},
        {"javax.management.j2ee", J2EE_URL}, // 7
        {"javax.persistence", J2EE_URL},
        {"javax.resource", J2EE_URL},
        {"javax.security.auth.message", J2EE_URL}, // 6
        {"javax.security.jacc", J2EE_URL},
        {"javax.servlet", J2EE_URL},
        {"javax.transaction", J2EE_URL},
        {"javax.validation", J2EE_URL}, // 6
        {"javax.webbeans", J2EE_URL}, // 6
        {"javax.websocket", J2EE_URL}, // 7
        {"javax.ws.rs", J2EE_URL}, // 6
        {"javax.xml.registry", J2EE_URL},
        {"javax.xml.rpc", J2EE_URL},
        {"javax.comm", JAVAXCOMM_URL},
        {"javax.jnlp", JAVAXJNLP_URL},

        {"com.sun.java.browser.dom", COMMONDOM_URL},
        {"org.w3c.dom.css", COMMONDOM_URL},
        {"org.w3c.dom.html", COMMONDOM_URL},
        {"org.w3c.dom.ranges", COMMONDOM_URL},
        {"org.w3c.dom.ranges", COMMONDOM_URL},
        {"org.w3c.dom.stylesheets", COMMONDOM_URL},
        {"org.w3c.dom.traversal", COMMONDOM_URL},
        {"org.w3c.dom.views", COMMONDOM_URL},

        {"java.applet", J2SE_URL},
        {"java.awt", J2SE_URL},
        {"java.beans", J2SE_URL},
        {"java.io", J2SE_URL},
        {"java.lang", J2SE_URL},
        {"java.math", J2SE_URL},
        {"java.net", J2SE_URL},
        {"java.nio", J2SE_URL},
        {"java.rmi", J2SE_URL},
        {"java.security", J2SE_URL},
        {"java.sql", J2SE_URL},
        {"java.text", J2SE_URL},
        {"java.time", J2SE_URL}, // 8
        {"java.util", J2SE_URL},
        {"javax.accessibility", J2SE_URL},
        {"javax.activity", J2SE_URL}, // 1.5
        {"javax.annotation", J2SE_URL}, // 6
        {"javax.crypto", J2SE_URL},
        {"javax.imageio", J2SE_URL},
        {"javax.jws", J2SE_URL},
        {"javax.lang", J2SE_URL}, // 6
        {"javax.management", J2SE_URL}, // 7
        {"javax.naming", J2SE_URL},
        {"javax.net", J2SE_URL},
        {"javax.print", J2SE_URL},
        {"javax.rmi", J2SE_URL},
        {"javax.script", J2SE_URL}, // 6
        {"javax.security", J2SE_URL},
        {"javax.sound", J2SE_URL},
        {"javax.sql", J2SE_URL},
        {"javax.swing", J2SE_URL},
        {"javax.tools", J2SE_URL}, // 6
        {"javax.xml", J2SE_URL}, // after all the other javax.xml subpackages in JEE
        {"org.ietf.jgss", J2SE_URL},
        {"org.omg", J2SE_URL},
        {"org.w3c.dom", J2SE_URL}, // after all the other W3C DOM subpackages in Common DOM
        {"org.xml.sax", J2SE_URL},

        {"javax.microedition", JME_URL},
        {"com.sun.lwuit", LWUIT_URL},
        {"javax.help", JAVAHELP_URL},
        {"javax.speech", JAVASPEECH_URL},
        {"javafx", JAVAFX_URL},
        {"javax.media.jai", JAI_URL},
        {"com.sun.j3d", JAVA3D_URL},
        {"javax.media.j3d", JAVA3D_URL},
        {"javax.vecmath", JAVA3D_URL},
        {"com.sun.opengl", JOGL_URL},
        {"com.sun.javafx.newt", JOGL_URL},
        {"javax.media.nativewindow", JOGL_URL},
        {"javax.media.opengl", JOGL_URL},
        {"javax.media", JMF_URL}, // after all the other javax.media subpackages in JAI, Java3D and JOGL
        {"com.sun.jersey", JERSEY1_URL},
        {"com.sun.ws.rs.ext", JERSEY1_URL},
        {"org.glassfish.jersey", JERSEY2_URL},
        {"com.sun.research.ws.wadl", JERSEY2_URL},

        {"org.apache.lucene", LUCENE_URL},
        {"org.apache.poi", POI_URL},
        {"org.apache.log4j", LOG4J_URL},
        {"org.apache.axis2", AXIS2_URL},
        {"org.apache.strutsel", STRUTS1_URL},
        {"org.apache.struts", STRUTS1_URL},
        {"org.apache.struts2", STRUTS2_URL},
        {"com.opensymphony.xwork2", XWORK_URL},
        {"org.apache.wicket", WICKET_URL},
        {"org.apache.xmlbeans", XMLBEANS_URL},
        {"org.apache.shiro", SHIRO_URL},
        {"org.apache.tapestry5", TAPESTRY5_URL},
        {"org.apache.ws.axis.security", WSS4J_URL},
        {"org.apache.ws.security", WSS4J_URL},
        {"org.apache.xml.security", XML_CRYPTO_URL},

        {"org.apache.commons.collections", COLLECTIONS_URL},
        {"org.apache.commons.cli", CLI_URL},
        {"org.apache.commons.validator", VALIDATOR_URL},
        {"org.apache.commons.primitives", PRIMITIVES_URL},
        {"org.apache.commons.math", MATH_URL},
        {"org.apache.commons.jexl", JEXL_URL},
        {"org.apache.commons.jxpath", JXPATH_URL},
        {"org.apache.commons.io", IO_URL},
        {"org.apache.commons.fileupload", FILEUPLOAD_URL},
        {"org.apache.commons.digester", DIGESTER_URL},
        {"org.apache.commons.dbcp", DBCP_URL},
        {"org.apache.commons.configuration", CONFIGURATION_URL},
        {"org.apache.commons.codec", CODEC_URL},
        {"org.apache.commons.beanutils", BEANUTILS_URL},
        {"org.apache.commons.httpclient", HTTPCLIENT_URL},
        {"org.apache.commons.net", NET_URL},
        {"org.apache.commons.lang", LANG_URL},
        {"org.apache.commons.logging", LOGGING_URL},
        {"org.apache.commons.compress", COMPRESS_URL},
        {"org.apache.commons.betwixt", BETWIXT_URL},
        {"org.apache.commons.pool", POOL_URL},

        {"org.apache.catalina", TOMCAT_URL},
        {"org.apache.coyote", TOMCAT_URL},
        {"org.apache.el", TOMCAT_URL},
        {"org.apache.jasper", JASPER_URL},
        {"org.apache.jk", TOMCAT_URL},
        {"org.apache.juli", TOMCAT_URL},
        {"org.apache.naming", TOMCAT_URL},
        {"org.apache.tomcat", TOMCAT_URL},

        {"ij", IMAGEJ_URL},
        {"junit", JUNIT_OLD_URL},
        {"org.junit", JUNIT_URL},
        {"org.hamcrest", JUNIT_URL},
        {"com.lowagie", ITEXT2_URL},
        {"com.itextpdf", ITEXT_URL},
        {"org.apache.pdfbox", PDFBOX_URL},
        {"org.jfree.chart", JFREECHART_URL},
        {"org.jfree.data", JFREECHART_URL},
        {"net.sourceforge.jwebunit", JWEBUNIT_URL},
        {"nu.xom", XOM_URL},
        {"info.monitorenter", JCHART2D_URL},
        {"jcifs", JCIFS_URL},
        {"net.sourceforge.stripes", STRIPES_URL},
        {"com.approximatrix.charting", OPENCHART_URL},
        {"quicktime", QUICKTIME_URL},
        {"com.apple.eawt", APPLEJAVA_URL},
        {"com.apple.eio", APPLEJAVA_URL},
        {"android", ANDROID_URL},
        {"dalvik", ANDROID_URL},
        {"com.meterware", HTTPUNIT_URL},
        {"com.gargoylesoftware.htmlunit", HTMLUNIT_URL},
        {"org.jdom2", JDOM2_URL},
        {"org.jdom", JDOM1_URL},
        {"org.dom4j", DOM4J_URL},
        {"jxl", JEXCEL_URL},
        {"net.sf.mpxj", MPXJ_URL},
        {"org.springframework", SPRING_URL},
        {"org.jboss.seam", SEAM_URL},
        {"org.hibernate.search", HIBERNATE_SEARCH_URL},
        {"org.hibernate.validator", HIBERNATE_VALIDATOR_URL},
        {"org.hibernate.shards", HIBERNATE_SHARDS_URL},
        {"org.hibernate", HIBERNATE_URL}, // after the other org.hibernate subpackages
		{"org.quartz", QUARTZ_URL},
		{"org.osgi.framework", OSGI_URL_CORE},
		{"org.osgi.resource", OSGI_URL_CORE},
		{"org.osgi.service.condpermadmin", OSGI_URL_CORE},
		{"org.osgi.service.packageadmin", OSGI_URL_CORE},
		{"org.osgi.service.permissionadmin", OSGI_URL_CORE},
		{"org.osgi.service.startlevel", OSGI_URL_CORE},
		{"org.osgi.service.url", OSGI_URL_CORE},
		{"org.osgi.util.tracker", OSGI_URL_CORE},
		{"org.osgi", OSGI_URL_ENTERPRISE}, // after the other org.osgi packages that are part of the Core
		{"com.google.common", GOOGLE_GUAVA_URL },
		{"org.jaxen", JAXEN_URL }
    };

	private String lookup (String packageName, String apiVersion) {
        for (int i=0; i<urlMap.length; i++) {
            if (packageName.startsWith(urlMap[i][0])) {
				String url = urlMap[i][1];
				if (url.startsWith(VERSIONED)) {
					String versionKey = url.substring(url.indexOf(":")+1);
					String finalUrl = versionedUrls.get(versionKey+":"+apiVersion);
					if (finalUrl != null) {
						return finalUrl;
					}
					return versionedUrls.get(versionKey+":"+OTHER);
				}
				return url;
            }
        }

		return null;
	}

	// @Override
    public String substitute (String clazzName)
    {
		// remove any leading or trailing whitespace
		clazzName = clazzName.trim();

		int colonIndex = clazzName.indexOf(":");
		String apiVersion = null;
		if (colonIndex != -1) {
			apiVersion = clazzName.substring(colonIndex+1);
			clazzName = clazzName.substring(0, colonIndex);
		}

        int lastDotIndex = clazzName.lastIndexOf('.');
		int hashIndex = clazzName.indexOf('#');
		// Handle page-internal hashes like java.lang.Object#equals(java.lang.Object)
		// Assume java.lang package if no package name is given
		if (hashIndex == -1) {
			if (lastDotIndex == -1) {
				clazzName = "java.lang." + clazzName;
				lastDotIndex = clazzName.lastIndexOf('.');
			}
		} else {
			lastDotIndex = clazzName.lastIndexOf('.', hashIndex);
			if (lastDotIndex == -1) {
				clazzName = "java.lang." + clazzName;
				hashIndex = clazzName.indexOf('#');
				lastDotIndex = clazzName.lastIndexOf('.', hashIndex);
			}
		}

        String packageName = clazzName.substring(0, lastDotIndex).toLowerCase();

		String url = lookup(packageName, apiVersion);
		if (url != null) {
				// http://java.sun.com/javase/6/docs/api/java/util/Map.Entry.html
			if (hashIndex != -1) {
				String part1 = replaceDots(clazzName.substring(0, hashIndex));
				String part2 = clazzName.substring(hashIndex);
				// parentheses can be left out if there are no parameters
				if (part2.indexOf("(") < 0) {
					clazzName += "()";
					part2 += "()";
				}
				if (url.equals(versionedUrls.get(JSE_KEY+":8"))) {
					// Java SE 8 introduces a new URL style
					part2 = part2.replaceAll("[)(]", "-");
				}

				return "<a href=\"" + url + part1 + ".html" + part2 
					+ "\" target=\"_blank\" rel=\"nofollow\">" + clazzName + "</a>";
			} else {
				return "<a href=\"" + url + replaceDots(clazzName)
					+ ".html\" target=\"_blank\" rel=\"nofollow\">" + clazzName + "</a>";
			}
		}

		// if nothing is matched, then the original classname is returned
        return clazzName;
    }

	/** 
	 * Dots are replaced by backslashes, except if the next character is uppercase
	 * or inside of parentheses. The method relies on package names being lowercase.
	 * That allows linking to inner classes like java.util.Map.Entry.
	 * and to method hashes like java.util.Map.Entry#equals(java.lang.Object)
	 */
	private String replaceDots (String clazzName) {
		StringBuilder sb = new StringBuilder(clazzName);
		boolean classNameHasStarted = false;
		for (int i=0; i<sb.length(); i++) {
			if (sb.charAt(i) == '.') {
				if (!classNameHasStarted)
					sb.setCharAt(i, '/');

				if (Character.isUpperCase(sb.charAt(i+1)))
					classNameHasStarted = true;
			}
		}
		return sb.toString();
	}
}

