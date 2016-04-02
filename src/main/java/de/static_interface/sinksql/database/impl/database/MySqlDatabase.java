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

package de.static_interface.sinksql.database.impl.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.static_interface.sinksql.database.DatabaseConnectionInfo;
import de.static_interface.sinksql.database.SqlDatabase;

import java.sql.SQLException;

/**
 * MySQL database implementation<br/>
 * See <a href="https://www.mysql.com/">MySQL homepage</a> for more information about MySQL
 */
public class MySqlDatabase extends SqlDatabase {

    /**
     * @param info the connection info
     */
    public MySqlDatabase(DatabaseConnectionInfo info) {
        super(info, '`');
    }

    @Override
    protected void setupConfig() {
        HikariConfig hConfig = new HikariConfig();
        hConfig.setMaximumPoolSize(10);
        /*
                We use the MariaDB driver for MySQL connections, because bukkit itself and some other plugins use older versions of the MySQL JDBC driver
            which result in MethodNotFoundExceptions or AbstractMethodErrors. Updating the driver itself is hard and may not work on all servers, so we use this simple solution
            to get the latest and up-to-date driver.
                There should be no problems, since MariaDB is a compatible fork of MySQL

                Description on the driver homepage: "MariaDB Connector/J is a Type 4 JDBC driver. It was developed specifically as a lightweight JDBC connector
            for use with MySQL and MariaDB database servers. It's originally based on the Drizzle JDBC code, and with a lot of additions and bug fixes."
         */
        hConfig.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
        hConfig.addDataSourceProperty("serverName", getConnectionInfo().getAddress());
        hConfig.addDataSourceProperty("port", getConnectionInfo().getPort());
        hConfig.addDataSourceProperty("databaseName", getConnectionInfo().getDatabaseName());
        hConfig.addDataSourceProperty("user", getConnectionInfo().getUsername());
        hConfig.addDataSourceProperty("password", getConnectionInfo().getPassword());
        hConfig.setConnectionTimeout(5000);
        dataSource = new HikariDataSource(hConfig);
    }

    @Override
    public void connect() throws SQLException {
        setupConfig();
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            dataSource.close();
            throw e;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }

        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    protected boolean supportsEngines() {
        return true;
    }
}
