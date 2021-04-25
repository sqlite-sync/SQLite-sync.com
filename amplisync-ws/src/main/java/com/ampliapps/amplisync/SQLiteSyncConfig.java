package com.ampliapps.amplisync;

import org.apache.logging.log4j.LogManager;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.IOException;
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

public class SQLiteSyncConfig {

    public static Boolean IsConfigLoaded = false;
    public static String DBURL = "jdbc:mysql://127.0.0.1:3306/sqlitesync?rewriteBatchedStatements=true";
    public static String DBUSER = "root";
    public static String DBPASS = "pass";
    public static String DBDRIVER = "com.mysql.cj.jdbc.Driver";
    public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static Integer HISTORY_DAYS = 7;
    public static String WORKING_DIR = "../working dir/";
    public static Integer LOG_LEVEL = 5;
    public static String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ssZZ";

    public static void Load() {

        if(SQLiteSyncConfig.IsConfigLoaded)
            return;

        try {
            Context ctx = new InitialContext();
            Context env = (Context) ctx.lookup("java:comp/env");
            String workingDir = (String) env.lookup("working-dir");
            if(!workingDir.isEmpty())
                WORKING_DIR = workingDir;
        } catch (NamingException e) {
            System.out.println(e.getMessage());
        }

        System.setProperty("log4j.SQLiteWorkingDir", WORKING_DIR);
        org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(WORKING_DIR + "config/sync.properties"));

            DBURL = properties.getProperty("DBURL");
            DBUSER = properties.getProperty("DBUSER");
            DBPASS = properties.getProperty("DBPASS");
            DBDRIVER = properties.getProperty("DBDRIVER");
            DATE_FORMAT = properties.getProperty("DATE_FORMAT");
            TIMESTAMP_FORMAT = properties.getProperty("TIMESTAMP_FORMAT");
            HISTORY_DAYS = Integer.parseInt(properties.getProperty("HISTORY_DAYS"));
            LOG_LEVEL = Integer.parseInt(properties.getProperty("LOG_LEVEL"));

            SQLiteSyncConfig.IsConfigLoaded = true;

            Logs.write(Logs.Level.INFO, "Working dir is set to " + WORKING_DIR);
            Logs.write(Logs.Level.INFO, "Database type is set to MySQL");

        } catch (IOException e) {
            Logs.write(Logs.Level.ERROR, "SQLiteSyncConfig->Load() " + e.getMessage());
        }
    }
}
