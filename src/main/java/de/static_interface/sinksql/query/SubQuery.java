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

package de.static_interface.sinksql.query;

import de.static_interface.sinksql.AbstractTable;
import de.static_interface.sinksql.Row;

public abstract class SubQuery<T extends Row> extends Query<T> {

    public SubQuery(Query parent) {
        super(parent);
    }

    public MasterQuery<T> getMasterQuery() {
        return getParent().getMasterQuery();
    }

    @Override
    public AbstractTable<T> getTable() {
        return getParent().getTable();
    }

    @Override
    public void setTable(AbstractTable<T> table) {
        getParent().setTable(table);
    }
}
