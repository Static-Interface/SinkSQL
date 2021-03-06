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

public interface DatabaseConnectionInfo {

    /**
     * @return the address to connect to
     */
    String getAddress();

    /**
     * @return the port of the connection
     */
    int getPort();

    /**
     * @return the username for authentification
     */
    String getUsername();

    /**
     * @return the password for authentification
     */
    String getPassword();

    /**
     * @return the prefix for the tables
     */
    String getTablePrefix();

    /**
     * @return the name of the database
     */
    String getDatabaseName();
}
