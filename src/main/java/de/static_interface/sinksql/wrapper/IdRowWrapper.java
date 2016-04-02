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

package de.static_interface.sinksql.wrapper;

import de.static_interface.sinksql.AbstractTable;
import de.static_interface.sinksql.IdRow;
import de.static_interface.sinksql.query.Query;

public abstract class IdRowWrapper<T extends IdRow> implements RowWrapper<T> {
    private int id;
    private String idColumn;
    private AbstractTable<T> table;

    public IdRowWrapper(AbstractTable<T> table, T row) {
        id = row.getId();
        idColumn = row.getIdColumn();
        this.table = table;
    }

    public T getBase() {
        return Query.from(table).select().where(idColumn, Query.eq("?")).get(id);
    }

    public int getId() {
        return id;
    }

    public AbstractTable<T> getTable() {
        return table;
    }
}
