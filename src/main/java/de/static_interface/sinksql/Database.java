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

import com.zaxxer.hikari.HikariDataSource;
import de.static_interface.sinksql.query.Query;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nullable;

public abstract class Database {
    private final DatabaseConnectionInfo info;
    protected HikariDataSource dataSource;
    protected Connection connection;

    /**
     * @param info the connection info
     */
    public Database(@Nullable DatabaseConnectionInfo info) {
        this.info = info;
    }

    /**
     * Get the SQL type of a field
     * @param f the Field to convert
     * @return the SQL type
     * @throws RuntimeException if there is no native SQL type representation
     */
    public abstract String toDatabaseType(Field f);

    protected abstract void setupConfig();

    /**
     * Connect to the database
     * @throws SQLException
     */
    public abstract void connect() throws SQLException;

    /**
     * Close the connection of the database
     * @throws SQLException
     */
    public abstract void close() throws SQLException;

    /**
     * @return the {@link DatabaseConnectionInfo}
     */
    @Nullable
    public DatabaseConnectionInfo getConnectionInfo() {
        return info;
    }

    /**
     * @return the {@link Connection}
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * @param tQuery the query to build
     * @return the parsed query
     */
    public abstract String parseQuery(Query tQuery);

    /**
     * Escapes a string and adds "'s to start and end
     * @param s the string to convert
     * @return the converted string
     */
    public abstract String stringify(String s);

    public abstract <T extends Row> void createTable(AbstractTable<T> abstractTable);

    public abstract <T extends Row> T insert(AbstractTable<T> abstractTable, T row);

    /**
     * @return true if connected to database
     */
    public boolean isConnected() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
