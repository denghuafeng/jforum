package net.jforum.csrf;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.owasp.csrfguard.*;
import org.owasp.csrfguard.util.*;

/**
 * Reads OWASP format property file with one exception. We add all the
 * "org.owasp.csrfguard.unprotected" properties at runtime using the
 * csrf.properties file.
 * 
 * Except for the new addCsrfExcludeProperties() method, all code was copied
 * from OWASP's CsrfGuardServletContextListener.java
 * https://www.owasp.org/index.php/CSRFGuard_3_Configuration
 * 
 * Also added appPath since /WEB-INF wasn't loading
 * 
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class CsrfListener implements ServletContextListener {
    private final static String CONFIG_PARAM = "Owasp.CsrfGuard.Config";
    private final static String CONFIG_EXTENSIONS_PARAM = "Owasp.CsrfGuard.Config.Extensions";
    private final static String CONFIG_PRINT_PARAM = "Owasp.CsrfGuard.Config.Print";

    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String appPath = event.getServletContext().getRealPath("");
        String config = context.getInitParameter(CONFIG_PARAM);
        String extensions = context.getInitParameter(CONFIG_EXTENSIONS_PARAM);
        if (config == null) {
            throw new RuntimeException(String.format("failure to specify context init-param - %s", CONFIG_PARAM));
        }
        if (extensions == null) {
            throw new RuntimeException(String.format("failure to specify context init-param - %s",
                    CONFIG_EXTENSIONS_PARAM));
        }
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = getResourceStream(appPath + config, context);
            properties.load(is);
            addCsrfExcludeProperties(appPath + extensions, properties);
            CsrfGuard.load(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Streams.close(is);
        }
        String printConfig = context.getInitParameter(CONFIG_PRINT_PARAM);
        if (printConfig != null && Boolean.parseBoolean(printConfig)) {
            context.log(CsrfGuard.getInstance().toString());
        }
    }

    /**
     * custom function
     * 
     * @throws IOException
     * @throws
     */
    private void addCsrfExcludeProperties(String csrfPath, Properties properties) throws IOException {
        File csrfFile = new File(csrfPath);
        Properties csrfProperties = new Properties();
        csrfProperties.load(new FileInputStream(csrfFile));
        int i = 0;
        for (Object key : csrfProperties.keySet()) {
            String value = csrfProperties.getProperty(key.toString());
            i++;
            if (!value.equals("AddToken")) {
                properties.put("org.owasp.csrfguard.unprotected." + i, key);
            }
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        /** nothing to do **/
    }

    private InputStream getResourceStream(String resourceName, ServletContext context) throws IOException {
        InputStream is = null;
        /** try classpath **/
        is = getClass().getClassLoader().getResourceAsStream(resourceName);
        /** try web context **/
        if (is == null) {
            String fileName = context.getRealPath(resourceName);
            if (fileName != null) {
            	File file = new File(fileName); 
            	if (file.exists()) {
            		is = new FileInputStream(fileName);
            	}
            }
        }
        /** try current directory **/
        if (is == null) {
        	if (resourceName != null) {
        		File file = new File(resourceName);        	
        		if (file.exists()) {
        			is = new FileInputStream(resourceName);
        		}
        	}
        }
        /** fail if still empty **/
        if (is == null) {
            throw new IOException(String.format("unable to locate resource - %s", resourceName));
        }
        return is;
    }
}

