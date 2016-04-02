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

import de.static_interface.sinksql.database.AbstractTable;
import de.static_interface.sinksql.database.Row;
import de.static_interface.sinksql.database.query.MasterQuery;
import de.static_interface.sinksql.database.query.Query;

public class FromQuery<T extends Row> extends Query<T> {
    public FromQuery(AbstractTable<T> table) {
        super(null);
        setTable(table);
    }

    /**
     * Start a select query. Calls {@link #select(String...)} with "*" as argument
     */
    public SelectQuery<T> select() {
        return select("*");
    }

    /**
     * Start a select query
     @param columns the columns which should be queried or * for all columns
     */
    public SelectQuery<T> select(String... columns) {
        SelectQuery<T> query = new SelectQuery(this, columns);
        setChild(query);
        return query;
    }

    /**
     * Starts an update query
     */
    public UpdateQuery<T> update() {
        UpdateQuery<T> query = new UpdateQuery<>(this);
        setChild(query);
        return query;
    }

    /**
     * Starts a delete query
     */
    public DeleteQuery<T> delete() {
        DeleteQuery<T> query = new DeleteQuery<>(this);
        setChild(query);
        return query;
    }

    @Override
    public MasterQuery<T> getMasterQuery() {
        return (MasterQuery<T>) getChild();
    }
}
