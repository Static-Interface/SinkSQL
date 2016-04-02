/*
 * Copyright (c) 2013 - 2016 Trojaner <trojaner25@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.sinksql.database.query.impl;

import de.static_interface.sinksql.database.Row;
import de.static_interface.sinksql.database.query.Query;
import de.static_interface.sinksql.database.query.SubQuery;
import de.static_interface.sinksql.database.query.condition.WhereCondition;

public class WhereQuery<T extends Row> extends SubQuery<T> {
    private WhereCondition condition;
    private String column;
    private int paranthesisState = 0;

    public WhereQuery(Query<T> parent, String column, WhereCondition condition) {
        super(parent);
        this.column = column;
        this.condition = condition;
    }

    public WhereQuery<T> openParanthesis() {
        paranthesisState = 1;
        return this;
    }

    public WhereQuery<T> closeParanthesis() {
        paranthesisState = 2;
        return this;
    }

    public void resetParanthesisState() {
        paranthesisState = 0;
    }

    public int getParanthesisState() {
        return paranthesisState;
    }

    public WhereCondition getCondition() {
        return condition;
    }

    /**
     * SQL <code>AND</code> for <code>WHERE</code> clauses
     * @param column the column
     * @param condition the condition
     */
    public AndQuery<T> and(String column, WhereCondition condition) {
        AndQuery<T> query = new AndQuery(this, column, condition);
        setChild(query);
        return query;
    }

    /**
     * SQL <code>OR</code> statement for <code>WHERE</code> clauses
     * @param column the column
     * @param condition the condition
     */
    public OrQuery<T> or(String column, WhereCondition condition) {
        OrQuery<T> query = new OrQuery(this, column, condition);
        setChild(query);
        return query;
    }

    public String getColumn() {
        return column;
    }
}
