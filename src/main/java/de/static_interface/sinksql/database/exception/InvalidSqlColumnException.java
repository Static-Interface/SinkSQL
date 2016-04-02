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

package de.static_interface.sinksql.database.exception;


import de.static_interface.sinksql.database.AbstractTable;
import de.static_interface.sinksql.database.Row;
import de.static_interface.sinksql.database.annotation.Column;
import de.static_interface.sinksql.database.annotation.ForeignKey;
import de.static_interface.sinksql.database.annotation.Index;

import java.lang.reflect.Field;

public class InvalidSqlColumnException extends RuntimeException {

    /**
     * This is thrown when an SQL table with an invalid {@link Row} implementation called {@link AbstractTable#create()}
     * Examples: A boolean which was annotated as <code>UNSIGNED</code>, Strings with <code>ZEROFILL</code>, etc...
     * @param table The parent table of the coulm which couldn't be created
     * @param columnField The wrapper Field of the column which couldn't be created
     * @param columName The name of the column which couldn't be created
     * @param reason The reason why it failed
     * @see Column
     * @see ForeignKey
     * @see Index
     */
    public InvalidSqlColumnException(AbstractTable table, Field columnField, String columName, String reason) {
        super("Column \"" + columName + "\" " + "(wrapper: " + columnField.getType().getName() + ") on table \"" + table.getName() + "\" (wrapper: "
              + table.getClass().getName() + ") couldn't be created: " + reason);
    }
}
