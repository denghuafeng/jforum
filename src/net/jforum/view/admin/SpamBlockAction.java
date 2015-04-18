package net.jforum.view.admin;

import java.util.List;

import net.jforum.dao.SpamDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.util.preferences.TemplateKeys;

import org.apache.log4j.Logger;

public class SpamBlockAction extends AdminCommand {

    private static final Logger LOG = Logger.getLogger(SpamBlockAction.class);

    @Override
    public void list() {
        List<String> words = getSpamDao().selectAll();
        context.put("spamPatterns", words);
        setTemplateName(TemplateKeys.SPAM_BLOCK_LIST);
    }

    public void insert() {
        String pattern = request.getParameter("pattern");
        LOG.info("Creating " + pattern);
        getSpamDao().addSpam(pattern);
        this.list();
    }

    public void delete() {
        String pattern = request.getParameter("pattern");
        LOG.info("Deleting " + pattern);
        getSpamDao().deleteSpam(pattern);
        this.list();
    }

    private SpamDAO getSpamDao() {
        return DataAccessDriver.getInstance().newSpamDAO();
    }
}
