package SQLiteSyncCore.SyncServer.Synchronization;

import SQLiteSyncCore.JDBCCloser;
import SQLiteSyncCore.Logs;
import SQLiteSyncCore.SQLiteSyncConfig;
import SQLiteSyncCore.SyncServer.CommonTools;
import SQLiteSyncCore.SyncServer.Helpers;
import SQLiteSyncCore.SyncServer.SchemaPublish.SchemaGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseTable extends Database {

    public String Name = null;
    public String Schema = null;
    public Boolean ReadOnly = false;
    public List<DatabaseTableColumn> Columns = new ArrayList<DatabaseTableColumn>();
    public List<DatabaseTableIndex> Indexes = new ArrayList<DatabaseTableIndex>();
    public List<String> PrimaryKeyColumns = new ArrayList<String>();

    public DatabaseTable(String _tableName, String _schema) {
        Name = _tableName;
        Schema = _schema;
        if (Schema.isEmpty()) {
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            Schema = schemaGenerator.GetDefaultTableSchema();
        }
        if (Name != null && !Name.isEmpty()) {
            BuildTableFromMySQL();
        }
    }

    private Boolean IsIndexExists(String indexName) {
        for (DatabaseTableIndex idx : this.Indexes)
            if (idx.Name.equals(indexName))
                return true;

        return false;
    }

    private DatabaseTableIndex GetIndexByName(String indexName) {
        for (DatabaseTableIndex idx : this.Indexes)
            if (idx.Name.equals(indexName))
                return idx;

        return new DatabaseTableIndex();
    }

    private void BuildTableFromMySQL() {
        Connection cn = Database.getInstance().GetDBConnection();
        try {
            if (cn == null)
                return;

            Statement getColumns = cn.createStatement();
            String getColumnsQuery = "select * from information_schema.columns where table_schema = DATABASE() and table_name='" + this.Name + "' order by table_name,ordinal_position ";

            ResultSet reader = getColumns.executeQuery(getColumnsQuery);

            while (reader.next()) {
                DatabaseTableColumn column = new DatabaseTableColumn();
                column.Name = reader.getString("column_name");
                if (reader.getString("is_nullable").equals("YES"))
                    column.AllowDBNull = true;
                else
                    column.AllowDBNull = false;

                column.DataTypeName = reader.getString("data_type");
                column.DefaultValue = reader.getString("column_default");

                if (column.DefaultValue != null && column.DefaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP"))
                    column.DefaultValue = "";//"datetime('now', 'localtime')";

                if (reader.getString("extra").equals("auto_increment"))
                    column.IsAutoIncrement = true;
                else
                    column.IsAutoIncrement = false;

                if (reader.getString("column_key").equals("PRI"))
                    column.IsInPrimaryKey = true;
                else
                    column.IsInPrimaryKey = false;

                if (column.IsInPrimaryKey)
                    this.PrimaryKeyColumns.add(column.Name);

                column.SqlDataTypeName = Helpers.GetSqlDbType(reader.getString("data_type"));
                this.Columns.add(column);
            }

            reader.close();

            Statement getIndexes = cn.createStatement();
            reader = getIndexes.executeQuery("SHOW INDEXES FROM " + this.Name);
            while (reader.next()) {
                if (IsIndexExists(reader.getString("key_name"))) {
                    GetIndexByName(reader.getString("key_name")).Columns.add(reader.getString("column_name"));
                } else {
                    DatabaseTableIndex index = new DatabaseTableIndex();
                    if (reader.getString("non_unique").equals("0"))
                        index.IsUnique = true;
                    else
                        index.IsUnique = false;
                    index.Name = reader.getString("key_name");
                    index.Columns.add(reader.getString("column_name"));

                    this.Indexes.add(index);
                }
            }
            reader.close();

            CheckReadOnlyOption();
        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "BuildTableFromMySQL() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

    private void CheckReadOnlyOption() {
        Connection cn = Database.getInstance().GetDBConnection();
        try {

            CommonTools commonTools = new CommonTools();

            if (commonTools.IsMergeTablesToSyncExists()) {

                PreparedStatement readOnlyPS = cn.prepareStatement(QUERIES.DO_SYNC_GET_TABLE());

                readOnlyPS.setString(1, Name);
                readOnlyPS.setString(2, Schema);

                ResultSet readOnlyRS = readOnlyPS.executeQuery();
                while (readOnlyRS.next()) {
                    Short readOnly = readOnlyRS.getShort("ReadOnly");
                    if (readOnly == 1)
                        ReadOnly = true;
                }
                readOnlyRS.close();
            }

        } catch (SQLException e) {
            Logs.write(Logs.Level.ERROR, "CheckReadOnlyOption() " + e.getMessage());
        } finally {
            JDBCCloser.close(cn);
        }
    }

}
