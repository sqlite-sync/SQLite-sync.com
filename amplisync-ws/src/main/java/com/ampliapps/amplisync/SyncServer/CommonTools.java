package com.ampliapps.amplisync.SyncServer;

import com.ampliapps.amplisync.SyncServer.Synchronization.Database;
import com.ampliapps.amplisync.SyncServer.Synchronization.DatabaseTable;
import com.ampliapps.amplisync.SyncServer.Synchronization.DatabaseTableColumn;
import com.ampliapps.amplisync.JDBCCloser;
import com.ampliapps.amplisync.Logs;
import com.ampliapps.amplisync.SyncServer.Synchronization.DatabaseTableGuavaCacheUtil;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

public class CommonTools extends Database {
    public CommonTools() {

    }

    public List<String> GetTableList() {
        List<String> tablesList = new ArrayList<>();
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            Statement tableToPublish = cn.createStatement();
            ResultSet reader = tableToPublish.executeQuery(QUERIES.TABLES_LIST());
            while (reader.next()) {
                String tableName = reader.getString("TABLE_NAME");
                if (!reader.wasNull() && !tableName.isEmpty()) {
                    tablesList.add(tableName);
                }
            }
            reader.close();

            return tablesList;
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "GetTableList() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
            return tablesList;
        }
    }

    public Boolean IsMergeTablesToSyncExists() {
        List<String> tablesList = GetTableList();
        for (String tb : tablesList)
            if (tb.equalsIgnoreCase("MergeTablesToSync"))
                return true;

        return false;
    }

    public Boolean IsTableAddedToSynchronization(String tableName) {
        if (!IsMergeTablesToSyncExists())
            return false;

        Connection cn = Database.getInstance().GetDBConnection();
        try {
            String query = QUERIES.DO_SYNC_GET_TABLE();

            PreparedStatement tableToPublish = cn.prepareStatement(query);

            String[] tmp = tableName.split(Pattern.quote("."));
            String schema = GetDefaultTableSchema();
            String name = tableName;
            if (tmp.length > 1) {
                schema = tableName.replace("." + tmp[tmp.length - 1], "");
                name = tmp[tmp.length - 1];
            }

            tableToPublish.setString(1, name);
            tableToPublish.setString(2, schema);

            ResultSet reader = tableToPublish.executeQuery();
            Boolean tableAlreadyAddedToSynchronization = false;
            if (reader.next())
                tableAlreadyAddedToSynchronization = true;

            reader.close();
            return tableAlreadyAddedToSynchronization;

        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "IsTableAddedToSynchronization() " + e.getMessage());
            return false;
        }
        finally {
            JDBCCloser.close(cn);
        }
    }

    public void CreateSQLiteSynDatabaseObjects() {
        Boolean sqlitesyncObjectsAreThere = false;
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            Statement tableToPublish = cn.createStatement();
            ResultSet reader = tableToPublish.executeQuery(QUERIES.TABLES_LIST());
            while (reader.next())
                if (reader.getString("TABLE_NAME").endsWith("MergeTablesToSync"))
                    sqlitesyncObjectsAreThere = true;

            reader.close();

            if (!sqlitesyncObjectsAreThere) {

                String query = "";
                String queryResource = "";

                queryResource = "/MYSQL_CREATE_SCRIPT.sql";
                InputStream in = getClass().getResourceAsStream(queryResource);
                BufferedReader readerResources = new BufferedReader(new InputStreamReader(in));
                query = org.apache.commons.io.IOUtils.toString(readerResources);

                this.ImportSQL(query);

                PreparedStatement insMergeIdent = cn.prepareStatement(QUERIES.INSERT_MERGEIDENT_MERGETABLETOSYNC());
                insMergeIdent.setString(1, GetDefaultTableSchema());
                insMergeIdent.execute();
            }
        } catch (IOException | SQLException e) {
            Logs.write(Logs.Level.ERROR, "CreateSQLiteSynDatabaseObjects() " + e.getMessage());
        }
        finally {
            JDBCCloser.close(cn);
        }
    }

    public void DropSQLiteSynDatabaseObjects() {
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            String query = "";
            String queryResource = "";

                    queryResource = "/MYSQL_DROP_SCRIPT.sql";
            InputStream in = getClass().getResourceAsStream(queryResource);
            BufferedReader readerResources = new BufferedReader(new InputStreamReader(in));
            query = org.apache.commons.io.IOUtils.toString(readerResources);
            this.ImportSQL(query);
        } catch (IOException | SQLException e) {
            Logs.write(Logs.Level.ERROR, "DropSQLiteSynDatabaseObjects() " + e.getMessage());
        }
        finally {
            JDBCCloser.close(cn);
        }
    }

    public Integer CheckIfSubscriberExists(String deviceUUID) {
        Map<Integer, String> subscribers = GetSubscribers();

        if (!subscribers.containsValue(deviceUUID)) {
            SaveSubscriber(null, deviceUUID);
            subscribers = GetSubscribers();
        }

        for (Map.Entry<Integer, String> s : subscribers.entrySet())
            if (s.getValue().toString().equals(deviceUUID))
                return s.getKey();

        return -1;
    }

    public int GetSynchronizedTablesCount() {
        int count = 0;
        if (IsMergeTablesToSyncExists()) {
            Connection cn = Database.getInstance().GetDBConnection();
            try {

                Statement tableToPublish = cn.createStatement();
                ResultSet reader = tableToPublish.executeQuery(QUERIES.GET_MERGE_TABLES_TO_SYNC());
                while (reader.next())
                    if (!reader.getString("TableName").equalsIgnoreCase("MergeIdentity"))
                        count++;

                reader.close();

            } catch (SQLException e) {
                Logs.write(Logs.Level.ERROR, "GetSynchronizedTablesCount() " + e.getMessage());
            }
            finally {
                JDBCCloser.close(cn);
            }
        }
        return count;
    }

    public Map<Integer, String> GetSynchronizedTables() {
        Map<Integer, String> tables = new HashMap<>();
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            Statement tableToPublish = cn.createStatement();
            ResultSet reader = tableToPublish.executeQuery(QUERIES.GET_MERGE_TABLES_TO_SYNC());
            while (reader.next()) {
                String tableNameWithSchema = reader.getString("TableName");
                String tableSchema = reader.getString("TableSchema");
                if (!reader.wasNull() && !tableSchema.isEmpty())
                    tableNameWithSchema = tableSchema + "." + reader.getString("TableName");
                tables.put(reader.getInt("TableId"), tableNameWithSchema);
            }

            reader.close();
            tableToPublish.close();
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "GetSynchronizedTables() " + e.getMessage());
        }
        finally {
            JDBCCloser.close(cn);
        }

        return tables;
    }

    public void AddTableToSynchronization(String tableName) {

        if (GetSynchronizedTablesCount() == 0)
            CreateSQLiteSynDatabaseObjects();

        if (IsTableAddedToSynchronization(tableName)) {
            Logs.write(Logs.Level.INFO, "AddTableToSynchronization() table " + tableName + " already added.");
            return;
        }

        if (tableName.trim().length() > 0) {
            String[] tmp = tableName.split(Pattern.quote("."));
            String schema = GetDefaultTableSchema();
            String name = tableName;
            if (tmp.length > 1) {
                schema = tableName.replace("." + tmp[tmp.length - 1], "");
                name = tmp[tmp.length - 1];
            }
            String primaryKeyColumnName = "";
            DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(name, schema);

            for (DatabaseTableColumn col : table.Columns)
                if (col.IsInPrimaryKey)
                    primaryKeyColumnName = col.Name;

            String queryResource = "";

                    queryResource = "/MYSQL_ADD_TABLE.sql";

            GenerateAndExecuteSchemaChanges(schema, name, primaryKeyColumnName, queryResource);
        }
    }

    public void RemoveTableFromSynchronization(String tableName) {

        if (!IsTableAddedToSynchronization(tableName)) {
            Logs.write(Logs.Level.INFO, "RemoveTableFromSynchronization() table " + tableName + " already removed.");
            return;
        }

        if (tableName.trim().length() > 0) {
            String[] tmp = tableName.split(Pattern.quote("."));
            String schema = GetDefaultTableSchema();
            String name = tableName;
            if (tmp.length > 1) {
                schema = tableName.replace("." + tmp[tmp.length - 1], "");
                name = tmp[tmp.length - 1];
            }
            String primaryKeyColumnName = "";
            DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(name, schema);

            for (DatabaseTableColumn col : table.Columns)
                if (col.IsInPrimaryKey)
                    primaryKeyColumnName = col.Name;

            String queryResource = "";

                    queryResource = "/MYSQL_REMOVE_TABLE.sql";

            GenerateAndExecuteSchemaChanges(schema, name, primaryKeyColumnName, queryResource);
        }

        Logs.write(Logs.Level.INFO, "Table " + tableName + " removed.");

        if (GetSynchronizedTablesCount() == 0)
            DropSQLiteSynDatabaseObjects();
    }

    private void GenerateAndExecuteSchemaChanges(String schema, String name, String primaryKeyColumnName, String queryResource) {
        String query = "";
        String queries = "";
        InputStream in = getClass().getResourceAsStream(queryResource);
        BufferedReader readerResources = new BufferedReader(new InputStreamReader(in));

        try {
            query = org.apache.commons.io.IOUtils.toString(readerResources);
        } catch (IOException e) {
            Logs.write(Logs.Level.ERROR, "GenerateAndExecuteSchemaChanges() " + e.getMessage());
        }

                queries = query.replace("{$SCHEMA_TABLE}", schema).replace("{$TABLE_NAME}", name).replace("{$TABLE_PK}", primaryKeyColumnName);


        Connection cn = Database.getInstance().GetDBConnection();
        try {
            this.ImportSQL(queries);
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "GenerateAndExecuteSchemaChanges() " + e.getMessage());
        }
        finally {
            JDBCCloser.close(cn);
        }

    }

    public Map<Integer, String> GetSubscribers() {
        Map<Integer, String> subscibers = new HashMap<>();

        if (IsMergeTablesToSyncExists()) {
            Connection cn = Database.getInstance().GetDBConnection();
            try {
                Statement tableToPublish = cn.createStatement();
                ResultSet reader = tableToPublish.executeQuery(QUERIES.GET_SUBSCRIBERS_LIST());
                while (reader.next()) {
                    subscibers.put(reader.getInt("SubscriberId"), reader.getString("Name"));
                }
                reader.close();
            } catch (SQLException e) {
                Logs.write(Logs.Level.ERROR, "GetSubscribers() " + e.getMessage());
            }
            finally {
                JDBCCloser.close(cn);
            }
        }

        return subscibers;
    }

    public void SaveSubscriber(String SubscriberId, String name) {
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            String query = "";

            if (SubscriberId == null || SubscriberId.isEmpty())
                query = QUERIES.INSERT_SUBSCRIBER();
            else
                query = QUERIES.UPDATE_SUBSCRIBER();

            PreparedStatement cmd = cn.prepareStatement(query);

            cmd.setString(1, name);
            cmd.setString(2, name);
            if (SubscriberId !=  null && !SubscriberId.isEmpty())
                cmd.setInt(3, Integer.parseInt(SubscriberId));

            cmd.execute();
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "SaveSubscriber() " + e.getMessage());
        }
        finally {
            JDBCCloser.close(cn);
        }
    }

    public void CheckIdentiesRange(String tableName, String subscriberId) {

        return;
        //todo: migrate function from .net version
    }

    private void ImportSQL(String in) throws SQLException {
        Connection conn = Database.getInstance().GetDBConnection();
        try {
            String[] scanner = in.split("KOL_SC");
            Statement currentStatement = null;
            for (String query : scanner) {

                String rawStatement = query;
                try {
                    currentStatement = conn.createStatement();
                    currentStatement.execute(rawStatement);
                } catch (SQLException e) {
                    Logs.write(Logs.Level.ERROR, "ImportSQL() " + e.getMessage());
                } finally {

                    if (currentStatement != null) {
                        try {
                            currentStatement.close();
                        } catch (SQLException e) {
                            Logs.write(Logs.Level.ERROR, "ImportSQL()->currentStatement.close()" + e.getMessage());
                        }
                    }
                    currentStatement = null;
                }
            }
        }
        finally {
            JDBCCloser.close(conn);
        }
    }

    public static void DeleteFilesOlderThanNdays(long days, String fileExtension, String dirPath) {

        File folder = new File(dirPath);

        if (folder.exists()) {
            File[] listFiles = folder.listFiles();
            long eligibleForDeletion = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000);
            for (File listFile : listFiles) {
                if (listFile.getName().endsWith(fileExtension) &&
                        listFile.lastModified() < eligibleForDeletion) {
                    if (!listFile.delete()) {
                        Logs.write(Logs.Level.ERROR, "DeleteFilesOlderThanNdays: Sorry Unable to Delete Files..");
                    }
                }
            }
        }
    }

    public Boolean CheckIfDBConnectionIsOK(){
        Connection cn = Database.getInstance().GetDBConnection();
        if(cn == null)
            return false;
        else
            return true;
    }

    public String GetVersionOfSQLiteSyncCOM() {
        return SQLiteSyncVersion;
    }
}
