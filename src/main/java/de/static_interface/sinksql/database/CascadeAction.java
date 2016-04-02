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

package de.static_interface.sinksql.database;

/**
 * See <a href="http://dev.mysql.com/doc/refman/5.6/en/create-table-foreign-keys.html">MySQL documentation</a> for more information
 */
public enum CascadeAction {
    /**
     * Rejects the delete or update operation for the parent table. Specifying RESTRICT (or NO ACTION) is the same as omitting the ON DELETE or ON UPDATE clause.
     */
    RESTRICT,
    /**
     * Delete or update the row from the parent table, and automatically delete or update the matching rows in the child table
     */
    CASCADE,
    /**
     * Delete or update the row from the parent table, and set the foreign key column or columns in the child table to NULL
     */
    SET_NULL,
    /**
     * Delete or update the row from the parent table, and set the foreign key column or columns in the child table to NULL.
     */
    SET_DEFAULT,
    /**
     * A keyword from standard SQL. In MySQL, equivalent to RESTRICT.
     */
    NO_ACTION;

    public String toSql() {
        return name().replace("_", " ");
    }
}
