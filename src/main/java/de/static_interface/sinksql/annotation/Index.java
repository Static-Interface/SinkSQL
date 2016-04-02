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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Index {

    /**
     * The name of the index. Default will be the <code>columname_I</code> if no other name has been specified.<br/><br/>
     * <b>Example:</b><br/>
     * <code>
     *     &#64;Column(name = "user_id")<br/>
     *     &#64;Index<br/>
     *     public int userId;<br/>
     * </code>
     * The default <code>name</code> would be in this case <code>user_id_I</code><br/><br/>
     * Throws an exception on {@link AbstractTable#create()} if an index with this name already exists
     * @return the name of the index
     */
    String name() default "";
}
