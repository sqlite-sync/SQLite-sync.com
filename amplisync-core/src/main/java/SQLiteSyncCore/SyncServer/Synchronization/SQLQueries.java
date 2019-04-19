package SQLiteSyncCore.SyncServer.Synchronization;

import SQLiteSyncCore.SQLiteSyncConfig;

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

public class SQLQueries {


    public static String GET_ROWID_COLUMN_NAME() {
        String rowIdColname = "RowId";
        return rowIdColname;
    }

    public String GET_MERGE_TABLES_TO_SYNC() {
        String val = "";
        val = "select * from MergeTablesToSync";
        return val;
    }

    public String DO_SYNC_GET_TABLE() {
        String val = "";
        val = "select * from MergeTablesToSync where TableName=? and TableSchema=?";
        return val;

    }

    public String DO_SYNC_GET_TABLE_SCHEMA() {
        String val = "";
        val = "select * from MergeTablesToSync where TableName=?";
        return val;
    }

    public String START_NEW_SYNC() {
        String val = "";
        val = "insert into MergeSync (SubscriberId, SyncObject, TableId) values (?,?,?);";// SELECT LAST_INSERT_ID();
        return val;
    }

    public String COMMIT_SYNC() {
        String val = "";
        val = "select * from MergeTablesToSync Where TableId=(select TableId from MergeSync where SyncId=?)";
        return val;
    }

    public String COMMIT_SYNC_UPDATE() {
        String val = "";
        val = "update MergeSync set SyncEnd=now() where SyncId=?";
        return val;
    }

    public String UPDATE_SYNC_DATA_UPDATE(String tableName) {
        String val = "";
        val = "update MergeContent_" + tableName + " set RowVer=?, TableId=?, Action=2, ChangeDate=now() where RowId=? and SubscriberId=?";
        return val;
    }

    public String UPDATE_SYNC_DATA_DELETE(String tableName) {
        String val = "";
        val = "delete from MergeContent_" + tableName + " where RowId=? and SubscriberId=?";
        return val;
    }

    public String INSERT_MERGE_CONTENT(String tableName) {
        String val = "";
        val = "insert into MergeContent_" + tableName + " (TableId,SubscriberId,RowId,RowVer,ChangeDate,Action,SyncId) values (?,?,?,?,?,?,?)";
        return val;
    }

    public String TABLES_LIST() {
        String val = "";
        val = "SELECT * FROM information_schema.tables WHERE table_schema = DATABASE() and table_type='BASE TABLE' order by TABLE_NAME asc";
        return val;
    }

    public String GET_SUBSCRIBERS_LIST() {
        String val = "";
        val = "select * from MergeSubscribers order by Name";
        return val;
    }

    public String INSERT_SUBSCRIBER() {
        String val = "";
        val = "INSERT INTO MergeSubscribers(Name,UniqueName,NeedReinitialization)VALUES(?,?,0)";
        return val;
    }

    public String UPDATE_SUBSCRIBER() {
        String val = "";
        val = "update MergeSubscribers set Name=?,UniqueName=? where subscriberId=?";
        return val;
    }

    public String INSERT_MERGEIDENT_MERGETABLETOSYNC() {
        String val = "";
        val = "INSERT INTO MergeTablesToSync (TableName,TableSchema,TableFilter,IdentityRange,IdentityTrashold)VALUES('MergeIdentity',?,'',5000,80)";
        return val;
    }

    public String CLEAR_MERGE_CONTENT_BY_SUBSCRIBER(String tableName) {
        String val = "";
        val = "delete from MergeContent_" + tableName + " where SubscriberId=?";
        return val;
    }
}
