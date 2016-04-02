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

package de.static_interface.sinksql;

import de.static_interface.sinksql.annotation.ForeignKey;
import de.static_interface.sinksql.annotation.UniqueKey;
import de.static_interface.sinksql.annotation.Column;
import de.static_interface.sinksql.annotation.Index;
import de.static_interface.sinksql.exception.InvalidSqlColumnException;
import de.static_interface.sinksql.impl.table.OptionsTable;
import de.static_interface.sinksql.query.Query;
import de.static_interface.sinksql.query.condition.EqualsCondition;
import de.static_interface.sinksql.query.condition.GreaterThanCondition;
import de.static_interface.sinksql.query.condition.GreaterThanEqualsCondition;
import de.static_interface.sinksql.query.condition.LikeCondition;
import de.static_interface.sinksql.query.condition.WhereCondition;
import de.static_interface.sinksql.query.impl.AndQuery;
import de.static_interface.sinksql.query.impl.DeleteQuery;
import de.static_interface.sinksql.query.impl.FromQuery;
import de.static_interface.sinksql.query.impl.LimitQuery;
import de.static_interface.sinksql.query.impl.OrQuery;
import de.static_interface.sinksql.query.impl.OrderByQuery;
import de.static_interface.sinksql.query.impl.SelectQuery;
import de.static_interface.sinksql.query.impl.SetQuery;
import de.static_interface.sinksql.query.impl.UpdateQuery;
import de.static_interface.sinksql.query.impl.WhereQuery;
import de.static_interface.sinksql.util.ReflectionUtil;
import de.static_interface.sinksql.util.StringUtil;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public abstract class SqlDatabase extends Database {

    private final char backtick;
    int queryType = 0;
    int selectQuery = 1;
    int updateQuery = 2;
    int deleteQuery = 3;
    private boolean firstSetCall = true;

    /**
     *  @param info the connection info
     * @param backtick the backtick used by the database sql synrax
     */
    public SqlDatabase(@Nullable DatabaseConnectionInfo info, char backtick) {
        super(info);
        this.backtick = backtick;
    }

    @Override
    public String toDatabaseType(Field f) {
        Class clazz = f.getType();
        Column column = FieldCache.getAnnotation(f, Column.class);
        String keyLength = "";
        if (column.keyLength() >= 0) {
            keyLength = "(" + column.keyLength() + ")";
        }

        boolean isKey = column.primaryKey()
                        || column.uniqueKey()
                        || FieldCache.getAnnotation(f, ForeignKey.class) != null
                        || FieldCache.getAnnotation(f, UniqueKey.class) != null;

        if (clazz == Date.class) {
            throw new RuntimeException("Date is not supported for now !");
        }
        if (clazz == java.sql.Date.class) {
            throw new RuntimeException("Date is not supported for now!");
        }
        if (clazz == Integer.class || clazz == int.class) {
            return "INT" + keyLength;
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return keyLength.equals("") ? "TINYINT(1)" : "TINYINT" + keyLength;
        }
        if (clazz == Double.class || clazz == double.class) {
            return "DOUBLE" + keyLength;
        }
        if (clazz == Float.class || clazz == float.class) {
            return "FLOAT" + keyLength;
        }
        if (clazz == Long.class || clazz == long.class) {
            return "BIGINT" + keyLength;
        }
        if (clazz == Short.class || clazz == short.class) {
            return "SMALLINT" + keyLength;
        }
        if (clazz == Byte.class || clazz == byte.class) {
            return "TINYINT" + keyLength;
        }
        if (clazz == String.class) {
            if (keyLength.equals("")) {
                return isKey ? "VARCHAR(255)" : "VARCHAR(999)";
            } else {
                return "VARCHAR" + keyLength;
            }
        }
        throw new RuntimeException("No database type available for: " + clazz.getName());
    }

    /**
     * @return The backtick used by the database sql syntax
     */
    public char getBacktick() {
        return backtick;
    }

    @Override
    public String parseQuery(Query tQuery) {
        String sql = "";
        while (tQuery != null) {
            sql += toSql(tQuery);
            tQuery = tQuery.getChild();
        }
        queryType = 0;
        firstSetCall = true;
        return sql.trim();
    }

    protected String toSql(Query tQuery) {
        String s = handleQuery(tQuery);
        if (!StringUtil.isEmptyOrNull(s)) {
            return s;
        }

        char bt = getBacktick();
        if (tQuery instanceof FromQuery) {
            return "";
        }

        if (tQuery instanceof SelectQuery) {
            queryType = selectQuery;
            validateColumnNames(tQuery, ((SelectQuery) tQuery).getColumns());
            return "SELECT " + StringUtil.formatArrayToString(((SelectQuery) tQuery).getColumns(), ",") + " FROM " + bt + "{TABLE}" + bt + " ";
        }

        if (tQuery instanceof UpdateQuery) {
            queryType = updateQuery;
            return "UPDATE " + bt + "{TABLE}" + bt + " ";
        }

        if (tQuery instanceof DeleteQuery) {
            queryType = deleteQuery;
            return "DELETE FROM " + bt + "{TABLE}" + bt + " ";
        }

        if (tQuery instanceof SetQuery) {
            if (queryType != updateQuery) {
                throw new IllegalStateException("Can only use SET statements on UPDATE queries!");
            }
            String columnName = ((SetQuery) tQuery).getColumn();
            validateColumnNames(tQuery, columnName);
            String value = tQuery.getTable().toSqlValue(((SetQuery) tQuery).getValue());

            String setStatement = bt + columnName + bt + "=" + value + " ";
            if (!firstSetCall) {
                setStatement = ", " + setStatement;
            } else {
                setStatement = "SET " + setStatement;
                firstSetCall = false;
            }
            return setStatement;
        }

        if (tQuery instanceof AndQuery) {
            return "AND " + whereStatementToSql((WhereQuery) tQuery) + " ";
        }

        if (tQuery instanceof OrQuery) {
            return "OR " + whereStatementToSql((WhereQuery) tQuery) + " ";
        }

        if (tQuery instanceof WhereQuery) {
            return "WHERE " + whereStatementToSql((WhereQuery) tQuery) + " ";
        }

        if (tQuery instanceof OrderByQuery) {
            String columnName = ((OrderByQuery) tQuery).getColumn();
            validateColumnNames(tQuery, columnName);
            String order = ((OrderByQuery) tQuery).getOrder().name().toUpperCase();
            return "ORDER BY " + bt + columnName + bt + " " + order + " ";
        }

        if (tQuery instanceof LimitQuery) {
            return "LIMIT " + ((LimitQuery) tQuery).getOffset() + "," + ((LimitQuery) tQuery).getRowCount() + " ";
        }

        throw new IllegalStateException("Query not supported: " + tQuery.getClass().getName());
    }

    protected String handleQuery(Query query) {
        //easier integration for 3rd party extensions
        return null;
    }

    private void validateColumnNames(Query query, String... columns) {
        if (true) {
            return; // not stable
        }
        if (query.isColumnVerificationDisabled()) {
            return;
        }

        if (columns.length < 1) {
            return;
        }

        AbstractTable table = query.getTable();
        Class<Row> rowClass = table.getRowClass();

        List<String> rowColumns = new ArrayList<>();
        for (Field f : ReflectionUtil.getAllFields(rowClass)) {
            if (f == null) {
                continue;
            }
            String name = f.getName();
            Column c = FieldCache.getAnnotation(f, Column.class);
            if (!StringUtil.isEmptyOrNull(c.name())) {
                name = c.name();
            }
            rowColumns.add(name.trim());
        }

        boolean isInvalid = false;
        for (String s : columns) {
            if (s.equals("*")) {
                continue;
            }
            if (StringUtil.isEmptyOrNull(s)) {
                isInvalid = true;
            }

            if (s.contains("'")) {
                isInvalid = true;
            }

            if (s.contains("'")) {
                isInvalid = true;
            }

            if (isInvalid) {
                throw new IllegalArgumentException("Column \"" + s + "\" has an illegal name in query \"" + query.getClass().getSimpleName() + "\"");
            }

            if (!rowColumns.contains(s.trim())) {
                throw new IllegalStateException(
                        "Column \"" + s + "\"" + " not found in table \"" + table.getName() + "\"" + " in query " + query.getClass().getSimpleName()
                        + "\"");
            }
        }
    }

    protected String whereStatementToSql(WhereQuery tQuery) {
        WhereCondition condition = tQuery.getCondition();
        String prefix = "";
        String suffix = "";
        if (tQuery.getParanthesisState() == 1) {
            prefix = "(";
        }

        if (tQuery.getParanthesisState() == 2) {
            suffix = ")";
        }

        char bt = getBacktick();
        String columName = bt + tQuery.getColumn() + bt;
        validateColumnNames(tQuery, columName);

        if (condition instanceof GreaterThanCondition) {
            String operator = "";
            boolean isInverted = ((GreaterThanCondition) condition).isInverted();
            boolean isNegated = condition.isNegated();
            if (isInverted && !isNegated || !isInverted && condition.isNegated()) {
                operator = "<";
            } else if (isInverted && isNegated) {
                operator = ">";
            } else if (!isInverted && !isNegated) {
                operator = ">";
            }

            boolean isEquals = condition instanceof GreaterThanEqualsCondition;
            if ((isEquals && !isNegated) || (!isEquals && condition.isNegated())) {
                operator += "=";
            }

            String value = tQuery.getTable().toSqlValue(condition.getValue(), false);

            return prefix + columName + " " + operator + " " + value + suffix;
        }

        if (condition instanceof EqualsCondition) {
            String equalsOperator = "=";
            if (condition.isNegated()) {
                equalsOperator = "!=";
            }

            Object o = condition.getValue();
            if (o == null) {
                equalsOperator = "IS";
                if (condition.isNegated()) {
                    equalsOperator = "IS NOT";
                }
            }

            return prefix + columName + " " + equalsOperator + " " + (o == null ? "NULL" : o.toString()) + suffix;
        }

        if (condition instanceof LikeCondition) {
            String likeOperator = "LIKE";
            if (condition.isNegated()) {
                likeOperator = "NOT LIKE";
            }

            return prefix + columName + " " + likeOperator + ((LikeCondition) condition).getPattern() + suffix;
        }

        throw new IllegalStateException("Condition not supported: " + condition.getClass().getName());
    }

    @Override
    public String stringify(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("['\"\\\\]", "\\\\$0");
        return "\"" + s + "\"";
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T extends Row> void createTable(AbstractTable<T> abstractTable) {
        char bt = getBacktick();
        String sql = "CREATE TABLE IF NOT EXISTS " + bt + abstractTable.getName() + bt + " (";

        List<String> primaryKeys = new ArrayList<>();
        List<String> uniqueKeys = new ArrayList<>();
        List<Field> foreignKeys = new ArrayList<>();
        List<Field> indexes = new ArrayList<>();
        HashMap<Integer, List<String>> combinedUniqueKeys = new HashMap<>();

        Class foreignOptionsTable = null;

        if (abstractTable instanceof OptionsTable)

        {
            foreignOptionsTable = ((OptionsTable) abstractTable).getForeignTable();
        }

        for (
                Field f
                :

                abstractTable.getRowClass()

                        .

                                getFields()

                )

        {
            Column column = FieldCache.getAnnotation(f, Column.class);
            if (column == null) {
                continue;
            }
            String name = StringUtil.isEmptyOrNull(column.name()) ? f.getName() : column.name();

            sql += bt + name + bt + " " + toDatabaseType(f);

            if (column.zerofill()) {
                if (!ReflectionUtil.isNumber(f.getType())) {
                    throw new InvalidSqlColumnException(abstractTable, f, name, "column was annotated as ZEROFILL but wrapper type is not a number");
                }
                sql += " ZEROFILL";
            }

            if (column.unsigned()) {
                if (!ReflectionUtil.isNumber(f.getType())) {
                    throw new InvalidSqlColumnException(abstractTable, f, name,
                                                        "column was annotated as UNSIGNED but wrapper type is not a number");
                }
                sql += " UNSIGNED";
            }

            if (column.autoIncrement()) {
                if (!ReflectionUtil.isNumber(f.getType())) {
                    throw new InvalidSqlColumnException(abstractTable, f, name,
                                                        "column was annotated as AUTO_INCREMENT but wrapper type is not a number");
                }
                sql += " AUTO_INCREMENT";
            }

            if (column.uniqueKey()) {
                uniqueKeys.add(name);
            }

            UniqueKey uniqueKey = FieldCache.getAnnotation(f, UniqueKey.class);
            if (uniqueKey != null) {
                if (uniqueKey.combinationId() == Integer.MAX_VALUE) {
                    uniqueKeys.add(name);
                } else {
                    List<String> keys = combinedUniqueKeys.get(uniqueKey.combinationId());
                    if (keys == null) {
                        keys = new ArrayList<>();
                    }
                    keys.add(name);
                    combinedUniqueKeys.put(uniqueKey.combinationId(), keys);
                }
            }

            if (column.primaryKey()) {
                primaryKeys.add(name);
            }

            if (FieldCache.getAnnotation(f, Nullable.class) == null) {
                sql += " NOT NULL";
            } else if (ReflectionUtil.isPrimitiveClass(f.getType())) {
                // The column is nullable but the wrapper type is a primitive value, which can't be null
                throw new InvalidSqlColumnException(abstractTable, f, name,
                                                    "column was annotated as NULLABLE but wrapper type is a primitive type");
            }

            if (!StringUtil.isEmptyOrNull(column.defaultValue())) {
                sql += " DEFAULT " + column.defaultValue();
            }

            if (!StringUtil.isEmptyOrNull(column.comment())) {
                sql += " COMMENT '" + column.comment() + "'";
            }

            if (FieldCache.getAnnotation(f, ForeignKey.class) != null) {
                foreignKeys.add(f);
            }

            if (FieldCache.getAnnotation(f, Index.class) != null) {
                indexes.add(f);
            }

            sql += ",";
        }

        if (primaryKeys.size() > 0)

        {
            String columns = "";
            for (String f : primaryKeys) {
                if (!columns.equals("")) {
                    columns += ", ";
                }
                columns += bt + f + bt;
            }
            sql += "PRIMARY KEY (" + columns + "),";
        }

        if (uniqueKeys.size() > 0)

        {
            for (String s : uniqueKeys) {
                sql += "UNIQUE (" + bt + s + bt + "),";
            }
        }

        if (combinedUniqueKeys.size() > 0)

        {
            for (List<String> columnsList : combinedUniqueKeys.values()) {
                String columns = "";
                String first = null;
                for (String f : columnsList) {
                    if (!columns.equals("")) {
                        columns += ", ";
                    }
                    if (first == null) {
                        first = f;
                    }
                    columns += bt + f + bt;
                }
                sql += "UNIQUE KEY " + bt + first + "_uk" + bt + " (" + columns + "),";
            }
        }

        for (
                Field f
                : foreignKeys)

        {
            Column column = FieldCache.getAnnotation(f, Column.class);
            String name = StringUtil.isEmptyOrNull(column.name()) ? f.getName() : column.name();
            ForeignKey foreignKey = FieldCache.getAnnotation(f, ForeignKey.class);

            sql = addForeignKey(sql, name, foreignKey.table(), foreignKey.column(), foreignKey.onUpdate(), foreignKey.onDelete());
        }

        if (foreignOptionsTable != null)

        {
            String column = ((OptionsTable) abstractTable).getForeignColumn();
            CascadeAction onUpdate = ((OptionsTable) abstractTable).getForeignOnUpdateAction();
            CascadeAction onDelete = ((OptionsTable) abstractTable).getForeignOnDeleteAction();
            sql = addForeignKey(sql, "foreignTarget", foreignOptionsTable, column, onUpdate, onDelete);
        }

        for (
                Field f
                : indexes)

        {
            if (abstractTable.getEngine().equalsIgnoreCase("InnoDB") && foreignKeys.contains(f)) {
                continue; //InnoDB already creates indexes for foreign keys, so skip these...
            }

            Column column = FieldCache.getAnnotation(f, Column.class);
            String name = StringUtil.isEmptyOrNull(column.name()) ? f.getName() : column.name();

            Index index = FieldCache.getAnnotation(f, Index.class);
            String indexName = StringUtil.isEmptyOrNull(index.name()) ? name + "_I" : index.name();

            sql += "INDEX " + bt + indexName + bt + " (" + bt + name + bt + ")";

            sql += ",";
        }

        if (sql.endsWith(","))

        {
            sql = sql.substring(0, sql.length() - 1);
        }

        sql += ")";
        if (supportsEngines())

        {
            //Todo: do other SQL databases support engines?
            sql += " ENGINE=" + abstractTable.getEngine();
        }

        sql += ";";

        abstractTable.executeUpdate(sql);
    }

    protected abstract boolean supportsEngines();

    protected String addForeignKey(String sql, String name, Class<? extends AbstractTable> targetClass, String columnName, CascadeAction onUpdate,
                                   CascadeAction onDelete) {
        char bt = getBacktick();

        String tablename;
        try {
            tablename = targetClass.getField("TABLE_NAME").get(null).toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException("Static String Field TABLE_NAME was not declared in table wrapper class " + targetClass.getName() + "!", e);
        }

        sql +=
                "FOREIGN KEY (" + bt + name + bt + ") REFERENCES " + getConnectionInfo().getTablePrefix() + tablename + " (" + bt + columnName
                + bt + ")";
        sql += " ON UPDATE " + onUpdate.toSql() + " ON DELETE " + onDelete.toSql();

        sql += ",";

        return sql;
    }

    @Override
    public <T extends Row> T insert(AbstractTable<T> abstractTable, T row) {
        Validate.notNull(row);
        String columns = "";
        char bt = getBacktick();
        int i = 0;
        List<Field> fields = ReflectionUtil.getAllFields(abstractTable.getRowClass());
        Map<Field, String> autoIncrements = new HashMap<>();
        for (Field f : fields) {
            Column column = FieldCache.getAnnotation(f, Column.class);
            if (column == null) {
                continue;
            }

            String name = StringUtil.isEmptyOrNull(column.name()) ? f.getName() : column.name();

            if (column.autoIncrement()) {
                autoIncrements.put(f, name);
            }

            name = bt + name + bt;
            if (i == 0) {
                columns = name;
                i++;
                continue;
            }
            columns += ", " + name;
            i++;
        }

        if (i == 0) {
            throw new IllegalStateException(abstractTable.getRowClass().getName() + " doesn't have any public fields!");
        }

        String valuesPlaceholders = "";
        for (int k = 0; k < i; k++) {
            if (k == 0) {
                valuesPlaceholders = "?";
                continue;
            }
            valuesPlaceholders += ",?";
        }

        String sql = "INSERT INTO `{TABLE}` (" + columns + ") " + "VALUES(" + valuesPlaceholders + ")";
        List<Object> values = new ArrayList<>();
        for (Field f : fields) {
            try {
                values.add(f.get(row));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        PreparedStatement ps = abstractTable.createPreparedStatement(sql, Statement.RETURN_GENERATED_KEYS, values.toArray(new Object[values.size()]));
        try {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ResultSet rs;
        try {
            rs = ps.getGeneratedKeys();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (Field f : autoIncrements.keySet()) {
            abstractTable.setFieldFromResultSet(row, rs, f, autoIncrements.get(f));
        }

        try {
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return row;
    }
}
