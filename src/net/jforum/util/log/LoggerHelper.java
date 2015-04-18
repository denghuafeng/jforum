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
 * Created on 01.11.2013 by Heri
 * The JForum Project
 * http://www.jforum.net
 */

package net.jforum.util.log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;


/**
 * Utilities for helping with Log4j
 * <p>
 * @author Heri
 */

public final class LoggerHelper
{

    /**
     * Checks if the log4j framework is fully initialized, i.e. if at least one appender
     * can be found in the repository.
     * <p>
     * If it is not properly initialized means that Log4j's auto-configuration did not find
     * a suitable config file. In this case JForum copies its log4j_template.xml into 
     * the classpath as log4j.xml in order that Log4j finds it the next time. This config
     * file is then used for re-configuring log4j by invoking its DOMConfigurator.
     * <p>
     * Any throwable is caught and printed onto console.
     * <p>
     * 
     * @param templateDir
     *        folder where the log4j_template.xml can be found
     * @param classpathDir
     *        a folder on the classpath (i.e. WEB-INF/classes).
     */
    public static void checkLoggerInitialization( String templateDir, String classpathDir )
    {
        try
        {
            if ( loggerFrameworkFullyConfigured() ) {
                return;
            }

            new LoggerHelper().provideJForumLogConfig( templateDir, classpathDir );
        }
        catch ( Throwable e )
        {
            System.err.println( "JForum: problems initializing the logger: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * Tries to copy the log4j_template.xml file into the classpath, renames it to log4j.xml
     * and performs a reconfiguration of Log4j by invoking the DOMConfigurator.
     * <p>
     * If one of the assumptions is not fulfilled the DOMConfigurator is invoked with the
     * template file unless the the template cannot be found. In latter case an Exception is thrown.
     * <p>
     * @param templateDir
     *        folder where the log4j_template.xml can be found
     * @param classpathDir
     *        a folder on the classpath (i.e. WEB-INF/classes).
     * @throws Exception
     *         if the template file cannot be found.
     */
    void provideJForumLogConfig( String templateDir, String classpathDir ) throws Exception
	{
        File template = checkTemplate( templateDir );
        File dest = null;
        try
        {
            //checkClasspathDir( classpathDir );
            dest = checkDestFile( classpathDir );
        }
        catch ( Throwable e )
        {
            System.err.println( "JForum: " + e.getMessage() );
            e.printStackTrace();
            System.out.println( "JForum: Going to initialize logger with " + template );
            dest = template;
        }
        if ( template != dest )
        {
            FileUtils.copyFile( template, dest, true );
        }
        DOMConfigurator.configure( dest.toURI().toURL() );
	}

    /**
     * Checks if the file log4j_template.xml can be found in given directory.
     * <p>
     * If not found also the file log4j.xml is evaluated.
     * <p>
     * @param aTemplateDir
     *        folder where the file is to be expected
     * @return an initialized File object.
     * @throws Exception
     *         if neither log4j.xml nor log4j_template.xml can be found
     */
    File checkTemplate( String aTemplateDir ) throws Exception
    {
        File template = new File( aTemplateDir, "log4j_template.xml" );
        if ( !template.exists() )
        {
            // maybe old installation which still has log4j.xml in WEB-INF:
            File firstTemplate = template;
            template = new File( aTemplateDir, "log4j.xml" );
            if ( !template.exists() )
            {
                throw new Exception( "template not found: \"" + firstTemplate + "\"" );
            }
        }
        return template;
    } 

    /**
     * Checks if the folder exists and is a classpath folder.
     * <p>
     * @param aClasspathDir
     * @throws Exception if either not exists, is not a folder or not a classpath folder.
     */
    void checkClasspathDir( String aClasspathDir ) throws Exception
    {
        checkFolderExists( aClasspathDir );

        Enumeration<URL> urls = ClassLoader.getSystemResources( "net" );
        File netRes = new File( aClasspathDir, "net" );
        while ( urls.hasMoreElements() )
        {
            URL url = urls.nextElement();
            if (url.toString().equals(netRes.toURI().toURL().toString()))
            {
                return;
            }
        }

        throw new Exception( "given folder is not classpath: \"" + aClasspathDir + "\"" );
    }

    /**
     * Checks if the given folder name exists and is a directory
     * <p>
     * @param aFolder
     * @throws Exception
     *         if either does not exist or is not a directory
     */
    void checkFolderExists( String aFolder ) throws Exception
    {
        File destDir = new File( aFolder );
        if ( !destDir.exists() )
        {
            throw new Exception( "folder does not exist: \"" + aFolder + "\"" );
        }
        if ( !destDir.isDirectory() )
        {
            throw new Exception( "given folder is not directory: \"" + aFolder + "\"" );
        }
    }

    /**
     * Builds a File object by using the given folder plus "log4j.xml".
     * <p>
     * First some checks are done:
     * <ul>
     *   <li>if given folder is a classpath at all</li>
     *   <li>if the file already exists there</li>
     * </ul> 
     * In the former case an Exception is thrown, in the latter case the file is 
     * renamed to log4j.xml.bak because we assume that it is a corrupt log4j 
     * config file (it will be replaced by our template, see caller).
     * <p>
     * 
     * @param aFolder
     * @return
     * @throws IOException
     * @throws Exception
     * @throws MalformedURLException
     */
    File checkDestFile( String aFolder ) throws Exception
    {
        File dest = new File( aFolder, "log4j.xml" );
        if ( dest.exists() )
        {
            /*
             * Note: if we reach here, it means that the found log4j.xml has no appenders configured.
             */
            throw new Exception( dest.getPath() + " already exists in classpath" );
        }

        return dest;
    }

    /**
     * Tests if the logger is configured. Returns <code>true</code>
     * if there is at least one appender found.
     * <p>
     * Since this method loads itself the LogManager it is ensured that it executes its built in 
     * auto-configuration (unless not yet done before).
     * <p>
     * @return <code>true</code> if at least one appender is configured.
     */
    static boolean loggerFrameworkFullyConfigured()
    {
        if ( LogManager.getRootLogger().getAllAppenders().hasMoreElements() )
        {
            return true;
        }

        List<Logger> loggers = getCurrentLoggers();
        for ( Logger logger : loggers )
        {
            if ( logger.getAllAppenders().hasMoreElements() )
            {
                // at least one appender found
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the current loggers.
     * <p>
     * @return
     */
    private static List<Logger> getCurrentLoggers()
    {
        List<Logger> result = new ArrayList<Logger>();
        @SuppressWarnings( "rawtypes")
        Enumeration loggers = LogManager.getCurrentLoggers();
        while ( loggers.hasMoreElements() )
        {
            Logger logger = (Logger) loggers.nextElement();
            result.add( logger );
        }

        return result;
    }
}
