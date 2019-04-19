package SQLiteSyncCore.SyncServer.SchemaPublish;

import SQLiteSyncCore.JDBCCloser;
import SQLiteSyncCore.Logs;
import SQLiteSyncCore.SQLiteSyncConfig;
import SQLiteSyncCore.SyncServer.CommonTools;
import SQLiteSyncCore.SyncServer.Helpers;
import SQLiteSyncCore.SyncServer.Synchronization.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class SchemaGenerator extends Database {

    Map<String, String> schema = new HashMap<>();
    String _subscriber;
    String _tableId;

    private int schemaOrder = 0;

    public SchemaGenerator() {

    }

    private String GetSchemaOrder() {
        String schemaOrderString = String.valueOf(schemaOrder);

        for (int i = schemaOrderString.length(); i < 5; i++)
            schemaOrderString = "0" + schemaOrderString;

        schemaOrder++;
        return schemaOrderString + " ";
    }

    public String GetFullSchematScript(String subscriberUUID) {

        CommonTools common = new CommonTools();
        String subscriber = common.CheckIfSubscriberExists(subscriberUUID).toString();

        if (subscriber.equalsIgnoreCase("-1")) {
            Logs.write(Logs.Level.ERROR, "Error creating new subscriber for UUID " + subscriberUUID);
            return "Error creating new subscriber for UUID " + subscriberUUID;
        }

        Logs.write(Logs.Level.INFO, "Reinitializing subscriber " + subscriber);
        schema.put(this.GetSchemaOrder() + "SQLiteSync.com version", SQLiteSyncVersion);
        _subscriber = subscriber;
        addMainObjects();
        GenerateTableSchema("MergeIdentity", GetDefaultTableSchema());
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            Statement tableToPublish = cn.createStatement();
            ResultSet reader = tableToPublish.executeQuery(QUERIES.GET_MERGE_TABLES_TO_SYNC());
            while (reader.next())
                if (!reader.getString("TableName").equalsIgnoreCase("MergeIdentity")) {
                    _tableId = (reader.getString("TableId"));
                    String tableSchema = reader.getString("TableSchema");

                    if (tableSchema == null || tableSchema.isEmpty())
                        tableSchema = GetDefaultTableSchema();
                    GenerateTableSchema(reader.getString("TableName"), tableSchema);
                }

            DeleteOldRecordsFromMergeContent(subscriber, "MergeIdentity");
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "GetFullSchematScript() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }

        ReinitializeSubscriber(subscriber);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        StringWriter stringEmp = new StringWriter();
        try {
            objectMapper.writeValue(stringEmp, schema);
        } catch (IOException ex) {
            Logs.write(Logs.Level.ERROR, "GetFullSchematScript()->JSON serialize " + ex.getMessage());
        }

        Logs.write(Logs.Level.INFO, "Finished reinitializing subscriber " + subscriber);
        Logs.write(Logs.Level.TRACE, "Reinitialization script " + stringEmp.toString());
        return stringEmp.toString();
    }

    private void ReinitializeSubscriber(String subscriber) {

        Connection cn = Database.getInstance().GetDBConnection();

        try {
            Statement tableToPublish = cn.createStatement();
            ResultSet reader = tableToPublish.executeQuery(QUERIES.GET_MERGE_TABLES_TO_SYNC());
            while (reader.next())
                if (!reader.getString("TableName").equalsIgnoreCase("MergeIdentity"))
                    CreateNewIdentityPoolForTable(subscriber, reader.getString("TableName"), reader.getString("IdentityRange"), reader.getString("TableId"), reader.getString("TableSchema"), true);
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "ReinitializeSubscriber() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    public void CreateNewIdentityPoolForTable(String subscriber, String tableName, String identityRange, String tableId, String tableSchema, Boolean reinitialization) {

        Connection cnAct = Database.getInstance().GetDBConnection();

        try {
            _tableId = tableId;
            if (tableSchema == null || tableSchema.isEmpty())
                tableSchema = GetDefaultTableSchema();

            DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

            if (reinitialization)
                DeleteOldRecordsFromMergeContent(subscriber, tableName);

            Boolean isAutoIncrement = false;

            for (DatabaseTableColumn column : table.Columns)
                if (column.IsAutoIncrement)
                    isAutoIncrement = true;

            if (table.PrimaryKeyColumns.size() == 1 && isAutoIncrement) {
                Integer maxRowVer = 0;
                Integer identityVal = 0;


                Statement cmdReinitialize_1 = cnAct.createStatement();
                String query_cmdReinitialize_1 = "select (ifnull(max(Rev),0) + 1) as maxRowVer from MergeIdentity where TableId=" + _tableId + " and SubscriberId=" + subscriber;
                maxRowVer = 0;
                ResultSet rscmdReinitialize_1 = cmdReinitialize_1.executeQuery(query_cmdReinitialize_1);
                if (rscmdReinitialize_1.next())
                    maxRowVer = rscmdReinitialize_1.getInt("maxRowVer");

                Statement cmdReinitialize_2 = cnAct.createStatement();
                String query_cmdReinitialize_2 = "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "'";
                identityVal = 0;
                ResultSet rscmdReinitialize_2 = cmdReinitialize_2.executeQuery(query_cmdReinitialize_2);
                if (rscmdReinitialize_2.next())
                    identityVal = rscmdReinitialize_2.getInt("AUTO_INCREMENT");

                Statement cmdReinitialize_3 = cnAct.createStatement();
                String query_cmdReinitialize_3 = "INSERT INTO MergeIdentity " +
                        "   (TableId " +
                        "   ,SubscriberId " +
                        "   ,Rev " +
                        "   ,IdentityStart " +
                        "   ,IdentityEnd " +
                        "   ,IdentityCurrent " +
                        "   ,RowId " +
                        "   ,RowVer) " +
                        "select " + _tableId + ", " + subscriber + ", " + maxRowVer.toString() + " , " + identityVal.toString() + ", " + (identityVal + Integer.parseInt(identityRange)) + ", " + identityVal.toString() + ", uuid(), 1";
                cmdReinitialize_3.execute(query_cmdReinitialize_3);

                //based on: http://stackoverflow.com/questions/970597/change-auto-increment-starting-number
                Statement cmdReinitialize_4 = cnAct.createStatement();
                String query_cmdReinitialize_4 = "alter table " + tableName + " auto_increment=" + (identityVal + Integer.parseInt(identityRange));
                cmdReinitialize_4.execute(query_cmdReinitialize_4);

            }
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "CreateNewIdentityPoolForTable() " + e.getMessage());
        } finally {
            JDBCCloser.close(cnAct);
        }
    }

    private void DeleteOldRecordsFromMergeContent(String subscriber, String tableName) {
        Connection cnAct = Database.getInstance().GetDBConnection();
        try {

            PreparedStatement clearMergeContent = cnAct.prepareStatement(QUERIES.CLEAR_MERGE_CONTENT_BY_SUBSCRIBER(tableName));
            clearMergeContent.setInt(1, Integer.parseInt(subscriber));
            clearMergeContent.execute();
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "DeleteOldRecordsFromMergeContent() " + e.getMessage());
        } finally {
            JDBCCloser.close(cnAct);
        }
    }

    private void addMainObjects() {
        schema.put(this.GetSchemaOrder() + "MergeDelete drop", "DROP TABLE IF EXISTS MergeDelete;");
        schema.put(this.GetSchemaOrder() + "MergeDelete create", "CREATE TABLE \"MergeDelete\" ( \"TableId\" INTEGER, \"RowId\" TEXT );");
    }

    private void GenerateTableSchema(String tableName, String tableSchema) {
        StringBuilder schemaTmp = new StringBuilder();
        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

        schema.put(this.GetSchemaOrder() + tableName + " drop", "DROP TABLE IF EXISTS " + tableName);

        schemaTmp.append("CREATE TABLE \"" + tableName + "\" (");
        StringBuilder columns = new StringBuilder();
        StringBuilder triggerInsert = new StringBuilder();
        StringBuilder triggerUpdate = new StringBuilder();
        StringBuilder triggerDelete = new StringBuilder();

        if (!tableName.equalsIgnoreCase("MergeIdentity")) {
            triggerUpdate.append(CreateUpdateTrigger(table, GenerateUpdateableColumns(table.Columns)));
            triggerDelete.append(CreateDeleteTrigger(table, _tableId.toString()));
        }

        for (DatabaseTableColumn column : table.Columns)
            if (!column.Name.equalsIgnoreCase("MergeInsertSource") && !column.Name.equalsIgnoreCase("RowVer")) {
                columns.append("[" + column.Name + "] ");
                columns.append(Helpers.TypeConvertionTable(column.DataTypeName) + " ");

                if (!column.AllowDBNull && !column.IsInPrimaryKey)
                    columns.append("NOT NULL ");

                if (column.DefaultValue != null && !column.DefaultValue.isEmpty())
                    columns.append(" " + BuildDefaultValue(column) + " ");

                if (column.IsAutoIncrement && !tableName.equalsIgnoreCase("MergeIdentity"))
                    triggerInsert.append(CreateInsertTrigger(table, _tableId.toString(), _subscriber));

                columns.append(",");
            }

        columns.append("\"MergeUpdate\" ");
        columns.append(" INTEGER ");
        columns.append("NOT NULL ");
        columns.append("DEFAULT (0) ");
        columns.append(",");

        schemaTmp.append(columns.toString().substring(0, columns.toString().length() - 1));
        schemaTmp.append(");");
        schema.put(this.GetSchemaOrder() + tableName, schemaTmp.toString());
        if (triggerInsert.toString().trim().length() > 0)
            schema.put(this.GetSchemaOrder() + tableName + "_MergeInsert", triggerInsert.toString());
        if (triggerUpdate.toString().trim().length() > 0)
            schema.put(this.GetSchemaOrder() + tableName + "_MergeUpdate", triggerUpdate.toString());
        if (triggerDelete.toString().trim().length() > 0)
            schema.put(this.GetSchemaOrder() + tableName + "_MergeDelete", triggerDelete.toString());

        //indexes
        if (!tableName.equalsIgnoreCase("MergeIdentity")) {
            for (DatabaseTableIndex idx : table.Indexes) {
                schemaTmp = new StringBuilder();
                schemaTmp.append("CREATE ");
                if (idx.IsUnique)
                    schemaTmp.append("UNIQUE ");

                schemaTmp.append("INDEX ");
                if (idx.Name.equalsIgnoreCase("PRIMARY"))
                    schemaTmp.append("\"PK_" + tableName + "_" + idx.Name + "\" on ");
                else
                    schemaTmp.append("\"" + idx.Name + "\" on ");
                schemaTmp.append(tableName + " (");

                StringBuilder indexedColumns = new StringBuilder();

                for (String col : idx.Columns) {
                    indexedColumns.append(col + " ASC,");
                }

                schemaTmp.append(indexedColumns.toString().substring(0, indexedColumns.toString().length() - 1));
                schemaTmp.append(");");

                schema.put(this.GetSchemaOrder() + tableSchema + "_" + tableName + "_" + idx.Name, schemaTmp.toString());
            }

            schema.put(this.GetSchemaOrder() + tableName + "_MergeUpdate_Index", "CREATE INDEX \"" + tableName + "_MergeUpdateIndex\" on " + tableName + " (MergeUpdate ASC);");
        } else {
            schema.put(this.GetSchemaOrder() + "MergeIdentity_Index", "CREATE INDEX \"MergeIdentity_PK_Index\" ON \"" + tableName + "\" (TableId ASC, SubscriberId ASC, IdentityCurrent ASC, Rev ASC)");
        }
    }

    private String BuildDefaultValue(DatabaseTableColumn column) {
        String defaultValue = "";
        if (column.DataTypeName.equalsIgnoreCase("uniqueidentifier"))
            return "";
        switch (Helpers.TypeConvertionTable(column.DataTypeName)) {
            case "BLOB":
                break;
            case "TEXT":
                defaultValue = "DEFAULT ('" + column.DefaultValue + "')";
                break;
            case "INTEGER":
                defaultValue = "DEFAULT (" + column.DefaultValue + ")";
                break;
            case "REAL":
                defaultValue = "DEFAULT (" + column.DefaultValue + ")";
                break;
        }
        return defaultValue;
    }

    public String CreateDeleteTrigger(DatabaseTable table, String tableId) {
        StringBuilder trigger = new StringBuilder();

        trigger.append(" CREATE TRIGGER IF NOT EXISTS \"trMergeDelete_" + table.Name + "\" ");
        trigger.append("    AFTER DELETE ");
        trigger.append("    ON " + table.Name + "  ");
        trigger.append(" BEGIN 	 ");

        if (table.ReadOnly)
            trigger.append(" 	SELECT RAISE(ABORT, 'Table " + table.Name + " is readonly.'); ");
        else
            trigger.append(" 	INSERT INTO MergeDelete values (" + tableId + ",  old.RowId); ");

        trigger.append(" END; ");

        return trigger.toString();
    }

    public String CreateUpdateTrigger(DatabaseTable table, String updateableColumns) {
        StringBuilder trigger = new StringBuilder();

        trigger.append(" CREATE TRIGGER IF NOT EXISTS \"trMergeUpdate_" + table.Name + "\" ");
        trigger.append("    AFTER UPDATE OF ");
        trigger.append(updateableColumns);
        trigger.append("    ON " + table.Name + " ");
        trigger.append(" BEGIN ");

        if (table.ReadOnly)
            trigger.append(" 	SELECT RAISE(ABORT, 'Table " + table.Name + " is readonly.'); ");
        else
            trigger.append(" 	UPDATE " + table.Name + " SET MergeUpdate = 1 WHERE RowId = old.RowId and RowId<>''; ");

        trigger.append(" END; ");

        return trigger.toString();
    }

    public String CreateInsertTrigger(DatabaseTable table, String tableId, String subscriberId) {
        StringBuilder trigger = new StringBuilder();

        if (table.PrimaryKeyColumns.size() == 1) {

            trigger.append(" CREATE TRIGGER  IF NOT EXISTS \"trMergeInsert_" + table.Name + "\" ");
            trigger.append("    AFTER  INSERT  ");
            trigger.append("    ON " + table.Name + " ");
            trigger.append(" BEGIN ");

            trigger.append(" 	SELECT CASE ");
            trigger.append(" 	  WHEN  ");
            trigger.append(" 	   (select IdentityCurrent from MergeIdentity where TableId=" + tableId + " and SubscriberId=" + subscriberId + " and  IdentityCurrent<IdentityEnd  order by Rev ASC) IS NULL ");
            trigger.append(" 	  THEN  ");
            trigger.append(" 	   RAISE(ABORT, 'Identity range for table " + table.Name + " is exhausted.') ");
            trigger.append(" 	END; ");
            trigger.append(" 	update MergeIdentity set IdentityCurrent = IdentityCurrent + 1, MergeUpdate=1 where TableId=" + tableId + " and SubscriberId=" + subscriberId + "  ");
            trigger.append(" 	and Rev=(select Rev from MergeIdentity where TableId=" + tableId + " and SubscriberId=" + subscriberId + " and  IdentityCurrent<IdentityEnd  order by Rev ASC limit 1); ");

            trigger.append(" 	update " + table.Name + " set " + table.PrimaryKeyColumns.get(0) + " = (select IdentityCurrent from MergeIdentity where TableId=" + tableId + " and SubscriberId=" + subscriberId + " and  IdentityCurrent<IdentityEnd  order by Rev ASC limit 1) where " + table.PrimaryKeyColumns.get(0) + " IS NULL; ");

            trigger.append(" END; ");
        }

        if (table.ReadOnly) {
            trigger.append(" CREATE TRIGGER IF NOT EXISTS \"trMergeInsert_" + table.Name + "\" ");
            trigger.append("    AFTER  INSERT  ");
            trigger.append("    ON " + table.Name + " ");
            trigger.append(" BEGIN ");

            trigger.append(" SELECT RAISE(ABORT, 'Table " + table.Name + " is readonly.'); ");

            trigger.append(" END; ");
        }
        return trigger.toString();
    }

    public String CreateInsertStatmentWithParams(String tableName, String tableSchema) {
        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

        StringBuilder insert = new StringBuilder();

        insert.append("INSERT INTO `" + table.Name + "` ");

        insert.append(" (");
        for (DatabaseTableColumn col : table.Columns) {
            insert.append("[" + col.Name + "],");
        }
        String tmp = insert.toString().substring(0, insert.toString().length() - 1);
        insert = new StringBuilder();
        insert.append(tmp);
        insert.append(") VALUES (");

        for (DatabaseTableColumn col : table.Columns) {
            insert.append("?,");
        }
        tmp = insert.toString().substring(0, insert.toString().length() - 1);
        insert = new StringBuilder();
        insert.append(tmp);
        insert.append(")");

        return insert.toString();
    }

    public Object[] CreateUpdateStatmentWithParams(String tableName, String tableSchema) {
        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

        StringBuilder update = new StringBuilder();

        update.append("UPDATE `" + table.Name + "` SET ");

        Integer paramCount = 0;
        for (DatabaseTableColumn col : table.Columns)
            if (!col.IsInPrimaryKey && !col.Name.equalsIgnoreCase("RowVer") && !col.Name.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME()) && !col.Name.equalsIgnoreCase("MergeInsertSource")) {
                {
                    update.append("[" + col.Name + "]=?,");
                    paramCount++;
                }
            }

        String tmp = update.toString().substring(0, update.toString().length() - 1);
        update = new StringBuilder();
        update.append(tmp);
        update.append(" WHERE RowId=?;");
        paramCount++;

        Object[] ret = new Object[2];
        ret[0] = update.toString();
        ret[1] = paramCount;
        return ret;
    }

    public List<DatabaseTableParameter> GetStatmentParams(String tableName, Boolean withIdentity, String tableSchema) {
        DatabaseTable table = DatabaseTableGuavaCacheUtil.getTableUsingGuava(tableName, tableSchema);

        List<DatabaseTableParameter> paramsList = new ArrayList<>();

        Integer order = 0;
        for (Integer i = 0; i < table.Columns.size(); i++) {
            if ((!table.Columns.get(i).IsAutoIncrement && !table.Columns.get(i).IsInPrimaryKey) || withIdentity) {
                DatabaseTableParameter param = new DatabaseTableParameter();
                param.IsNullable = table.Columns.get(i).AllowDBNull;
                param.ParameterName = table.Columns.get(i).Name;
                param.ParameterOrder = order + 1;
                order++;
                param.DbType = table.Columns.get(i).SqlDataTypeName.toLowerCase();//(DbType) Enum.Parse(typeof(DbType), Helpers.GetSqlDbType(table.Columns[i].SqlDataTypeName.toLowerCase()));
                paramsList.add(param);

            }
        }
        return paramsList;
    }

    public String GenerateUpdateableColumns(List<DatabaseTableColumn> columns) {
        String updateableColumns = "";
        for (DatabaseTableColumn column : columns)
            if (!column.IsInPrimaryKey && !column.Name.equalsIgnoreCase("MergeUpdate") && !column.Name.equalsIgnoreCase(SQLQueries.GET_ROWID_COLUMN_NAME()) && !column.Name.equalsIgnoreCase("RowVer"))
                updateableColumns += "[" + column.Name + "],";
        if (updateableColumns.endsWith(","))
            updateableColumns = updateableColumns.substring(0, updateableColumns.length() - 1);

        return updateableColumns;
    }
}
