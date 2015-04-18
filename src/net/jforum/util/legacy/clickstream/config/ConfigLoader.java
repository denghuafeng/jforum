package net.jforum.util.legacy.clickstream.config;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.jforum.exceptions.ConfigLoadException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *Loads clickstream.xml for JForum.
 * 
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 * @author Rafael Steil (little hacks for JForum)
 * @version $Id$
 */
public class ConfigLoader
{
	private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class);

	private transient ClickstreamConfig config;

	private static ConfigLoader instance = new ConfigLoader();;

	public static ConfigLoader getInstance()
	{
		return instance;
	}

	private ConfigLoader() {}

	public ClickstreamConfig getConfig()
	{
		if (this.config != null) {
			return this.config;
		}

		synchronized (instance) {
			this.config = new ClickstreamConfig();
	
			try {
				final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
	
				final String path = SystemGlobals.getValue(ConfigKeys.CLICKSTREAM_CONFIG);
				
				if (path != null) {
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("Loading clickstream config from " + path);
					}
					
					final File fileInput = new File(path);
					
					if (fileInput.exists()) {
						parser.parse(fileInput, new ConfigHandler());
					}
					else {
						parser.parse(new InputSource(path), new ConfigHandler());
					}
				}
				return config;
			}
			catch (SAXException e) {
				LOGGER.error("Could not parse clickstream XML", e);
				throw new ConfigLoadException(e.getMessage());				
			}
			catch (IOException e) {
				LOGGER.error("Could not read clickstream config from stream", e);
				throw new ConfigLoadException(e.getMessage());				
			}
			catch (ParserConfigurationException e) {
				LOGGER.fatal("Could not obtain SAX parser", e);
				throw new ConfigLoadException(e.getMessage());				
			}						
		}
	}

	/**
	 * SAX Handler implementation for handling tags in config file and building config objects.
	 */
	private class ConfigHandler extends DefaultHandler
	{
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			if ("bot-host".equals(qName)) {
				config.addBotHost(attributes.getValue("name"));
			}
			else if ("bot-agent".equals(qName)) {
				config.addBotAgent(attributes.getValue("name"));
			}
		}
	}
}
