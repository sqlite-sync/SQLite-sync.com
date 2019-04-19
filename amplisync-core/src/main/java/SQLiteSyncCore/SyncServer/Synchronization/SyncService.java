package SQLiteSyncCore.SyncServer.Synchronization;

import SQLiteSyncCore.JDBCCloser;
import SQLiteSyncCore.Logs;
import SQLiteSyncCore.SQLiteSyncConfig;
import SQLiteSyncCore.SyncServer.CommonTools;
import SQLiteSyncCore.SyncServer.Helpers;
import SQLiteSyncCore.SyncServer.SchemaPublish.SchemaGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.rowset.CachedRowSetImpl;
import jersey.repackaged.com.google.common.cache.CacheBuilder;
import jersey.repackaged.com.google.common.cache.CacheLoader;
import jersey.repackaged.com.google.common.cache.LoadingCache;
import org.apache.commons.codec.binary.Base64;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

public class SyncService extends Database {

    List<DataObject> dataToSync = new ArrayList<>();

    private CachedRowSetImpl tablesData = null;
    private PreparedStatement recieveDataStatment = null;
    private static LoadingCache<String, String> tableSchemaCache = null;
    public Integer syncIdForTestPurpose = -1;

    public SyncService() {
        SQLiteSyncConfig.Load();
        PrepareCacheForTableSchemaGetter();

    }

    private void PrepareCacheForTableSchemaGetter() {
        if (tableSchemaCache == null)
            tableSchemaCache = CacheBuilder.newBuilder()
                    .maximumSize(200) // maximum 100 records can be cached
                    .expireAfterAccess(60, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
                    .build(new CacheLoader<String, String>() { // build the cacheloader

                        @Override
                        public String load(String tableName) throws Exception {
                            //make the expensive call
                            String tableSchema = "";
                            Connection cn = Database.getInstance().GetDBConnection();
                            try {
                                PreparedStatement tableToPublish = cn.prepareStatement(QUERIES.DO_SYNC_GET_TABLE_SCHEMA());
                                tableToPublish.setString(1, tableName);

                                ResultSet reader = tableToPublish.executeQuery();
                                while (reader.next()) {
                                    tableSchema = reader.getString("TableSchema");
                                }
                            } catch (SQLException e) {
                                Logs.write(Logs.Level.ERROR, "GetTableSchema() " + e.getMessage());
                            } finally {
                                JDBCCloser.close(cn);
                            }

                            if (tableSchema == null || tableSchema.isEmpty())
                                tableSchema = GetDefaultTableSchema();

                            return tableSchema;
                        }
                    });
    }

    public String DoSync(String subscriberUUID, String table) {

        CommonTools common = new CommonTools();
        String subscriberId = common.CheckIfSubscriberExists(subscriberUUID).toString();

        if (subscriberId.equalsIgnoreCase("-1")) {
            Logs.write(Logs.Level.ERROR, "Error creating new subscriber for UUID " + subscriberUUID);
            return "Error creating new subscriber for UUID " + subscriberUUID;
        }

        Logs.write(Logs.Level.INFO, "Getting changes for subscriber " + subscriberId + " and table " + table);

        Integer tableId = 0;
        String tableSchema = "";
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            String query = QUERIES.DO_SYNC_GET_TABLE();
            PreparedStatement tableToPublish = cn.prepareStatement(query);

            String[] tmp = table.split(Pattern.quote("."));
            String name = table;
            if (tmp.length > 1) {
                name = tmp[tmp.length - 1];
            }
            String schema = GetTableSchema(name);
            tableToPublish.setString(1, name);
            tableToPublish.setString(2, schema);

            ResultSet reader = tableToPublish.executeQuery();
            if (reader.next()) {
                tableId = reader.getInt("TableId");
                tableSchema = reader.getString("TableSchema");

                if (tableSchema == null || tableSchema.isEmpty())
                    tableSchema = GetDefaultTableSchema();

                EnumarateChanges(reader.getString("TableId"), subscriberId, reader.getString("TableName"), tableSchema, reader.getString("TableFilter"));
            } else
                Logs.write(Logs.Level.INFO, "DoSync(). Table " + schema + "." + table + " was not found in MergeTablesToSync.");


            Integer syncId = StartNewSync(subscriberId, tableId);
            this.syncIdForTestPurpose = syncId;

            for (DataObject obj : dataToSync) {
                obj.SyncId = syncId;
                obj.SQLiteSyncVersion = SQLiteSyncVersion;
            }

            if (dataToSync.size() == 0) {
                DataObject emptySync = new DataObject();
                emptySync.SyncId = -1;
                dataToSync.add(emptySync);
            }

        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "DoSync() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        StringWriter stringEmp = new StringWriter();
        try {
            objectMapper.writeValue(stringEmp, dataToSync);
        } catch (IOException ex) {
            Logs.write(Logs.Level.ERROR, "DoSync()->JSON Serialization " + ex.getMessage());
        }
        Logs.write(Logs.Level.TRACE, stringEmp.toString());
        return stringEmp.toString();
    }

    private Integer StartNewSync(String subscriberId, Integer tableId) {

        File theDir = new File(SQLiteSyncConfig.WORKING_DIR + "SyncData");
        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (SecurityException e) {
                Logs.write(Logs.Level.ERROR, "StartNewSync()->Creating folder SyncData " + e.getMessage());
            }
        }

        Integer syncId = SetSyncStartMarker(subscriberId, tableId);

        BinaryWriter binaryWriter = new BinaryWriter();
        binaryWriter.writeToBinary(SQLiteSyncConfig.WORKING_DIR + "SyncData/" + syncId + ".dat", tablesData);

        CommonTools.DeleteFilesOlderThanNdays(SQLiteSyncConfig.HISTORY_DAYS, "dat", SQLiteSyncConfig.WORKING_DIR + "SyncData/");

        return syncId;
    }

    private void EnumarateChanges(String tableId, String subscriberId, String tableName, String tableSchema, String tableFilter) {
        DataObject tableSync = new DataObject();
        Boolean hasRows = false;
        tableSync.TableName = tableName;
        GenerateQueries(tableSync, tableSchema);
        String filterVW = tableSchema + "." + tableName;

        if (tableSchema == null || tableSchema.isEmpty())
            filterVW = tableName;

        String filterVW_CD = " ";

        if (tableName.equalsIgnoreCase("MergeIdentity"))
            filterVW_CD = "and vw.SubscriberId=" + subscriberId + " ";

        StringBuilder query = BuildMergeQuery(tableId, subscriberId, tableName, tableSchema, filterVW, filterVW_CD);

        SchemaGenerator schemaGenerator = new SchemaGenerator();

        Connection cn = Database.getInstance().GetDBConnection();
        try {
            Statement cmd = cn.createStatement();

            Logs.write(Logs.Level.TRACE, query.toString());

            DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

            if (!tableName.equalsIgnoreCase("MergeIdentity")) {
                tableSync.TriggerInsert = schemaGenerator.CreateInsertTrigger(table, tableId, subscriberId);
                tableSync.TriggerInsertDrop = "DROP TRIGGER IF EXISTS \"trMergeInsert_" + tableName + "\"";
                tableSync.TriggerUpdate = schemaGenerator.CreateUpdateTrigger(table, schemaGenerator.GenerateUpdateableColumns(table.Columns));
                tableSync.TriggerUpdateDrop = "DROP TRIGGER IF EXISTS \"trMergeUpdate_" + tableName + "\"";
                tableSync.TriggerDelete = schemaGenerator.CreateDeleteTrigger(table, tableId);
                tableSync.TriggerDeleteDrop = "DROP TRIGGER IF EXISTS \"trMergeDelete_" + tableName + "\"";
            }

            StringBuilder records = new StringBuilder();
            records.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            records.append("<records>");

            boolean hasResults = cmd.execute(query.toString());

            do {
                if (hasResults) {
                    try (ResultSet rs = cmd.getResultSet()) {
                        tablesData = new CachedRowSetImpl();
                        tablesData.populate(rs);

                        tablesData.beforeFirst();
                        while (tablesData.next()) {

                            hasRows = true;
                            Integer MergeContent_Action = GetSyncActionType();

                            records.append("<r a=\"" + MergeContent_Action + "\">");

                            ResultSetMetaData rsmd = tablesData.getMetaData();

                            switch (MergeContent_Action) {
                                case 1://insert
                                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                        String columnName = rsmd.getColumnName(i);
                                        String colDataType = rsmd.getColumnTypeName(i);
                                        String colValue = tablesData.getString(i);
                                        Boolean wasNull = tablesData.wasNull();
                                        if (!columnName.equalsIgnoreCase("MergeInsertSource") && !columnName.equalsIgnoreCase("RowVer") && !columnName.toLowerCase().contains("MergeContent_".toLowerCase())) {
                                            BuildSyncRowXML(records, colDataType, colValue, wasNull);
                                        }
                                    }
                                    break;
                                case 2://update
                                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                        String columnName = rsmd.getColumnName(i);
                                        String colDataType = rsmd.getColumnTypeName(i);
                                        String colValue = tablesData.getString(i);
                                        Boolean wasNull = tablesData.wasNull();
                                        Boolean colInPK = false;
                                        for (String pk : table.PrimaryKeyColumns)
                                            if (pk.equalsIgnoreCase(columnName))
                                                colInPK = true;
                                        if (!columnName.equalsIgnoreCase("MergeInsertSource") && !columnName.equalsIgnoreCase("RowVer") && !columnName.toLowerCase().contains("MergeContent_".toLowerCase()) && !colInPK) {
                                            BuildSyncRowXML(records, colDataType, colValue, wasNull);
                                        }
                                    }

                                    if (table.PrimaryKeyColumns.size() == 0)
                                        AddRowIdParameter(records, rsmd);
                                    else {
                                        for (String pk : table.PrimaryKeyColumns) {
                                            Integer identityColIndex = -1;
                                            for (int i = 1; i <= rsmd.getColumnCount(); i++)
                                                if (rsmd.getColumnName(i).equalsIgnoreCase(pk))
                                                    identityColIndex = i;

                                            if (identityColIndex > -1) {
                                                String colDataType = rsmd.getColumnTypeName(identityColIndex);
                                                String colValue = tablesData.getString(identityColIndex);

                                                records.append("<c>");
                                                if (colDataType.equalsIgnoreCase("Decimal") || colDataType.equalsIgnoreCase("Money") || colDataType.equalsIgnoreCase("Real"))
                                                    records.append(colValue.replace(",", "."));
                                                else//<![CDATA[" and ends with "]]>
                                                    records.append(colValue.replace("<", "{ln}").replace(">", "{pn}").replace("&", " ").replace("'", "`"));
                                                records.append("</c>");
                                            }
                                        }
                                    }
                                    break;
                                case 3://delete
                                    AddRowIdParameter(records, rsmd);

                                    break;
                            }

                            records.append("</r>");
                        }

                    } catch (Exception e) {
                        Logs.write(Logs.Level.ERROR, "EnumarateChanges() " + e.getMessage());
                    }
                }

                hasResults = cmd.getMoreResults();

            } while (hasResults || cmd.getUpdateCount() != -1);

            records.append("</records>");
            tableSync.Records = records.toString();

        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "EnumarateChanges() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }


        if (hasRows)
            dataToSync.add(tableSync);
    }

    private void AddRowIdParameter(StringBuilder records, ResultSetMetaData rsmd) throws SQLException {
        for (int i = 1; i < rsmd.getColumnCount(); i++) {
            String columnName = rsmd.getColumnName(i);
            String colValue = tablesData.getString(i);
            if (columnName.toLowerCase().contains("MergeContent_RowId".toLowerCase())) {
                records.append("<c>");
                records.append(colValue.replace("<", "{ln}").replace(">", "{pn}").replace("&", " ").replace("'", "`"));
                records.append("</c>");
            }
        }
    }

    private Integer GetSyncActionType() throws SQLException {
        Integer MergeContent_Action = tablesData.getInt("MergeContent_Action");
        if (MergeContent_Action != 3) {
            Date d = tablesData.getDate("MergeContent_ChangeDate");
            if (MergeContent_Action == -1 && tablesData.wasNull())
                MergeContent_Action = 1;
            else
                MergeContent_Action = 2;
        }
        return MergeContent_Action;
    }

    private void BuildSyncRowXML(StringBuilder records, String colDataType, String colValue, Boolean wasNull) {
        records.append("<c>");
        if (colDataType.equals("Decimal") || colDataType.equals("Money") || colDataType.equals("Real"))
            records.append(colValue.replace(",", "."));
        else if (colDataType.equalsIgnoreCase("Boolean") || colDataType.equalsIgnoreCase("bit")) {
            if (colValue.equalsIgnoreCase("False"))
                records.append("0");
            else
                records.append("1");
        } else if (Helpers.TypeConvertionTableIsBLOBType(colDataType)) {
            if (!wasNull) {
                byte[] bytesEncoded = Base64.encodeBase64(colValue.getBytes());
                records.append(new String(bytesEncoded));
            }
        } else if (colDataType.equalsIgnoreCase("datetime") || colDataType.equalsIgnoreCase("date")) {
            DateFormat format = new SimpleDateFormat(SQLiteSyncConfig.DATE_FORMAT);
            if (colValue != null && !colValue.isEmpty()) {
                try {
                    Date date = format.parse(colValue);
                    records.append(format.format(date));
                } catch (ParseException e) {
                    records.append("");
                }
            } else
                records.append("");
        } else {//<![CDATA[" and ends with "]]>
            if (!wasNull)
                records.append(colValue.replace("<", "{ln}").replace(">", "{pn}").replace("&", " ").replace("'", "`"));
            else
                records.append("");
        }
        records.append("</c>");
    }

    private StringBuilder BuildMergeQuery(String tableId, String subscriberId, String tableName, String tableSchema, String filterVW, String filterVW_CD) {
        StringBuilder query = new StringBuilder();

        query.append("select distinct * from ( ");
        query.append("select  ");

        query.append("tb.*," + tableId + " as MergeContent_TableId,   ");
        query.append(subscriberId + " as MergeContent_SubscriberId,  ");
        query.append("tb.RowId as MergeContent_RowId,  ");
        query.append("tb.RowVer as MergeContent_RowVer,  ");
        query.append("t.ChangeDate as MergeContent_ChangeDate, ");
        query.append("-1 as MergeContent_Action,   ");
        query.append("null as MergeContent_SyncId   ");

        query.append("from `" + tableName + "` tb ");
        query.append("left join `" + filterVW + "` vw on tb.RowId=vw.RowId ");
        query.append("left join `MergeContent_" + tableName + "` t on vw.RowId=t.RowId and t.SubscriberId=" + subscriberId + " ");
        query.append("where (t.RowVer<>tb.RowVer or t.RowId is null) " + filterVW_CD + " ");

        query.append("union all ");

        query.append("select  ");
        query.append("t.*," + tableId + " as MergeContent_TableId,   ");
        query.append(subscriberId + " as MergeContent_SubscriberId,  ");
        query.append("m.RowId as MergeContent_RowId,  ");
        query.append("m.RowVer as MergeContent_RowVer,  ");
        query.append("m.ChangeDate as MergeContent_ChangeDate,   ");
        query.append("3 as MergeContent_Action,   ");
        query.append("null as MergeContent_SyncId   ");

        query.append("from `MergeContent_" + tableName + "` m ");
        query.append("left join `" + filterVW + "` vw on m.RowId=vw.RowId and m.SubscriberId=" + subscriberId + " " + filterVW_CD + " ");
        query.append("left join `" + tableName + "` t on vw.RowId=t.RowId  ");
        query.append("where vw.RowId is null  and m.SubscriberId=" + subscriberId + " ");
        query.append(" ) as mergeVw");

        return query;
    }

    private void GenerateQueries(DataObject tableSync, String tableSchema) {
        String tableNameClear = tableSync.TableName;

        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableNameClear, tableSchema);

        StringBuilder insertStatment = new StringBuilder();
        StringBuilder updateStatment = new StringBuilder();

        insertStatment.append("INSERT INTO " + tableNameClear + " (");
        for (DatabaseTableColumn col : table.Columns)
            if (!col.Name.equalsIgnoreCase("MergeInsertSource") && !col.Name.equalsIgnoreCase("RowVer")) {
                insertStatment.append("[" + col.Name + "]");
                insertStatment.append(",");
            }

        tableSync.QueryInsert = insertStatment.toString().substring(0, insertStatment.toString().length() - 1) + ") VALUES (";
        insertStatment = new StringBuilder();
        for (DatabaseTableColumn col : table.Columns)
            if (!col.Name.equalsIgnoreCase("MergeInsertSource") && !col.Name.equalsIgnoreCase("RowVer")) {
                insertStatment.append("?");
                insertStatment.append(",");
            }
        tableSync.QueryInsert += insertStatment.toString().substring(0, insertStatment.toString().length() - 1) + ");";

        updateStatment.append("UPDATE " + tableNameClear + " set ");
        for (DatabaseTableColumn col : table.Columns)
            if (!col.Name.equalsIgnoreCase("MergeInsertSource") && !col.Name.equalsIgnoreCase("RowVer")) {
                if (!col.IsInPrimaryKey) {
                    updateStatment.append("[" + col.Name + "]");
                    updateStatment.append("=?,");
                }
            }

        updateStatment = new StringBuilder(updateStatment.toString().substring(0, updateStatment.toString().length() - 1));

        updateStatment.append(" where ");

        if (table.PrimaryKeyColumns.size() > 0) {
            for (String pk : table.PrimaryKeyColumns) {
                updateStatment.append(pk);
                updateStatment.append("=? and ");
            }

            tableSync.QueryUpdate = updateStatment.toString().substring(0, updateStatment.toString().length() - 5) + ";";
        } else {
            updateStatment.append(SQLQueries.GET_ROWID_COLUMN_NAME() + "=?");
            tableSync.QueryUpdate = updateStatment.toString() + ";";
        }

        tableSync.QueryDelete = "DELETE FROM " + tableNameClear + " where " + SQLQueries.GET_ROWID_COLUMN_NAME() + "=";
    }

    public void CommitSync(String syncId) {

        BinaryWriter binaryWriter = new BinaryWriter();
        tablesData = (CachedRowSetImpl) binaryWriter.readFromBinaryFile(SQLiteSyncConfig.WORKING_DIR + "SyncData/" + syncId + ".dat");

        String tableName = "";
        Integer tableId = 0;
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            PreparedStatement query = cn.prepareStatement(QUERIES.COMMIT_SYNC());
            query.setInt(1, Integer.parseInt(syncId));
            ResultSet reader = query.executeQuery();
            while (reader.next()) {
                tableName = reader.getString("TableName");
                tableId = reader.getInt("TableId");
            }
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "CommitSync() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }


        UpdateSyncData(Integer.parseInt(syncId), tableName, tableId);
        SetSyncFinishMarker(syncId);
    }

    private void SetSyncFinishMarker(String syncId) {
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            PreparedStatement query = cn.prepareStatement(QUERIES.COMMIT_SYNC_UPDATE());
            query.setInt(1, Integer.parseInt(syncId));
            query.execute();
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "SetSyncFinishMarker() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    private Integer SetSyncStartMarker(String subscriberId, Integer tableId) {
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            Integer id = 0;
            Integer affectedRows = 0;
            PreparedStatement query = cn.prepareStatement(QUERIES.START_NEW_SYNC(), Statement.RETURN_GENERATED_KEYS);
            query.setInt(1, Integer.parseInt(subscriberId));
            query.setString(2, "");
            query.setInt(3, tableId);


            affectedRows = query.executeUpdate();

            if (affectedRows == 0) {
                Logs.write(Logs.Level.ERROR, "Creating new sync failed, no ID obtained.");
            }

            try (ResultSet generatedKeys = query.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    id = generatedKeys.getInt(1);
                } else {
                    Logs.write(Logs.Level.ERROR, "Creating new sync failed, no ID obtained.");
                }
            }

            return id;
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "SetSyncStartMarker() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
        return 0;
    }

    private void UpdateSyncData(Integer syncId, String tableName, Integer tableId) {

        Connection cn = Database.getInstance().GetDBConnection();
        try {
            Integer subscriberId = 0;
            PreparedStatement cmdI = cn.prepareStatement(QUERIES.INSERT_MERGE_CONTENT(tableName));
            PreparedStatement cmdU = cn.prepareStatement(QUERIES.UPDATE_SYNC_DATA_UPDATE(tableName));
            PreparedStatement cmdD = cn.prepareStatement(QUERIES.UPDATE_SYNC_DATA_DELETE(tableName));

            tablesData.beforeFirst();
            while (tablesData.next()) {
                subscriberId = tablesData.getInt("MergeContent_SubscriberId");
                Integer MergeContent_Action = GetSyncActionType();

                switch (MergeContent_Action.toString()) {
                    case "1"://insert
                        cmdI.setInt(1, tableId);
                        cmdI.setInt(2, subscriberId);
                        cmdI.setString(3, tablesData.getString("MergeContent_RowId"));
                        cmdI.setInt(4, tablesData.getInt("MergeContent_RowVer"));
                        cmdI.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
                        cmdI.setInt(6, 1);
                        cmdI.setInt(7, syncId);
                        cmdI.addBatch();
                        break;
                    case "2"://update
                        cmdU.setInt(1, tablesData.getInt("MergeContent_RowVer"));
                        cmdU.setInt(2, tableId);
                        cmdU.setString(3, tablesData.getString("MergeContent_RowId"));
                        cmdU.setInt(4, subscriberId);
                        cmdU.addBatch();
                        break;
                    case "3"://delete
                        cmdD.setString(1, tablesData.getString("MergeContent_RowId"));
                        cmdD.setInt(2, subscriberId);
                        cmdD.addBatch();
                        break;
                }
            }

            cmdI.executeBatch();
            cmdU.executeBatch();
            cmdD.executeBatch();

        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "UpdateSyncData() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    public void ReceiveData(DeviceDataObject receivedData) {
        Logs.write(Logs.Level.INFO, "Receiving data from subscriber " + receivedData.getSubscriber());


        CommonTools common = new CommonTools();
        String subscriberId = common.CheckIfSubscriberExists(receivedData.getSubscriber()).toString();

        if (subscriberId.equalsIgnoreCase("-1")) {
            Logs.write(Logs.Level.ERROR, "Error creating new subscriber for UUID " + receivedData.getSubscriber());
            return;
        }

        Integer syncId = StartNewReception(subscriberId, receivedData.getContent());

        CommitChangesToDb(receivedData.getContent(), subscriberId);
        SetSyncFinishMarker(syncId.toString());
        Logs.write(Logs.Level.INFO, "Finished receiving data from subscriber " + receivedData.getSubscriber());
    }

    private Integer StartNewReception(String subscriberId, String data) {
        Integer syncId = SetSyncStartMarker(subscriberId, -1);
        BufferedWriter writer = null;

        File theDir = new File(SQLiteSyncConfig.WORKING_DIR + "ReceivedData");
        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                Logs.write(Logs.Level.ERROR, "StartNewReception()->Creating folder ReceivedData " + se.getMessage());
            }
        }

        CommonTools.DeleteFilesOlderThanNdays(SQLiteSyncConfig.HISTORY_DAYS, "dat", SQLiteSyncConfig.WORKING_DIR + "ReceivedData/");

        try {
            File recieveDataFile = new File(SQLiteSyncConfig.WORKING_DIR + "ReceivedData/" + syncId.toString() + ".dat");
            writer = new BufferedWriter(new FileWriter(recieveDataFile));
            writer.write(data);
        } catch (Exception e) {
            Logs.write(Logs.Level.ERROR, "StartNewReception() " + e.getMessage());
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                Logs.write(Logs.Level.ERROR, "StartNewReception() " + e.getMessage());
            }
        }

        return syncId;
    }

    private void CommitChangesToDb(String data, String subscriber) {
        try {

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);

            String currentTable = "";

            String insertRecordsXML = "";
            boolean isInsertRecords = false;
            String updateRecordsXML = "";
            boolean isUpdateRecords = false;
            String deletedRecordsXML = "";
            boolean isDeleteRecords = false;

            int eventType;
            while (streamReader.hasNext()) {
                eventType = streamReader.next();

                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:

                        if (streamReader.getLocalName().equals("ins")) {
                            isInsertRecords = true;
                        }
                        if (isInsertRecords) {
                            insertRecordsXML += "<" + streamReader.getLocalName() + ">";
                        }

                        if (streamReader.getLocalName().equals("upd")) {
                            isUpdateRecords = true;
                        }
                        if (isUpdateRecords) {
                            updateRecordsXML += "<" + streamReader.getLocalName() + ">";
                        }

                        if (streamReader.getLocalName().equals("delete")) {
                            isDeleteRecords = true;
                        }
                        if (isDeleteRecords) {
                            deletedRecordsXML += "<" + streamReader.getLocalName() + ">";
                        }

                        switch (streamReader.getLocalName()) {
                            case "tab": {
                                if ((currentTable != streamReader.getAttributeValue(0) && !currentTable.isEmpty()) || isDeleteRecords) {
                                    PushTableData(subscriber, currentTable, insertRecordsXML, updateRecordsXML);
                                    Logs.write(Logs.Level.DEBUG, "CommitChangesToDb(). Finished collecting changes for table " + currentTable + ".");
                                    insertRecordsXML = "";
                                    updateRecordsXML = "";
                                }
                                currentTable = streamReader.getAttributeValue(0);
                                Logs.write(Logs.Level.DEBUG, "CommitChangesToDb(). Started collecting changes for table " + currentTable + ".");
                            }
                            default:
                                break;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (isInsertRecords) {
                            insertRecordsXML += streamReader.getText();
                        }
                        if (isUpdateRecords) {
                            updateRecordsXML += streamReader.getText();
                        }
                        if (isDeleteRecords) {
                            deletedRecordsXML += streamReader.getText();
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (streamReader.getLocalName().equals("ins")) {
                            insertRecordsXML += "</" + streamReader.getLocalName() + ">";
                            isInsertRecords = false;
                        }
                        if (isInsertRecords && !streamReader.getLocalName().equals("ins")) {
                            insertRecordsXML += "</" + streamReader.getLocalName() + ">";
                        }
                        if (streamReader.getLocalName().equals("upd")) {
                            updateRecordsXML += "</" + streamReader.getLocalName() + ">";
                            isUpdateRecords = false;
                        }
                        if (isUpdateRecords && !streamReader.getLocalName().equals("upd")) {
                            updateRecordsXML += "</" + streamReader.getLocalName() + ">";
                        }
                        if (streamReader.getLocalName().equals("delete")) {
                            deletedRecordsXML += "</" + streamReader.getLocalName() + ">";
                            isDeleteRecords = false;
                        }
                        if (isDeleteRecords && !streamReader.getLocalName().equals("delete")) {
                            deletedRecordsXML += "</" + streamReader.getLocalName() + ">";
                        }
                        break;
                    default:
                        break;
                }
            }

            //for last table
            PushTableData(subscriber, currentTable, insertRecordsXML, updateRecordsXML);
            Logs.write(Logs.Level.DEBUG, "CommitChangesToDb(). Finished collecting changes for table " + currentTable + ".");
            //deleteing deleted records
            Logs.write(Logs.Level.DEBUG, "CommitChangesToDb(). Started collecting deleted records.");
            PushDeletedRecrods(subscriber, deletedRecordsXML);
            Logs.write(Logs.Level.DEBUG, "CommitChangesToDb(). Finished collecting deleted records.");

        } catch (XMLStreamException e) {
            Logs.write(Logs.Level.ERROR, "CommitChangesToDb() " + e.getMessage());
        }
    }

    private void PushTableData(String subscriber, String currentTable, String insertRecordsXML, String updateRecordsXML) {
        try {
            CommonTools commonTools = new CommonTools();
            commonTools.CheckIdentiesRange(currentTable, subscriber);

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            if (!insertRecordsXML.equalsIgnoreCase("<ins></ins>")) {
                InputStream in = new ByteArrayInputStream(insertRecordsXML.getBytes(StandardCharsets.UTF_8));
                XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
                PushInsertRecords(subscriber, currentTable, streamReader);
            }

            if (!updateRecordsXML.equalsIgnoreCase("<upd></upd>")) {
                InputStream inUpd = new ByteArrayInputStream(updateRecordsXML.getBytes(StandardCharsets.UTF_8));
                XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inUpd);
                PushUpdateRecords(subscriber, currentTable, streamReader);
            }

        } catch (XMLStreamException e) {
            Logs.write(Logs.Level.ERROR, "PushTableData() " + e.getMessage());
        }
    }

    private DatabaseTableParameter GetColumnPosition(String colName, List<DatabaseTableParameter> paramList) {
        for (DatabaseTableParameter p : paramList)
            if (p.ParameterName.equalsIgnoreCase(colName))
                return p;

        return null;
    }

    private void PushInsertRecords(String subscriber, String currentTable, XMLStreamReader insertRecords) {

        String tableSchema = GetTableSchema(currentTable);
        SchemaGenerator schemaGen = new SchemaGenerator();
        String insertStatment = schemaGen.CreateInsertStatmentWithParams(currentTable, tableSchema);

        //insert new records
        List<DatabaseTableParameter> paramList = schemaGen.GetStatmentParams(currentTable, true, tableSchema);

        Connection cn = Database.getInstance().GetDBConnection();
        try {
            recieveDataStatment = cn.prepareStatement(insertStatment);
            PreparedStatement mergeContent = cn.prepareStatement(QUERIES.INSERT_MERGE_CONTENT(currentTable));

            int eventType;
            Boolean isRecordData = false;
            Boolean hasRecords = false;
            String colName = "";
            Boolean isEmptyColValue = false;
            while (insertRecords.hasNext()) {
                eventType = insertRecords.next();

                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (insertRecords.getLocalName().equals("r")) {
                            isRecordData = true;
                        }
                        if (isRecordData) {
                            colName = insertRecords.getLocalName();
                            isEmptyColValue = true;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (isRecordData) {
                            isEmptyColValue = false;
                            if (!hasRecords)
                                hasRecords = true;
                            if (!colName.equalsIgnoreCase("RowVer") && !colName.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME())) {
                                DatabaseTableParameter param = GetColumnPosition(colName, paramList);
                                ParseStatmentParameter(param.ParameterOrder, colName, insertRecords.getText(), param);
                            }
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (isEmptyColValue) {
                            if (!colName.equalsIgnoreCase("RowVer") && !colName.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME())) {
                                DatabaseTableParameter param = GetColumnPosition(colName, paramList);
                                ParseStatmentParameter(param.ParameterOrder, colName, null, param);
                            }
                        }

                        if (insertRecords.getLocalName().equals("r")) {
                            isRecordData = false;
                            if (hasRecords) {
                                DatabaseTableParameter param = GetColumnPosition("RowVer", paramList);
                                recieveDataStatment.setInt(param.ParameterOrder, 1);
                                String rowIdValue = UUID.randomUUID().toString();
                                param = GetColumnPosition(SQLQueries.GET_ROWID_COLUMN_NAME(), paramList);
                                recieveDataStatment.setString(param.ParameterOrder, rowIdValue);
                                recieveDataStatment.addBatch();

                                mergeContent.setInt(1, 0);//tableid
                                mergeContent.setInt(2, Integer.parseInt(subscriber));
                                mergeContent.setString(3, rowIdValue);
                                mergeContent.setInt(4, 0);//rowver
                                mergeContent.setTimestamp(5, new java.sql.Timestamp(new Date().getTime()));
                                mergeContent.setInt(6, 1);//action
                                mergeContent.setInt(7, 0);//syncId
                                mergeContent.addBatch();
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            if (hasRecords) {
                recieveDataStatment.executeBatch();
                mergeContent.executeBatch();
            }
        } catch (XMLStreamException | SQLException e) {
            Logs.write(Logs.Level.ERROR, "PushInsertRecords() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    private void PushUpdateRecords(String subscriber, String currentTable, XMLStreamReader updateRecords) {
        String tableSchema = GetTableSchema(currentTable);
        SchemaGenerator schemaGen = new SchemaGenerator();
        Object[] retUpdSt = schemaGen.CreateUpdateStatmentWithParams(currentTable, tableSchema);
        String updateStatment = (String) retUpdSt[0];
        Integer paramCount = (Integer) retUpdSt[1];
        //insert new records
        List<DatabaseTableParameter> paramList = schemaGen.GetStatmentParams(currentTable, false, tableSchema);
        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(currentTable, tableSchema);
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            recieveDataStatment = cn.prepareStatement(updateStatment);

            int eventType;
            Boolean isRecordData = false;
            Boolean hasRecords = false;
            String colName = "";
            String rowIdValue = "";

            while (updateRecords.hasNext()) {
                eventType = updateRecords.next();

                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (updateRecords.getLocalName().equals("r")) {
                            isRecordData = true;
                        }
                        if (isRecordData) {
                            colName = updateRecords.getLocalName();
                            //we need to set up default value first
                            Boolean colInPK = false;
                            for (String pk : table.PrimaryKeyColumns)
                                if (pk.equalsIgnoreCase(colName))
                                    colInPK = true;

                            if (!colName.equalsIgnoreCase("RowVer") && !colName.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME()) && !colInPK) {
                                DatabaseTableParameter param = GetColumnPosition(colName, paramList);
                                if (param != null)
                                    ParseStatmentParameter(param.ParameterOrder, colName, null, param);
                            }
                        }
                        break;
                    case XMLStreamConstants.CDATA:
                    case XMLStreamConstants.CHARACTERS:
                        if (isRecordData) {
                            if (!hasRecords)
                                hasRecords = true;
                            if (!colName.equalsIgnoreCase("RowVer") && !colName.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME())) {
                                DatabaseTableParameter param = GetColumnPosition(colName, paramList);
                                if (param != null)
                                    ParseStatmentParameter(param.ParameterOrder, colName, updateRecords.getText(), param);
                            } else if (colName.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME()))
                                rowIdValue = updateRecords.getText();
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (updateRecords.getLocalName().equals("r")) {
                            isRecordData = false;
                            recieveDataStatment.setString(paramCount, rowIdValue);
                            recieveDataStatment.addBatch();
                        }
                        break;
                    default:
                        break;
                }
            }

            if (hasRecords) {
                recieveDataStatment.executeBatch();
            }
        } catch (XMLStreamException | SQLException e) {
            Logs.write(Logs.Level.ERROR, "PushUpdateRecords() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    private void PushDeletedRecrods(String subscriber, String deletedRecordsXML) {
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            if (deletedRecordsXML.equalsIgnoreCase("<delete></delete>"))
                return;

            CommonTools commonTools = new CommonTools();
            Map<Integer, String> tables = commonTools.GetSynchronizedTables();
            Map<Integer, DatabaseTable> tablesDef = new HashMap<>();

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            InputStream in = new ByteArrayInputStream(deletedRecordsXML.getBytes(StandardCharsets.UTF_8));
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);

            Statement deleteStatment = cn.createStatement();

            int eventType;

            String tableId = "";
            String recordId = "";
            Boolean isTableId = false;
            Boolean isRecordId = false;
            Boolean hasRecords = false;

            while (streamReader.hasNext()) {
                eventType = streamReader.next();

                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (streamReader.getLocalName().equals("tb")) {
                            isTableId = true;
                            isRecordId = false;
                        }
                        if (streamReader.getLocalName().equals("id")) {
                            isRecordId = true;
                            isTableId = false;
                        }

                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (isTableId) {
                            tableId = streamReader.getText();
                            if (!tablesDef.containsKey(tableId)) {
                                String tableSchema = "";
                                String tableName = tables.get(Integer.parseInt(tableId));
                                if (tableName.contains(".")) {
                                    String[] tmp = tableName.split(Pattern.quote("."));
                                    tableSchema = tmp[0];
                                    tableName = tmp[1];
                                }
                                tablesDef.put(Integer.parseInt(tableId), DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema));
                            }
                        }
                        if (isRecordId) {
                            recordId = streamReader.getText();
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (streamReader.getLocalName().equals("r")) {
                            isTableId = false;
                            isRecordId = false;
                            String deleteQuery = "";

                            if (tables.containsKey(Integer.parseInt(tableId))) {

                                deleteQuery = "delete from `" + tables.get(Integer.parseInt(tableId)) + "` where RowId='" + recordId.toString() + "'";
                                deleteStatment.addBatch(deleteQuery);
                                if (!hasRecords)
                                    hasRecords = true;
                            }
                            tableId = "";
                            recordId = "";
                        }
                        break;
                    default:
                        break;
                }
            }

            if (hasRecords)
                deleteStatment.executeBatch();

        } catch (XMLStreamException | SQLException e) {
            Logs.write(Logs.Level.ERROR, "PushDeletedRecrods() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    private void ParseStatmentParameter(Integer colNumber, String colName, String colValue, Object paramDef) {

        if (colValue == null || colValue.equalsIgnoreCase("null"))
            colValue = "";

        DatabaseTableParameter colParamDef = ((DatabaseTableParameter) paramDef);
        DateFormat format = new SimpleDateFormat(SQLiteSyncConfig.DATE_FORMAT);
        DateFormat formatTimestamp = new SimpleDateFormat(SQLiteSyncConfig.TIMESTAMP_FORMAT);
        String colDbType = colParamDef.DbType.toLowerCase();

        try {
            switch (colDbType) {
                case "blob":
                case "longblob":
                case "mediumblob":
                case "varbinary":
                case "binary":
                case "varbinarymax":
                case "image":
                case "byte[]":
                    byte[] byteData = colValue.getBytes("UTF-8");
                    Connection cn = Database.getInstance().GetDBConnection();
                    try {
                        Blob blobData = cn.createBlob();
                        blobData.setBytes(1, byteData);
                        recieveDataStatment.setBlob(colNumber, blobData);
                    } finally {
                        JDBCCloser.close(cn);
                    }
                    break;
                case "longtext":
                case "varchar":
                case "varchar2":
                case "varcharmax":
                case "nvarchar":
                case "enum":
                case "mediumtext":
                case "text":
                case "char":
                case "string":
                case "geography":
                case "geometry":
                case "hierarchyid":
                case "nchar":
                case "ntext":
                case "nvarcharmax":
                case "userdefineddatatype":
                case "userdefinedtabletype":
                case "userdefinedtype":
                case "variant":
                case "xml":
                case "tinytext":
                    if (colValue == null || colValue.isEmpty() || colValue.trim() == "") {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.NVARCHAR);
                        else
                            recieveDataStatment.setString(colNumber, "");
                    } else
                        recieveDataStatment.setString(colNumber, colValue);
                    break;
                case "boolean":
                case "byte":
                case "bit":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.BOOLEAN);
                        else
                            recieveDataStatment.setBoolean(colNumber, Boolean.parseBoolean("0"));
                    } else
                        recieveDataStatment.setBoolean(colNumber, Boolean.parseBoolean(colValue));
                    break;
                case "tinyint":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.TINYINT);
                        else
                            recieveDataStatment.setShort(colNumber, Short.parseShort("0"));
                    } else
                        recieveDataStatment.setShort(colNumber, Short.parseShort(colValue));

                    break;
                case "smallint":
                case "year":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.SMALLINT);
                        else
                            recieveDataStatment.setInt(colNumber, Integer.parseInt("0"));
                    } else
                        recieveDataStatment.setInt(colNumber, Integer.parseInt(colValue));
                    break;
                case "bigint":
                case "long":
                case "int64":
                case "serial":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.BIGINT);
                        else
                            recieveDataStatment.setLong(colNumber, Long.parseLong("0"));
                    } else
                        recieveDataStatment.setLong(colNumber, Long.parseLong(colValue));
                    break;
                case "mediumint":
                case "int":
                case "int16":
                case "int32":
                case "smalldatetime":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.INTEGER);
                        else
                            recieveDataStatment.setInt(colNumber, Integer.parseInt("0"));
                    } else
                        recieveDataStatment.setInt(colNumber, Integer.parseInt(colValue));
                    break;
                case "double":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.DOUBLE);
                        else
                            recieveDataStatment.setDouble(colNumber, Double.parseDouble("0"));
                    } else
                        recieveDataStatment.setDouble(colNumber, Double.parseDouble(colValue));
                    break;
                case "float":
                case "numeric":
                case "decimal":
                case "money":
                case "real":
                case "smallmoney":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.REAL);
                        else
                            recieveDataStatment.setFloat(colNumber, Float.parseFloat("0"));
                    } else
                        recieveDataStatment.setFloat(colNumber, Float.parseFloat(colValue));
                    break;
                case "time":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.TIME);
                        else
                            recieveDataStatment.setTime(colNumber, Time.valueOf("00:00:00"));
                    } else
                        recieveDataStatment.setTime(colNumber, Time.valueOf(colValue));
                    break;

                case "datetimeoffset":
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.TIMESTAMP);
                        else
                            recieveDataStatment.setTimestamp(colNumber, new Timestamp(System.currentTimeMillis()), cal);
                    } else {
                        OffsetDateTime offDt = OffsetDateTime.parse(colValue.split(Pattern.quote("+"))[0].trim().replace(" ", "T") + "+" + colValue.split(Pattern.quote("+"))[1], DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        recieveDataStatment.setTimestamp(colNumber, Timestamp.valueOf(offDt.toLocalDateTime()));
                    }

                    break;
                case "datetime":
                case "datetime2":
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    if (colValue != null && !colValue.isEmpty())
                        timestamp = Timestamp.valueOf(colValue);

                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.DATE);
                        else
                            recieveDataStatment.setTimestamp(colNumber, new java.sql.Timestamp(timestamp.getTime()));
                    } else {
                        recieveDataStatment.setTimestamp(colNumber, new java.sql.Timestamp(timestamp.getTime()));
                    }
                    break;
                case "date":
                    Date date = new Date();
                    if (colValue != null && !colValue.isEmpty())
                        date = format.parse(colValue);

                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.DATE);
                        else
                            recieveDataStatment.setDate(colNumber, new java.sql.Date(date.getTime()));
                    } else {
                        recieveDataStatment.setDate(colNumber, new java.sql.Date(date.getTime()));
                    }
                    break;
                case "uniqueidentifier":
                    if (colValue == null || colValue.isEmpty()) {
                        if (colParamDef.IsNullable)
                            recieveDataStatment.setNull(colNumber, Types.OTHER);
                        else
                            recieveDataStatment.setString(colNumber, UUID.randomUUID().toString());
                    } else
                        recieveDataStatment.setString(colNumber, colValue);
                    break;
                default:
                    recieveDataStatment.setString(colNumber, colValue);
                    break;
            }
        } catch (SQLException | UnsupportedEncodingException | ParseException e) {
            Logs.write(Logs.Level.ERROR, "ParseStatmentParameter() " + e.getMessage());
        }
    }

    private String GetTableSchema(String tableName) {

        try {
            return tableSchemaCache.get(tableName);
        } catch (ExecutionException e) {
            Logs.write(Logs.Level.ERROR, "GetTableSchema() " + e.getMessage());
            return GetDefaultTableSchema();
        }
    }
}
