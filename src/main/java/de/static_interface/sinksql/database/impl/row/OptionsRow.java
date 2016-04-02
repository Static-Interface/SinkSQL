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

package de.static_interface.sinksql.database.impl.row;

import de.static_interface.sinksql.database.IdRow;
import de.static_interface.sinksql.database.annotation.Column;

import javax.annotation.Nullable;

public final class OptionsRow implements IdRow {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    /**
     * The option key
     */
    @Column
    public String key;

    /**
     * The binary serialized option value
     */
    @Column
    public String value;

    /**
     * The optional foreignkey target associated with this option
     */
    @Column
    @Nullable
    public Integer foreignTarget;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getIdColumn() {
        return "id";
    }
}
