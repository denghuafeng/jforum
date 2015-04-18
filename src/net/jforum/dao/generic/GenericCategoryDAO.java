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
 * Created on Mar 6, 2003 / 11:09:34 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.entities.Category;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericCategoryDAO extends AutoKeys implements net.jforum.dao.CategoryDAO
{
	/**
	 * @see net.jforum.dao.CategoryDAO#selectById(int)
	 */
	public Category selectById(final int categoryId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("CategoryModel.selectById"));
			pstmt.setInt(1, categoryId);

			resultSet = pstmt.executeQuery();

			Category category = new Category();
			if (resultSet.next()) {
				category = this.getCategory(resultSet);
			}

			return category;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#selectAll()
	 */
	public List<Category> selectAll()
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("CategoryModel.selectAll"));
			final List<Category> list = new ArrayList<Category>();

			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(this.getCategory(resultSet));
			}
			return list;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	protected Category getCategory(final ResultSet resultSet) throws SQLException
	{
		final Category category = new Category();

		category.setId(resultSet.getInt("categories_id"));
		category.setName(resultSet.getString("title"));
		category.setOrder(resultSet.getInt("display_order"));
		category.setModerated(resultSet.getInt("moderated") == 1);

		return category;
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#canDelete(int)
	 */
	public boolean canDelete(final int categoryId)
	{
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = JForumExecutionContext.getConnection()
					.prepareStatement(SystemGlobals.getSql("CategoryModel.canDelete"));
			pstmt.setInt(1, categoryId);

			resultSet = pstmt.executeQuery();
			return !resultSet.next() || resultSet.getInt("total") < 1;

		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#delete(int)
	 */
	public void delete(final int categoryId)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("CategoryModel.delete"));
			pstmt.setInt(1, categoryId);
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#update(net.jforum.entities.Category)
	 */
	public void update(final Category category)
	{
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(SystemGlobals.getSql("CategoryModel.update"));
			pstmt.setString(1, category.getName());
			pstmt.setInt(2, category.isModerated() ? 1 : 0);
			pstmt.setInt(3, category.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#addNew(net.jforum.entities.Category)
	 */
	public int addNew(final Category category)
	{
		int order = 1;
		ResultSet resultSet = null;
		PreparedStatement pstmt = null;
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("CategoryModel.getMaxOrder"));
			resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				order = resultSet.getInt(1) + 1;
			}
			resultSet.close();
			pstmt.close();

			pstmt = this.getStatementForAutoKeys("CategoryModel.addNew");
			pstmt.setString(1, category.getName());
			pstmt.setInt(2, order);
			pstmt.setInt(3, category.isModerated() ? 1 : 0);

			this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("CategoryModel.lastGeneratedCategoryId"));
			final int id = this.executeAutoKeysQuery(pstmt);

			category.setId(id);
			category.setOrder(order);
			return id;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(resultSet, pstmt);
		}
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#setOrderUp(Category, Category)
	 */
	public void setOrderUp(final Category category, final Category relatedCategory)
	{
		this.setOrder(category, relatedCategory);
	}

	/**
	 * @see net.jforum.dao.CategoryDAO#setOrderDown(Category, Category)
	 */
	public void setOrderDown(final Category category, final Category relatedCategory)
	{
		this.setOrder(category, relatedCategory);
	}

	/**
	 * @param category Category
	 * @param otherCategory Category
	 */
	private void setOrder(final Category category, final Category otherCategory)
	{
		final int tmpOrder = otherCategory.getOrder();
		otherCategory.setOrder(category.getOrder());
		category.setOrder(tmpOrder);

		PreparedStatement pstmt = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("CategoryModel.setOrderById"));
			pstmt.setInt(1, otherCategory.getOrder());
			pstmt.setInt(2, otherCategory.getId());
			pstmt.executeUpdate();
			pstmt.close();

			pstmt = JForumExecutionContext.getConnection().prepareStatement(
					SystemGlobals.getSql("CategoryModel.setOrderById"));
			pstmt.setInt(1, category.getOrder());
			pstmt.setInt(2, category.getId());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}
}
