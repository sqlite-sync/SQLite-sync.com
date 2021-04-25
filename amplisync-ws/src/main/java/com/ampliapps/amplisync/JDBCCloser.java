package com.ampliapps.amplisync;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*************************************************************************
 *
 * CONFIDENTIAL
 * __________________
 *
 *  AMPLIFIER sp. z o.o.
 *  www.ampliapps.com
 *  support@ampliapps.com
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AMPLIFIER sp. z o.o. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to AMPLIFIER sp. z o.o.
 * and its suppliers and may be covered by U.S., European and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AMPLIFIER sp. z o.o..
 **************************************************************************/

public class JDBCCloser {
    /**
     * Closes the provided Connections
     *
     * @param connections
     *            Connections that should be closed
     */
    public static void close(Connection... connections) {

        if (connections == null)
            return;

        for (Connection connection : connections)
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    Logs.write(Logs.Level.ERROR, "JDBCCloser->close() " + e.getMessage());
                }
    }
}
