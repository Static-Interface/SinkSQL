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

import de.static_interface.sinksql.database.annotation.Column;
import de.static_interface.sinksql.database.annotation.ForeignKey;
import de.static_interface.sinksql.database.annotation.Index;
import de.static_interface.sinksql.database.query.Query;
import de.static_interface.sinksql.database.util.ReflectionUtil;
import de.static_interface.sinksql.database.util.StringUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class which allows interaction with a SQL table
 * @param <T> See {@link Row}
 */
public abstract class AbstractTable<T extends Row> {

    private static Map<Class<?>, Map<Class<?>, SqlObjectConverter>> convertProviders = new HashMap<>();
    private final String name;
    protected Database db;
    private boolean reconnected = false;
    /**
     * @param name the name of the table
     * @param db the database of this table
     */
    public AbstractTable(String name, Database db) {
        this.name = name;
        this.db = db;
    }

    public static <K, E> void registerSqlConverter(@Nonnull Class<K> databaseType, @Nonnull Class<E> objectType, SqlObjectConverter<K, E> converter) {
        if (databaseType != Database.class && !Database.class.isAssignableFrom(databaseType)) {
            throw new ClassCastException("Can't cast class \"" + databaseType.getName() + "\" to \"" + Database.class.getName() + "\"!");
        }
        Map<Class<?>, SqlObjectConverter> converters = convertProviders.get(databaseType);
        if (converters == null) {
            converters = new HashMap<>();
        }

        if (converters.get(objectType) != null) {
            throw new IllegalStateException(
                    "Class \"" + objectType.getSimpleName() + "\" already has a sql converter for database type: \"" + databaseType.getSimpleName()
                    + "\"");
        }

        converters.put(objectType, converter);
        convertProviders.put(databaseType, converters);
    }

    /**
     * @param rs the ResulSet to check
     * @param columnName the name of the column to check
     * @return thrue if the ResultSet contains the columnNmae
     * @throws SQLException
     */
    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    public <K, E> SqlObjectConverter<K, E> getSqlConverter(Class<K> databaseType, Class<E> objectType) {
        Map<Class<?>, SqlObjectConverter> converters = convertProviders.get(databaseType);

        if (converters != null) {
            if (converters.containsKey(objectType)) {
                return converters.get(objectType);
            }

            // No converter for this class available, search for of the interfaces

            Class<?> matchedClass = objectType;
            int found = 0;

            for (Class<?> clazz : objectType.getInterfaces()) {
                if (converters.containsKey(clazz)) {
                    found++;
                    matchedClass = clazz;
                }
            }

            if (found > 0) {
                if (found > 1) {
                    throw new IllegalStateException(
                            "Found multiple possible SqlObjectConverters for class: " + objectType.getSimpleName() + " in database: " + databaseType
                                    .getSimpleName());
                }

                return converters.get(matchedClass);
            }

            // Loop trough all superclasses, since there is also no SqlObjectConverter for the interface
            while (true) {
                if (converters.containsKey(matchedClass)) {
                    break;
                }

                matchedClass = matchedClass.getSuperclass();
                if (matchedClass == null) {
                    break;
                }
            }

            if (matchedClass != null) {
                return converters.get(matchedClass);
            }
        }

        if (databaseType.getSuperclass() != null && (databaseType == Database.class ||
                                                     Database.class.isAssignableFrom(databaseType.getSuperclass()))) {
            return (SqlObjectConverter<K, E>) getSqlConverter((Class<? extends Database>) databaseType.getSuperclass(), objectType);
        }

        return null;
    }

    /**
     * @return the prefixed name of the table
     */
    public final String getName() {
        validateConnection();
        return db.getConnectionInfo().getTablePrefix() + name;
    }

    /**
     * Get the database of this table
     * @return the database of this table
     */
    public final Database getDatabase() {
        return db;
    }

    /**
     * Create the table
     * @see ForeignKey
     * @see Column
     * @see Index
     * @throws SQLException if the {@link Row} class is malformed
     */
    @SuppressWarnings("deprecation")
    public void create() throws SQLException {
        db.createTable(this);
    }

    /**
     * @return the SQL storage engine
     */
    public String getEngine() {
        return "InnoDB"; // Table implemetations may override this
    }

    /**
     * @see {@link #toSqlValue(Object, boolean)}
     */
    public String toSqlValue(Object o) {
        return toSqlValue(o, false);
    }

    /**
     * Tries to convert an object to an sql parsable value<br/>
     * Adds (")'s to the start and ends of strings and escapes all other "'s
     * @param o The object to parse
     * @param strict if strict, strings like "?" and "null" will also be string values and not auto converted
     * @return the parsed sql value
     */
    public String toSqlValue(Object o, boolean strict) {
        SqlObjectConverter converter = getSqlConverter(getDatabase().getClass(), o.getClass());
        if (converter != null) {
            return converter.convert(getDatabase(), o, strict);
        }

        if (o instanceof String) {
            if (o.equals("?") && !strict) {
                return (String) o;
            }

            if (((String) o).equalsIgnoreCase("null") && !strict) {
                return null;
            }
        }

        //Todo: expand default supported types (e.g. like Date etc)

        if (ReflectionUtil.isPrimitiveClass(o.getClass()) ||
            ReflectionUtil.isWrapperClass(o.getClass())) {
            return o.toString();
        }

        return getDatabase().stringify(o.toString());
    }

    /**
     * Get the result as deserialized {@link T}[] from the given query
     * @param query The SQL query, <code>{TABLE}</code> will be replaced with {@link #getName()}
     * @param bindings the {@link PreparedStatement} bindings
     * @return the {@link ResultSet} deserialized as {@link T}
     * @deprecated Use the {@link Query} API with {@link Query#get(Object...)} or {@link Query#getResults(Object...)}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public T[] get(String query, Object... bindings) {
        try {
            query = query.replaceAll("\\Q{TABLE}\\E", getName());
            validateConnection();
            PreparedStatement statement = db.getConnection().prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            if (bindings != null && bindings.length > 0) {
                int i = 1;
                for (Object s : bindings) {
                    statement.setObject(i, s);
                    i++;
                }
            }

            ResultSet rs = executeQuery(query, bindings);

            List<T> result = deserializeResultSet(rs);
            rs.close();
            T[] array = (T[]) Array.newInstance(getRowClass(), result.size());
            return result.toArray(array);
        } catch (SQLException e) {
            System.out.println("Couldn't execute SQL query: " + sqlToString(query, bindings));
            throw new RuntimeException(e);
        }
    }

    protected void validateConnection() {
        if (!db.isConnected()) {
            throw new IllegalStateException("DB not connected");
        }
    }

    /**
     * Insert a row to the table
     * @param row the row to insert
     * @return the {@link T} object with auto-incremented fields
     */
    public T insert(T row) {
        return db.insert(this, row);
    }

    protected T setFieldFromResultSet(T instance, ResultSet rs, Field f, String columnName) {
        Column column = FieldCache.getAnnotation(f, Column.class);
        Object value;
        try {
            value = rs.getObject(columnName, f.getType());
            if (value == null) {
                value = rs.getObject(columnName);
            }

            if (value != null && ReflectionUtil.isWrapperClass(f.getType()) && ReflectionUtil.isPrimitiveClass(value.getClass())) {
                value = ReflectionUtil.primitiveToWrapper(value);
            } else if (value != null && ReflectionUtil.isWrapperClass(value.getClass()) && ReflectionUtil.isPrimitiveClass(f.getType())) {
                value = ReflectionUtil.wrapperToPrimitive(value);
            }

            if (value != null && f.getType().isAssignableFrom(value.getClass())) {
                value = f.getType().cast(value);
            }

            if (value instanceof Long && !f.getType().isAssignableFrom(Long.class)) {
                if (f.getType().isAssignableFrom(Byte.class)) {
                    value = ((Number) value).byteValue();
                } else if (f.getType().isAssignableFrom(Short.class)) {
                    value = ((Number) value).shortValue();
                } else if (f.getType().isAssignableFrom(Integer.class)) {
                    value = ((Number) value).intValue();
                }
            } else if (value instanceof Double && !f.getType().isAssignableFrom(Double.class)) {
                if (f.getType().isAssignableFrom(Float.class)) {
                    value = ((Number) value).floatValue();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if ((f.getType() == boolean.class || f.getType() == Boolean.class) && ReflectionUtil.isNumber(value.getClass())
            && value != (Object) false && value != (Object) true && value != Boolean.TRUE
            && value != Boolean.FALSE) {
            value = value != (Object) 0; // for some reason this is returned as int on TINYINT(1)..
        }

        if (value == null && (ReflectionUtil.isPrimitiveClass(f.getType()) || (FieldCache.getAnnotation(f, Nullable.class) == null && !column
                .autoIncrement()))) {
            System.out.println(
                    "Trying to set null value on a not nullable and not autoincrement column: " + getRowClass().getName() + "." + f
                            .getName());
        }

        try {
            f.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return instance;
    }

    protected T setFieldsFromResultSet(T instance, ResultSet rs) {
        List<Field> fields = ReflectionUtil.getAllFields(getRowClass());
        for (Field f : fields) {
            Column column = FieldCache.getAnnotation(f, Column.class);
            if (column == null) {
                continue;
            }
            Object value = null;
            try {
                String name = StringUtil.isEmptyOrNull(column.name()) ? f.getName() : column.name();
                if (!hasColumn(rs, name)) {
                    //Select query may not include this column
                    continue;
                }
                setFieldFromResultSet(instance, rs, f, name);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Couldn't set value \"" + (value == null ? "null" : value.toString()) + "\" for field: " + getRowClass().getName()
                        + "." + f.getName() + ": ", e);
            }
        }
        return instance;
    }

    protected List<T> deserializeResultSet(ResultSet r) {
        List<T> result = new ArrayList<>();
        Constructor<?> ctor;
        Object instance;
        try {
            ctor = getRowClass().getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Invalid row class: " + getRowClass().getName() + ": Constructor shouldn't accept arguments!");
        }
        try {
            while (r.next()) {
                try {
                    instance = ctor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Deserializing failed: ", e);
                }

                setFieldsFromResultSet((T) instance, r);

                result.add((T) instance);
            }
        } catch (SQLException e) {
            throw new RuntimeException("An error occurred while deserializing " + getRowClass().getName() + ": ", e);
        }
        return result;
    }

    /**
     * @return the {@link Class}&lt;{@link T}&gt; representation of {@link T}
     */
    public Class<T> getRowClass() {
        Object superclass = getClass().getGenericSuperclass();
        if (superclass instanceof Class) {
            return (Class<T>) superclass;
        }
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        Type type = genericSuperclass.getActualTypeArguments()[0];
        if (type instanceof Class) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        }
        throw new IllegalStateException("Unknown type: " + type.getTypeName());
    }

    /**
     * Execute a native query without auto deserialisation<br/>
     * @param sql the sql query, <code>{TABLE}</code> will be replaced with {@link #getName()}
     * @param bindings the {@link PreparedStatement} bindings
     * @return the {@link ResultSet} of the query
     * @deprecated Use {@link #get(String, Object...)} instead
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public ResultSet executeQuery(String sql, @Nullable Object... bindings) {
        sql = sql.replaceAll("\\Q{TABLE}\\E", getName());
        try {
            validateConnection();
            PreparedStatement statement = db.getConnection().prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                              ResultSet.CONCUR_UPDATABLE);
            parseBindings(statement, bindings);
            ResultSet rs;
            try {
                rs = statement.executeQuery();
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SQLNonTransientConnectionException && !reconnected) {
                    try {
                        statement.close();
                        db.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    db.connect();
                    reconnected = true;
                    return executeQuery(sql, bindings);
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
            reconnected = false;
            return rs;
        } catch (SQLException e) {
            System.out.println("Couldn't execute SQL query: " + sqlToString(sql, bindings));
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement createPreparedStatement(String sql, @Nullable Object... bindings) {
        return createPreparedStatement(sql, null, bindings);
    }

    public PreparedStatement createPreparedStatement(String sql, Integer flags, @Nullable Object... bindings) {
        validateConnection();
        sql = sql.replaceAll("\\Q{TABLE}\\E", getName());
        try {
            PreparedStatement statement;
            if (flags != null) {
                statement = db.getConnection().prepareStatement(sql, flags);
            } else {
                statement = db.getConnection().prepareStatement(sql);
            }

            parseBindings(statement, bindings);
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseBindings(PreparedStatement statement, Object... bindings) throws SQLException {
        if (bindings != null) {
            int i = 1;
            for (Object s : bindings) {
                statement.setObject(i, s);
                i++;
            }
        }
    }

    /**
     * Executes a plain SQL update statement with given bindings<br/>
     * @param sql the sql query, <code>{TABLE}</code> will be replaced with {@link #getName()}
     * @param bindings the {@link PreparedStatement} bindings
     * @deprecated Use the {@link Query} API with {@link Query#execute(Object...)})}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void executeUpdate(String sql, @Nullable Object... bindings) {
        validateConnection();
        try {
            PreparedStatement statement = createPreparedStatement(sql, bindings);
            try {
                statement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SQLNonTransientConnectionException && !reconnected) {
                    try {
                        statement.close();
                        db.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    db.connect();
                    reconnected = true;
                    executeUpdate(sql, bindings);
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            System.out.println("Couldn't execute SQL update statement: " + sqlToString(sql, bindings));
            throw new RuntimeException(e);
        }
        reconnected = false;
    }


    protected String sqlToString(String sql, Object... paramObjects) {
        if (sql == null || paramObjects == null || paramObjects.length < 1) {
            return sql;
        }

        for (Object paramObject : paramObjects) {
            sql = sql.replaceFirst("\\Q?\\E", paramObject == null ? "NULL" : paramObject.toString());
        }

        return sql;
    }

    /**
     * @return true if the table exists
     */
    public boolean exists() {
        validateConnection();
        try {
            DatabaseMetaData dbm = db.getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, name, null);
            return tables.next();
        } catch (Exception e) {
            return false;
        }
    }
}
