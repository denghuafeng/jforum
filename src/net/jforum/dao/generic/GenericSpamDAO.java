package net.jforum.dao.generic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import net.jforum.*;
import net.jforum.dao.*;
import net.jforum.exceptions.*;
import net.jforum.repository.SpamRepository;
import net.jforum.util.*;
import net.jforum.util.preferences.*;

public class GenericSpamDAO implements SpamDAO {

	public List<String> selectAll() {
		List<String> result = new ArrayList<String>();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = preparedStatementFromSqlKey("Spam.selectAll");

			rs = p.executeQuery();

			while (rs.next()) {
				result.add(rs.getString("pattern"));
			}

			return result;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			DbUtils.close(rs, p);
		}
	}

    public void addSpam (String pattern) {
        PreparedStatement p = null;
        try {
            p = preparedStatementFromSqlKey("Spam.create");

            p.setString(1, pattern);

            int recordsAdded = p.executeUpdate();

            if (recordsAdded == 1) {
				SpamRepository.load();
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            DbUtils.close(p);
        }
    }

    public void deleteSpam (String pattern) {
        PreparedStatement p = null;
        try {
            p = preparedStatementFromSqlKey("Spam.delete");

            p.setString(1, pattern);

            int recordsDeleted = p.executeUpdate();

            if (recordsDeleted == 1) {
				SpamRepository.load();
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            DbUtils.close(p);
        }
    }

    private PreparedStatement preparedStatementFromSqlKey (String sqlKey) throws SQLException {
        String sql = SystemGlobals.getSql(sqlKey);
        return JForumExecutionContext.getConnection().prepareStatement(sql);
    }
}
