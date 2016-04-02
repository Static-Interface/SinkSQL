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

package de.static_interface.sinksql.database.impl.table;

import de.static_interface.sinksql.database.AbstractTable;
import de.static_interface.sinksql.database.CascadeAction;
import de.static_interface.sinksql.database.Database;
import de.static_interface.sinksql.database.impl.row.OptionsRow;
import de.static_interface.sinksql.database.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import javax.annotation.Nullable;

public abstract class OptionsTable extends AbstractTable<OptionsRow> {

    /**
     * A predefined table for options
     * @param name the name of the table
     * @param db the database
     */
    public OptionsTable(String name, Database db) {
        super(name, db);
    }

    /**
     * Set an options value
     * @param key the option key
     * @param value the option value. Any Java-POJO object is supported
     */
    public void setOption(String key, Object value) {
        setOption(key, value, null);
    }

    /**
     * Set an options value
     * @param key the option key
     * @param value the option value. Any Java-POJO object is supported
     * @param foreignTarget the associated foreignkey target (for example, a userId if it is an user-based option)
     */
    public void setOption(String key, Object value, @Nullable Integer foreignTarget) {
        if (value != null && value.equals("null")) {
            value = null;
        }

        OptionsRow row;
        String parsedValue;
        try {
            row = getRowClass().newInstance();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();
            parsedValue = Base64.getEncoder().encodeToString(baos.toByteArray());
            row.key = key;
            row.value = parsedValue;
            row.foreignTarget = foreignTarget;
            insert(row);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param key the option key
     * @return the deserialized option value which was set using {@link #setOption(String, Object)}
     */
    public Object getOption(String key) {
        return getOptionInternal("SELECT * FROM {TABLE} WHERE `key`=?", Object.class, false, key);
    }

    /**
     * @param key the option key
     * @param foreignId the foreignkey associated with the option (for example, a users id)
     * @return the deserialized option value which was set using {@link #setOption(String, Object, Integer)}
     */
    public Object getOption(String key, Integer foreignId) {
        return getOptionInternal("SELECT * FROM `{TABLE}` WHERE `key`=? AND `foreignTarget`=?", Object.class, false, key, foreignId);
    }

    @Override
    public Class<OptionsRow> getRowClass() {
        return OptionsRow.class;
    }

    /**
     * @param key the option key
     * @param clazz the return type
     * @param defaultValue the default value if no option with this key exists
     * @param <K> the return type
     * @return the {@link K} value of the option
     */
    public <K> K getOption(String key, Class<K> clazz, K defaultValue) {
        try {
            return getOptionInternal("SELECT * FROM {TABLE} WHERE `key`=?", clazz, true, key);
        } catch (NullPointerException ignored) {
            return defaultValue;
        }
    }

    /**
     * @param key the option key
     * @param foreignId the foreignkey associated with the option (for example, a users id)
     * @param clazz the return type
     * @param defaultValue the default value if no option with this key exists
     * @param <K> the return type
     * @return the {@link K} value of the option
     */
    public <K> K getOption(String key, @Nullable Integer foreignId, Class<K> clazz, K defaultValue) {
        try {
            return getOptionInternal("SELECT * FROM {TABLE} WHERE `key`=? AND `foreignTarget`=?", clazz, true, key, foreignId);
        } catch (NullPointerException ignored) {
            return defaultValue;
        }
    }

    private <K> K getOptionInternal(String query, Class<K> clazz, boolean throwExceptionOnNull, Object... bindings) {
        String s;
        OptionsRow[] result = get(query, bindings);
        if (result == null || result.length < 1) {
            if (throwExceptionOnNull) {
                throw new NullPointerException();
            }
            return null;
        }
        s = result[0].value;
        if (s == null) {
            return null;
        }
        try {
            byte[] data = Base64.getDecoder().decode(s);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return (K) o;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error deserializing \"" + s + "\" on query: " + query + ", params: [" + StringUtil.formatArrayToString(bindings, ", ") + "]", e);
        }
    }

    @Nullable
    /**
     * @return the table for the {@link OptionsRow#foreignTarget}
     */
    public abstract Class<? extends AbstractTable> getForeignTable();

    /**
     * @return the column for the {@link OptionsRow#foreignTarget}
     */
    public abstract String getForeignColumn();

    /**
     * @return the onUpdate {@link CascadeAction} for the {@link OptionsRow#foreignTarget}
     */
    public abstract CascadeAction getForeignOnUpdateAction();

    /**
     * @return the onDelete {@link CascadeAction} for the {@link OptionsRow#foreignTarget}
     */
    public abstract CascadeAction getForeignOnDeleteAction();
}
