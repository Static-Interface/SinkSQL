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

package de.static_interface.sinksql.annotation;


import de.static_interface.sinksql.AbstractTable;
import de.static_interface.sinksql.CascadeAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

    /**
     * The table which is referenced by this foreign key
     * @return the referenced table
     */
    Class<? extends AbstractTable> table();

    /**
     * The column of the table which is referenced by this foreign key
     * @return the referenced column
     */
    String column();

    /**
     * The action which is performed when the referenced row got deleted
     * @see CascadeAction
     * @return the onDelete cascade action
     */
    CascadeAction onDelete() default CascadeAction.RESTRICT; //MySQL default

    /**
     * The action which is performed when the referenced row got updated
     * @see CascadeAction
     * @return the onUpdate cascade action
     */
    CascadeAction onUpdate() default CascadeAction.RESTRICT; //MySQL default
}
