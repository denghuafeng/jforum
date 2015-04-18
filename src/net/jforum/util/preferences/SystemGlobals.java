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
 * Created on Feb 24, 2003 / 8:25:35 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.util.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import net.jforum.exceptions.ForumException;
import net.jforum.util.SortedProperties;

import org.apache.log4j.Logger;

/**
 * <p>Store global configurations used in the system.
 * This is a helper class used to access the values
 * defined at SystemGlobals.properties and related config files.</p> 
 * 
 * <p>
 * Transient values are stored in a special place, and are not
 * modified when you change a regular key's value. 
 * </p>
 * 
 * @author Rafael Steil
 * @author Pieter Olivier
 */

public final class SystemGlobals implements VariableStore
{
    private static final Logger LOGGER = Logger.getLogger(SystemGlobals.class);

    private static SystemGlobals globals = new SystemGlobals();

    private String defaultConfig;
    private File installationConfig;

    private Properties defaults = new Properties();
    private Properties installation = new Properties();
    private Map<String, Object> objectProperties = new HashMap<String, Object>();
    private static List<File> additionalDefaultsList = new ArrayList<File>();
    private static Properties queries = new Properties();
    private static Properties transientValues = new Properties();

    private VariableExpander expander = new VariableExpander(this, "${", "}");

    private SystemGlobals() {}

    /**
     * Initialize the global configuration
     * @param appPath The application path (normally the path to the webapp base dir
     * @param mainConfigurationFile The file containing system defaults (when null, defaults to <appPath>/WEB-INF/config/SystemGlobals.properties)
     */
    public static void initGlobals(String appPath, String mainConfigurationFile)
    {
        globals.buildSystem(appPath, mainConfigurationFile);
    }

    public static void reset()
    {
        globals.defaults.clear();
        globals.installation.clear();
        additionalDefaultsList.clear();
        queries.clear();
        transientValues.clear();
    }

    private void buildSystem(String appPath, String mainConfigurationFile)
    {
        if (mainConfigurationFile == null) {
            throw new InvalidParameterException("defaultConfig could not be null");
        }

        this.defaultConfig = mainConfigurationFile;
        this.defaults.clear();

        this.defaults.put(ConfigKeys.APPLICATION_PATH, appPath);
        this.defaults.put(ConfigKeys.DEFAULT_CONFIG, mainConfigurationFile);		

        SystemGlobals.loadDefaults();
        debugValues( defaults, "defaults" );

        this.installation.clear();
        this.installationConfig = new File( getVariableValue(ConfigKeys.INSTALLATION_CONFIG) );
        if (this.installationConfig.exists() && !additionalDefaultsList.contains(this.installationConfig)) {
            additionalDefaultsList.add(0, this.installationConfig );
            LOGGER.info("Added " + this.installationConfig);
        }		

        for (File file : additionalDefaultsList) {
            loadAdditionalDefault(file);
        }
        globals.expander.clearCache();
        debugValues( globals.installation, "installation" );
    }

    /**
     * Sets a value for some property
     * 
     * @param field The property name
     * @param value The property value 
     * @see #getVariableValue(String)
     * */
    public static void setValue(String field, String value)
    {
        globals.installation.put(field, value);
        globals.expander.clearCache();
    }

    public static void setObjectValue(String field, Object value)
    {
        globals.objectProperties.put(field, value);
    }

    public static Object getObjectValue(String field)
    {
        return globals.objectProperties.get(field);
    }

    /**
     * Set a transient configuration value (a value that will not be saved) 
     * @param field The name of the configuration option
     * @param value The value of the configuration option
     */
    public static void setTransientValue(String field, String value)
    {
        LOGGER.debug( "Adding transient " + field + "=" + value );
        transientValues.put(field, value);
    }

    /**
     * Load system defaults
     */
    private static void loadDefaults()
    {
        LOGGER.info("Loading mainConfigurationFile " + globals.defaultConfig + " ...");
        loadProps( globals.defaults, new File( globals.defaultConfig ) );
        globals.expander.clearCache();
    }

    /**
     * Merge additional configuration defaults
     * 
     * @param file File from which to load the additional defaults
     */
    public static void loadAdditionalDefaults(String... file)
    {
        File[] files = new File[file.length];
        for ( int i = 0; i < file.length; i++ )
        {
            files[i] = new File(file[i]);
        }
        for ( int i = 0; i < files.length; i++ )
        {
            globals.loadAdditionalDefault( files[i] );
        }
        globals.expander.clearCache();
        debugValues( globals.installation, "installation" );
    }

    /**
     * Merge additional configuration into installations
     * <p>
     * If the file does not exist nothing is done.
     * <p>
     * The file is added to the internal list additionalDefaultsList if not yet present
     * <p>
     * 
     * @param file File from which to load the additional defaults. Must not be <code>null</code>
     */
    private void loadAdditionalDefault( File file )
    {
        if (!file.exists()) {
            LOGGER.info("Cannot find file " + file + ". Will ignore it");
            return;
        }

        LOGGER.info("Loading additional default into installation " + file + " ...");

        loadProps( installation, file );

        if (!additionalDefaultsList.contains(file)) {
            additionalDefaultsList.add(file);
            LOGGER.info("Added " + file);
        }
    }

    /**
     * Save installation defaults
     */
    public static void saveInstallation()
    {
        // We need this temporary "p" because, when
        // new FileOutputStream() is called, it will 
        // raise an event to the TimerTask who is listening
        // for file modifications, which then reloads the
        // configurations from the filesystem, overwriting
        // our new keys. 

        Properties p = new SortedProperties();
        p.putAll(globals.installation);

        try {
            FileOutputStream out = new FileOutputStream(globals.installationConfig);
            p.store(out, "Installation specific configuration options");
            out.close();
        }
        catch (IOException e) {
            throw new ForumException(e);
        }		
    }

    /**
     * Gets the value of some property
     * 
     * @param field The property name to retrieve the value
     * @return String with the value, or <code>null</code> if not found
     * @see #setValue(String, String)
     * */
    public static String getValue(String field)
    {
        return globals.getVariableValue(field);
    }

    public static String getTransientValue(String field)
    {
        return transientValues.getProperty(field);
    }

    /**
     * Retrieve an integer-valued configuration field
     * 
     * @param field Name of the configuration option
     * @return The value of the configuration option
     * @exception NullPointerException when the field does not exists
     */
    public static int getIntValue(String field)
    {
        return Integer.parseInt(getValue(field));
    }

    /**
     * Retrieve a boolean-values configuration field
     * 
     * @param field name of the configuration option
     * @return The value of the configuration option
     * @exception NullPointerException when the field does not exists
     */
    public static boolean getBoolValue(String field)
    {
        return "true".equals(getValue(field));
    }

    /**
     * Return the value of a configuration value as a variable. Variable expansion is performed
     * on the result.
     * 
     * @param field The field name to retrieve
     * @return The value of the field if present or null if not  
     */
    public String getVariableValue(String field)
    {
        String preExpansion = globals.installation.getProperty(field);

        if (preExpansion == null) {
            preExpansion = this.defaults.getProperty(field);

            if (preExpansion == null) {
                LOGGER.info("Key '" + field + "' is not found in " + globals.defaultConfig + " and " + globals.installationConfig);
                return null;
            }
        }

        return expander.expandVariables(preExpansion);
    }

    /**
     * Sets the application's root directory 
     * 
     * @param ap String containing the complete path to the root dir
     * @see #getApplicationPath
     * */
    public static void setApplicationPath(String ap)
    {
        setValue(ConfigKeys.APPLICATION_PATH, ap);
    }

    /**
     * Gets the complete path to the application's root dir
     * 
     * @return String with the path
     * @see #setApplicationPath
     * */
    public static String getApplicationPath()
    {
        return getValue(ConfigKeys.APPLICATION_PATH);
    }

    /**
     * Gets the path to the resource's directory.
     * This method returns the directory name where the config
     * files are stored. 
     * Note that this method does not return the complete path. If you 
     * want the full path, you must use 
     * <blockquote><pre>SystemGlobals.getApplicationPath() + SystemGlobals.getApplicationResourcedir()</pre></blockquote>
     * 
     * @return String with the name of the resource dir, relative 
     * to application's root dir.
     * @see #getApplicationPath()
     * */
    public static String getApplicationResourceDir()
    {
        return getValue(ConfigKeys.RESOURCE_DIR);
    }

    /**
     * Load the SQL queries
     *
     * @param queryFiles Complete path to the SQL queries file(s).
     **/
    public static void loadQueries(String... queryFiles)
    {
        File[] files = new File[queryFiles.length];
        for ( int i = 0; i < queryFiles.length; i++ )
        {
            files[i] = new File( queryFiles[i] );
        }
        loadQueries( files );
    }

    /**
     * Load the SQL queries
     *
     * @param queryFiles Complete path to the SQL queries file(s).
     **/
    public static void loadQueries(File... queryFiles)
    {
        for ( int i = 0; i < queryFiles.length; i++ )
        {
            LOGGER.info("Loading query file " + queryFiles[i] + " ...");
            loadProps( queries, queryFiles[i] );
        }
        debugValues( queries, "queries" );
    }

    /**
     * Loads an arbitrary property file into destination
     * <p>
     * @param destination 
     *        the loader Properties. Must not be <code>null</code>
     * @param file 
     *        file to be loaded. Must not be <code>null</code>
     */
    private static void loadProps( Properties destination, File file )
    {
        try
        {
            InputStream is = new FileInputStream( file );
            try
            {
                destination.load( is );
            }
            finally
            {
                is.close();
            } // try..finally
        }
        catch ( IOException e )
        {
            throw new ForumException( e );
        }
    }

    /**
     * Gets some SQL statement.
     * 
     * @param sql The query's name, as defined in the file loaded by
     * {@link #loadQueries(String)}
     * @return The SQL statement, or <code>null</code> if not found.
     * */
    public static String getSql(String sql)
    {
        return queries.getProperty(sql);
    }

    /**
     * Retrieve an iterator that iterates over all known configuration keys
     * 
     * @return An iterator that iterates over all known configuration keys
     */
    public static Iterator<Object> fetchConfigKeyIterator()
    {
        return globals.defaults.keySet().iterator();
    }

    public static Properties getConfigData()
    {
        return new Properties(globals.defaults);
    }

    /**
     * Lists all properties (expanded) in alphabetical order in logger
     * <p>
     * @param aProps the properties to be listed
     * @param aName the name of the 
     */
    private static void debugValues( Properties aProps, String aName )
    {
        // Note that the logger which emits this can be configured by using the param aName:
        // e.g.
        /*
            <logger name="net.jforum.util.preferences.SystemGlobals.defaults" additivity="true">
                <level value="debug" />
            </logger>

            <logger name="net.jforum.util.preferences.SystemGlobals.queries" additivity="true">
                <level value="info" />
            </logger>

            which will cause the defaults to be logged and the queries not.
         */

        Logger log = Logger.getLogger( SystemGlobals.class.getName() + "." + aName );
        if ( log.isDebugEnabled() )
        {
            StringBuilder sb = new StringBuilder( "SystemGlobals." );
            sb.append( aName ).append( " contains values:" );

            Enumeration<?> keys = aProps.propertyNames();

            if ( !keys.hasMoreElements() )
            {
                sb.append( " <none>" );
            }
            else
            {
                Map<String,String> sorted = new TreeMap<String,String>();
                while ( keys.hasMoreElements() )
                {
                    String key = (String) keys.nextElement();
                    String preExpansion = aProps.getProperty( key );
                    sorted.put( key, globals.expander.expandVariables(preExpansion) );
                }

                for (Map.Entry<String,String> entry : sorted.entrySet())
                {
                    sb.append( "\n    " ).append( entry.getKey()).append(" = ").append(entry.getValue());
                }
            }

            log.debug( sb.toString() );
        }
    }
}
