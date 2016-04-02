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

package de.static_interface.sinksql.database.annotation;

import de.static_interface.sinksql.database.AbstractTable;
import de.static_interface.sinksql.database.CascadeAction;
import de.static_interface.sinksql.database.Row;
import de.static_interface.sinksql.database.util.ReflectionUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /**
     * The name of the column. Default will be the <i>fieldname</i> if no other name has been specified.<br/><br/>
     * <b>Example:</b><br/>
     * <code>
     *     &#64;Column(name = "user_id")<br/>
     *     public int userId;
     * </code>
     * @return the name of the column
     */
    String name() default "";

    /**
     * True means that the value  will be auto incremented when {@link AbstractTable#insert(Row)} is called. <br/><br/>
     * Throws an exception on {@link AbstractTable#create()} if {@link ReflectionUtil#isNumber(Class)} returns false on this field
     * @return true if auto increment the column value on insert
     */
    boolean autoIncrement() default false;

    /**
     * Will make the column a SQL <code>PRIMARY KEY</code>. <b>Only one <code>PRIMARY KEY</code> per table is allowed</b><br/><br/>
     * Throws an exception on {@link AbstractTable#create()} if there are more than one columns defined as <code>PRIMARY KEY</code>
     * @return true if they column is a SQL <code>PRIMARY KEY</code>
     */
    boolean primaryKey() default false;

    /**
     * Doesn't allows rows with the same value on this column. Can be used on unique names, UUID's etc...<br/>
     * The SQL type is <code>UNIQUE KEY</code><br/><br/>
     * Throws an exception on {@link AbstractTable#insert(Row)} if a row with the same value on this column already exists
     * @return true if the column is a SQL <code>UNIQUE KEY</code>
     * @deprecated Use the {@link UniqueKey} annotation
     */
    @Deprecated
    boolean uniqueKey() default false;

    /**
     * Not escaped SQL comment. Shouldn't use not escaped quotation marks.
     * The SQL type is <code>COMMENT</code><br/><br/>
     * @return the SQL <code>COMMENT</code> of this column
     */
    String comment() default "";

    /**
     * Only useful with {@link CascadeAction#SET_DEFAULT}<br/>
     * <b>This is plain SQL</b><br/><br/>
     * <b>Example:</b><br/>
     * <code>
     *     &#64;Column(defaultValue = "123")<br/>
     *     public int userId;<br/><br/>
     *
     *     &#64;Column(defaultValue = "\"noname\"")<br/>
     *     public String name;<br/><br/>
     * </code>
     * @return the default value as SQL
     */
    String defaultValue() default "";

    /**
     * Makes the number type <code>UNSIGNED</code>.<br/>
     * <b>Note: </b>Java doesn't support unsigned bytes, floats, doubles, shorts, integers and longs. So this should be used carefully. <br/><br/>
     * Throws an exception on {@link AbstractTable#create()} if {@link ReflectionUtil#isNumber(Class)} returns false for the annotated field.<br/>
     * Throws an exception on {@link AbstractTable#insert(Row)} if the value is less than 0.
     * @return true if the field is an <code>UNSIGNED</code> number
     */
    boolean unsigned() default false;

    /**
     * Makes the number type <code>ZEROFILL</code>.<br/>
     * <b>Note: <code>ZEROFILL</code> values are also {@link Column#unsigned()}</b><br/><br/>
     * Throws an exception on {@link AbstractTable#create()} if {@link ReflectionUtil#isNumber(Class)} returns false for the annotated field.<br/>
     * Throws an exception on {@link AbstractTable#insert(Row)} if the value is less than 0.
     * @return the if the field should be <code>ZEROFILL</code>
     */
    boolean zerofill() default false;

    /**
     * The SQL key length
     * @return the key length
     */
    int keyLength() default -1;
}
