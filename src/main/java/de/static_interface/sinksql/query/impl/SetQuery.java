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

package de.static_interface.sinksql.query.impl;

import de.static_interface.sinksql.Row;
import de.static_interface.sinksql.query.Query;
import de.static_interface.sinksql.query.SubQuery;
import de.static_interface.sinksql.query.condition.WhereCondition;

public class SetQuery<T extends Row> extends SubQuery<T> {
    private final String column;
    private final Object value;

    public SetQuery(Query<T> parent, String column, Object value) {
        super(parent);
        this.column = column;
        this.value = value;
    }

    public SetQuery<T> set(String column, Object value) {
        SetQuery<T> query = new SetQuery(this, column, value);
        setChild(query);
        return query;
    }

    public WhereQuery<T> where(String columName, WhereCondition condition) {
        WhereQuery<T> query = new WhereQuery(this, columName, condition);
        setChild(query);
        return query;
    }

    public Object getValue() {
        return value;
    }

    public String getColumn() {
        return column;
    }
}
