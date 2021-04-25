package com.ampliapps.amplisync.SyncServer.Synchronization;

import com.ampliapps.amplisync.Logs;
import com.ampliapps.amplisync.SQLiteSyncConfig;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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

public class Database {
    public static String SQLiteSyncVersion = "";

    public SQLQueries QUERIES = new SQLQueries();

    private static Database datasource;
    private ComboPooledDataSource cpds;

    public Database() {

        String version = "";
        try {
            String resourceName = "project.properties";
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Properties props = new Properties();
            try(InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
                props.load(resourceStream);
            }
            version = props.getProperty("version");
        } catch (IOException ex){
            Logs.write(Logs.Level.ERROR, "GetVersionOfSQLiteSyncCOM: " + ex.getMessage());
        }
        SQLiteSyncVersion = version;

        SQLiteSyncConfig.Load();

        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass(SQLiteSyncConfig.DBDRIVER); //loads the jdbc driver
            cpds.setJdbcUrl(SQLiteSyncConfig.DBURL);
            cpds.setUser(SQLiteSyncConfig.DBUSER);
            cpds.setPassword(SQLiteSyncConfig.DBPASS);

            // the settings below are optional -- c3p0 can work with defaults
            cpds.setMinPoolSize(3);
            cpds.setAcquireIncrement(3);
            cpds.setMaxPoolSize(10);
            cpds.setIdleConnectionTestPeriod(300);
            cpds.setMaxIdleTime(240);
            cpds.setTestConnectionOnCheckin(false);
            cpds.setMaxStatements(2000);
            cpds.setMaxStatementsPerConnection(100);

        } catch (PropertyVetoException e){
            Logs.write(Logs.Level.ERROR, "Database constructor: " + e.getMessage());
        }
    }

    public static Database getInstance() {
        if (datasource == null) {
            datasource = new Database();
            return datasource;
        } else {
            return datasource;
        }
    }

    public Connection GetDBConnection() {
        try {
            return this.cpds.getConnection();
        } catch (SQLException e){
            Logs.write(Logs.Level.ERROR, "GetDBConnection() " + e.getMessage());
            Connection conn = null;
            try {
                Class.forName(SQLiteSyncConfig.DBDRIVER).newInstance();
                conn = DriverManager.getConnection(SQLiteSyncConfig.DBURL, SQLiteSyncConfig.DBUSER, SQLiteSyncConfig.DBPASS);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException ex) {
                Logs.write(Logs.Level.ERROR, "GetDBConnection() " + ex.getMessage());
            }

            return conn;
        }
    }

    protected String GetDefaultTableSchema() {
        return "";
    }
}
