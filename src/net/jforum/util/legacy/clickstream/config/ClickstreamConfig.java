package net.jforum.util.legacy.clickstream.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickstream configuration data.
 *
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 * @author Rafael Steil (little hacks for JForum)
 * @version $Id$
 */
public class ClickstreamConfig {
    private transient final List<String> botAgents = new ArrayList<String>();
    private transient final List<String> botHosts = new ArrayList<String>();

    public void addBotAgent(final String agent) {
        botAgents.add(agent);
    }

    public void addBotHost(final String host) {
        botHosts.add(host);
    }

    public List<String> getBotAgents() {
        return botAgents;
    }

    public List<String> getBotHosts() {
        return botHosts;
    }
}
