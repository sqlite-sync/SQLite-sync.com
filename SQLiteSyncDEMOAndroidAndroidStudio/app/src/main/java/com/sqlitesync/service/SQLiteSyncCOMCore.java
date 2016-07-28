package com.sqlitesync.service;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by konradgardocki on 15.07.2016.
 */
public class SQLiteSyncCOMCore {
    private SQLiteSyncCOMSync _syncService;
    private SQLiteSyncCOMDBHelper _dbHelper;

    private int _tablesToSyncCount = 0;

    public SQLiteSyncCOMCore(String dbFilename, String serviceUrl){
        _syncService = new SQLiteSyncCOMSync(serviceUrl);
        _dbHelper = new SQLiteSyncCOMDBHelper(dbFilename);
    }

    public void ReinitializeDB(String subscriberId) {
        ReinitializeDB(subscriberId,null);
    }

    public void ReinitializeDB(String subscriberId, final SynchronizationCallback synchronizationCallback){
        _syncService.GetFullDBSchema(subscriberId, new SQLiteSyncCOMSync.Callback() {
            @Override
            public void onFinished(Object outputObject, Exception exception) {
                if(exception == null){
                    try {
                        List<String> sortedKeys = new ArrayList<String>();
                        JSONObject jsonObject = new JSONObject(outputObject.toString());
                        boolean versionKeyFounded = false;

                        Iterator<String> iter = jsonObject.keys();
                        while (iter.hasNext())
                            sortedKeys.add(iter.next());
                        Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);

                        for(String key : sortedKeys)
                            if(key.startsWith("00000")){
                                versionKeyFounded = Integer.parseInt(jsonObject.getString(key)) >= 21;
                                break;
                            }

                        if(versionKeyFounded)
                            for(String key : sortedKeys)
                                if(!key.startsWith("00000"))
                                    _dbHelper.execSQL(jsonObject.getString(key));

                    } catch (Exception exc) {
                        exception = exc;
                    }
                }
                if(synchronizationCallback != null)
                    synchronizationCallback.onFinished(exception);
            }
        });
    }
    public void SendAndReceiveChanges(String subscriberId) {
        SendAndReceiveChanges(subscriberId,null);
    }

    public void SendAndReceiveChanges(final String subscriberId, final SynchronizationCallback synchronizationCallback){
        SQLiteSyncCOMDBHelper.ResultSet recordsToSync;
        SQLiteSyncCOMDBHelper.ResultSet tablesToSync = _dbHelper.getQueryResultSet("select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';");

        StringBuilder sqlitesync_SyncDataToSend = new StringBuilder();
        sqlitesync_SyncDataToSend.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">");
        for(SQLiteSyncCOMDBHelper.ResultRow table : tablesToSync.Rows){
            String tableName = table.getCellStringValue("tbl_Name");

            if(!tableName.equalsIgnoreCase("MergeDelete") && !tableName.equalsIgnoreCase("MergeIdentity")){
                sqlitesync_SyncDataToSend.append(String.format("<tab n=\"%1$s\">",tableName));

                sqlitesync_SyncDataToSend.append("<ins>");
                recordsToSync = _dbHelper.getQueryResultSet(String.format("select * from %1$s where RowId is null;",tableName));
                for(SQLiteSyncCOMDBHelper.ResultRow record : recordsToSync.Rows){
                    sqlitesync_SyncDataToSend.append("<r>");
                    for(SQLiteSyncCOMDBHelper.ResultCell cell : record.Cells){
                        if(!cell.ColumnName.equalsIgnoreCase("MergeUpdate")){
                            sqlitesync_SyncDataToSend.append(String.format("<%1$s>",cell.ColumnName));
                            sqlitesync_SyncDataToSend.append(String.format("<![CDATA[%1$s]]>",cell.Value));
                            sqlitesync_SyncDataToSend.append(String.format("</%1$s>",cell.ColumnName));
                        }
                    }
                    sqlitesync_SyncDataToSend.append("</r>");
                }
                sqlitesync_SyncDataToSend.append("</ins>");

                sqlitesync_SyncDataToSend.append("<upd>");
                recordsToSync = _dbHelper.getQueryResultSet(String.format("select * from %1$s where MergeUpdate > 0 and RowId is not null;",tableName));
                for(SQLiteSyncCOMDBHelper.ResultRow record : recordsToSync.Rows){
                    sqlitesync_SyncDataToSend.append("<r>");
                    for(SQLiteSyncCOMDBHelper.ResultCell cell : record.Cells){
                        if(!cell.ColumnName.equalsIgnoreCase("MergeUpdate")){
                            sqlitesync_SyncDataToSend.append(String.format("<%1$s>",cell.ColumnName));
                            sqlitesync_SyncDataToSend.append(String.format("<![CDATA[%1$s]]>",cell.Value));
                            sqlitesync_SyncDataToSend.append(String.format("</%1$s>",cell.ColumnName));
                        }
                    }
                    sqlitesync_SyncDataToSend.append("</r>");
                }
                sqlitesync_SyncDataToSend.append("</upd>");

                sqlitesync_SyncDataToSend.append("</tab>");
            }
        }

        sqlitesync_SyncDataToSend.append("<delete>");
        recordsToSync = _dbHelper.getQueryResultSet("select * from MergeDelete;");
        for(SQLiteSyncCOMDBHelper.ResultRow record : recordsToSync.Rows){
            sqlitesync_SyncDataToSend.append("<r>");
            sqlitesync_SyncDataToSend.append(String.format("<tb>%1$s</tb>",record.getCellStringValue("TableId")));
            sqlitesync_SyncDataToSend.append(String.format("<id>%1$s</id>",record.getCellStringValue("RowId")));
            sqlitesync_SyncDataToSend.append("</r>");
        }
        sqlitesync_SyncDataToSend.append("</delete>");

        sqlitesync_SyncDataToSend.append("</SyncData>");

        _syncService.ReceiveData(subscriberId, sqlitesync_SyncDataToSend.toString(), new SQLiteSyncCOMSync.Callback() {
            @Override
            public void onFinished(Object outputObject, Exception exception) {
                if(exception != null && synchronizationCallback != null)
                    synchronizationCallback.onFinished(exception);

                SQLiteSyncCOMDBHelper.ResultSet tablesToSync = _dbHelper.getQueryResultSet("select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';");
                for(SQLiteSyncCOMDBHelper.ResultRow table : tablesToSync.Rows) {
                    String tableName = table.getCellStringValue("tbl_Name");
                    if(!tableName.equalsIgnoreCase("MergeDelete") && !tableName.equalsIgnoreCase("MergeIdentity")) {
                        SQLiteSyncCOMDBHelper.ResultSet recordsToSync = _dbHelper.getQueryResultSet(String.format("select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_%1$s'",tableName));
                        String updTriggerSQL = recordsToSync.Rows.get(0).getCellStringValue("sql");
                        _dbHelper.execSQL(String.format("drop trigger trMergeUpdate_%1$s;",tableName));
                        _dbHelper.execSQL(String.format("update %1$s set MergeUpdate=0 where MergeUpdate > 0;",tableName));
                        _dbHelper.execSQL(updTriggerSQL);
                    }
                }
                _dbHelper.execSQL("delete from MergeDelete");

                tablesToSync = _dbHelper.getQueryResultSet("select tbl_Name from sqlite_master where type='table'");
                _tablesToSyncCount = tablesToSync.Rows.size();
                for(SQLiteSyncCOMDBHelper.ResultRow table : tablesToSync.Rows){
                    String tableName = table.getCellStringValue("tbl_Name");
                    _syncService.GetDataForSync(subscriberId, table.getCellStringValue("tbl_Name"), new SQLiteSyncCOMSync.Callback() {
                        @Override
                        public void onFinished(Object outputObject, Exception exception) {
                            if(exception != null && synchronizationCallback != null)
                                synchronizationCallback.onFinished(exception);

                            SQLiteSyncDataObject.SQLiteSyncDataRecord[] Records;
                            SQLiteSyncDataObject[] Data = SQLiteSyncDataObject.arrayFromObjectString(outputObject);

                            for(SQLiteSyncDataObject dataObject : Data){
                                if(dataObject.SyncId > 0) {
                                    _dbHelper.execSQL(dataObject.TriggerInsertDrop);
                                    _dbHelper.execSQL(dataObject.TriggerUpdateDrop);
                                    _dbHelper.execSQL(dataObject.TriggerDeleteDrop);

                                    Records = dataObject.getSQLiteSyncDataRecordArray();
                                    for(SQLiteSyncDataObject.SQLiteSyncDataRecord record : Records){
                                        switch (record.Action){
                                            case 1:
                                                _dbHelper.execSQL(dataObject.QueryInsert, record.Columns);
                                                break;
                                            case 2:
                                                _dbHelper.execSQL(dataObject.QueryUpdate, record.Columns);
                                                break;
                                            case 3:
                                                _dbHelper.execSQL(dataObject.QueryDelete + "?", record.Columns);
                                                break;
                                        }
                                    }

                                    _dbHelper.execSQL(dataObject.TriggerInsert);
                                    _dbHelper.execSQL(dataObject.TriggerUpdate);
                                    _dbHelper.execSQL(dataObject.TriggerDelete);

                                    _syncService.SyncCompleted(dataObject.SyncId, new SQLiteSyncCOMSync.Callback() {
                                        @Override
                                        public void onFinished(Object outputObject, Exception exception) {
                                            if(--_tablesToSyncCount == 0){
                                                if(synchronizationCallback != null)
                                                    synchronizationCallback.onFinished(exception);
                                            }
                                        }
                                    });
                                }
                                else if(--_tablesToSyncCount == 0){
                                    if(synchronizationCallback != null)
                                        synchronizationCallback.onFinished(exception);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public interface SynchronizationCallback {
        void onFinished(Exception exception);
    }
}
