package net.jforum.dao;

import java.util.List;

/**
 * Model interface for spam patterns and collections of them. <p/>
 *
 * This interface defines methods which are expected to be
 * implemented by a specific data access driver. The intention is to provide
 * all functionality needed to insert, delete and select some specific data.
 */

public interface SpamDAO {
    /**
     * Returns all the censored words currently in the database.
     */
    List<String> selectAll();

    /**
     * Adds the specified spam pattern to the database
     */
    void addSpam (String pattern);

    /**
     * Removes the specified spam pattern from the database
     */
    void deleteSpam (String pattern);
}
