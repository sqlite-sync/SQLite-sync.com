package com.sqlite_sync;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

/**
 * SQLiteSync class
 * @author sqlite-sync.com
 */
public class SQLiteSync {
    private String _serverURL;
    private String _dbFileName;

    /**
     * Interface used as callback from asynchronous methods.
     */
    public interface SQLiteSyncCallback{
        /**
         * Interface method executed when action finished successfull
         */
        void onSuccess();

        /**
         * Interface method executed when action finished with error
         * @param error exception encountered during action
         */
        void onError(Exception error);
    }

    /**
     * Create instance of SQLiteSync class
     * @param dbFileName path to local sqlite database file
     * @param serverURL synchronization webservice url
     */
    public SQLiteSync(String dbFileName, String serverURL){
        _serverURL = serverURL;
        _dbFileName = dbFileName;
    }

    //region Asynchronous methods

    /**
     * Asynchronously recreate database schema from remote server for specific subscriber
     * @param subscriberId id of subscriber
     * @param callback callback to be invoke after function complete
     */
    public void initializeSubscriber(@NonNull final String subscriberId, @NonNull final SQLiteSyncCallback callback){
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected Exception doInBackground(Void... voids) {
                try {
                    initializeSubscriber(subscriberId);
                    return null;
                }
                catch (Exception exception) {
                    return exception;
                }
            }

            protected void onPostExecute(Exception exception) {
                if(exception == null){
                    callback.onSuccess();
                }
                else{
                    callback.onError(exception);
                }
            }
        }.execute();
    }

    /**
     * Asynchronously send and receive any changes made for tables included in synchronization
     * @param subscriberId id of subscriber
     * @param callback callback to be invoke after function complete
     */
    public void synchronizeSubscriber(@NonNull final String subscriberId, @NonNull final SQLiteSyncCallback callback){
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected Exception doInBackground(Void... voids) {
                try{
                    synchronizeSubscriber(subscriberId);
                    return null;
                }
                catch (Exception exception){
                    return exception;
                }
            }

            protected void onPostExecute(Exception exception) {
                if(exception == null){
                    callback.onSuccess();
                }
                else{
                    callback.onError(exception);
                }
            }
        }.execute();
    }

    /**
     * Asynchronously add table to synchronization
     * @param tableName table name
     * @param callback callback to be invoke after function complete
     */
    public void addSynchrnizedTable(@NonNull final String tableName, @NonNull final SQLiteSyncCallback callback){
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected Exception doInBackground(Void... voids) {
                try {
                    addSynchrnizedTable(tableName);
                    return null;
                }
                catch (Exception exception){
                    return exception;
                }
            }

            protected void onPostExecute(Exception exception) {
                if(exception == null){
                    callback.onSuccess();
                }
                else{
                    callback.onError(exception);
                }
            }
        }.execute();
    }

    /**
     * Asynchronously remove table from synchronization
     * @param tableName table name
     * @param callback callback to be invoke after function complete
     */
    public void removeSynchrnizedTable(@NonNull final String tableName, @NonNull final SQLiteSyncCallback callback){
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected Exception doInBackground(Void... voids) {
                try {
                    removeSynchrnizedTable(tableName);
                    return null;
                }
                catch (Exception exception){
                    return exception;
                }
            }

            protected void onPostExecute(Exception exception) {
                if(exception == null){
                    callback.onSuccess();
                }
                else{
                    callback.onError(exception);
                }
            }
        }.execute();
    }
    //endregion

    //region Synchronous methods

    /**
     * Recreate database schema from remote server for specific subscriber
     * @param subscriberId id of subscriber
     * @throws Exception
     */
    public void initializeSubscriber(String subscriberId) throws Exception {
        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;
        Map<String, String> schema = null;

        String requestUrl = String.format("%s/InitializeSubscriber/%s", _serverURL, subscriberId);

        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    schema = new Gson().fromJson(resultString, new TypeToken<Map<String, String>>(){}.getType());
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        List<String> keys = new ArrayList<String>();
        Set<String> keySet = schema.keySet();
        for(String key : keySet){
            keys.add(key);
        }
        Collections.sort(keys);

        SQLiteDatabase db = openOrCreateDatabase(_dbFileName, null);
        try{
            db.beginTransaction();
            for (String key : keys) {
                if(!key.startsWith("00000")){
                    db.execSQL(schema.get(key));
                }
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Send and receive any changes made for tables included in synchronization
     * @param subscriberId id of subscriber
     * @throws Exception
     */
    public void synchronizeSubscriber(String subscriberId) throws Exception {
        sendLocalChanges(subscriberId);
        clearChangesMarker();

        List<String> tables = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try{
            tables = new ArrayList<String>();
            db = openOrCreateDatabase(_dbFileName, null);
            cursor = db.rawQuery("select tbl_Name from sqlite_master where type='table' and tbl_Name != 'android_metadata'", null);
            while (cursor.moveToNext()){
                tables.add(cursor.getString(0));
            }
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
            if(db != null && db.isOpen()){
                db.close();
            }
        }

        for(String tableName : tables){
            getRemoteChangesForTable(subscriberId, tableName);
        }
    }

    /**
     * Add table to synchronization
     * @param tableName table name
     * @throws Exception
     */
    public void addSynchrnizedTable(String tableName) throws Exception {
        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;

        String requestUrl = String.format("%s/AddTable/%s", _serverURL, tableName);

        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Remove table from synchronization
     * @param tableName table name
     * @throws Exception
     */
    public void removeSynchrnizedTable(String tableName) throws Exception {
        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;

        String requestUrl = String.format("%s/RemoveTable/%s", _serverURL, tableName);

        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    //endregion

    //region private Helpers

    /**
     * Send local changes to webservice
     * @param subscriberId id of subscriber
     * @throws Exception
     */
    private void sendLocalChanges(String subscriberId) throws Exception {
        String query;
        Cursor cursor = null;
        SQLiteDatabase db = null;

        StringBuilder builder = new StringBuilder();

        try {
            db = openOrCreateDatabase(_dbFileName, null);

            List<String> tables = new ArrayList<String>();
            query = "select tbl_Name from sqlite_master where type='table' and sql like '%RowId%'  and tbl_Name != 'android_metadata';";
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }
            cursor.close();

            builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">");

            for (String tableName : tables) {
                if (!tableName.equalsIgnoreCase("MergeDelete")) {
                    builder.append(String.format("<tab n=\"%1$s\">", tableName));

                    builder.append("<ins>");
                    query = String.format("select * from %1$s where RowId is null;", tableName);
                    cursor = db.rawQuery(query, null);
                    while (cursor.moveToNext()) {
                        builder.append("<r>");
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            if (!cursor.getColumnName(i).equalsIgnoreCase("MergeUpdate")) {
                                builder.append(
                                        String.format("<%1$s><![CDATA[%2$s]]></%1$s>",
                                                cursor.getColumnName(i),
                                                cursor.getString(i)));
                            }
                        }
                        builder.append("</r>");
                    }
                    cursor.close();
                    builder.append("</ins>");

                    builder.append("<upd>");
                    query = String.format("select * from %1$s where MergeUpdate > 0 and RowId is not null;", tableName);
                    cursor = db.rawQuery(query, null);
                    while (cursor.moveToNext()) {
                        builder.append("<r>");
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            if (!cursor.getColumnName(i).equalsIgnoreCase("MergeUpdate")) {
                                builder.append(String.format("<%1$s><![CDATA[%2$s]]></%1$s>",
                                                cursor.getColumnName(i),
                                                cursor.getString(i)));
                            }
                        }
                        builder.append("</r>");
                    }
                    cursor.close();
                    builder.append("</upd>");

                    builder.append("</tab>");
                }
            }

            builder.append("<delete>");
            query = String.format("select TableId,RowId from MergeDelete;");
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                builder.append(
                        String.format("<r><tb>%1$s</tb><id>%2$s</id></r>",
                                cursor.getString(0),
                                cursor.getString(1)));
            }
            cursor.close();
            builder.append("</delete>");

            builder.append("</SyncData>");
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
            if(db != null && db.isOpen()){
                db.close();
            }
        }

        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;

        String requestUrl = String.format("%s/Send", _serverURL);
        JSONObject inputObject = new JSONObject();

        try {
            inputObject.put("subscriber", subscriberId);
            inputObject.put("content", builder.toString());
            inputObject.put("version", "3");
            byte[] requestBytes = inputObject.toString().getBytes("UTF-8");

            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(requestBytes);
            wr.flush();
            wr.close();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    break;
                case HttpURLConnection.HTTP_NO_CONTENT:
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) { }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Clear updated & deleted records marker
     */
    private void clearChangesMarker(){
        String query;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try{
            db = openOrCreateDatabase(_dbFileName, null);

            List<String> tables = new ArrayList<String>();
            query = "select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';";
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()){
                tables.add(cursor.getString(0));
            }
            cursor.close();

            db.beginTransaction();
            for(String tableName : tables){
                if(tableName.equalsIgnoreCase("MergeIdentity")) {
                    db.execSQL(String.format("update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;"));
                }

                if(!tableName.equalsIgnoreCase("MergeDelete") && !tableName.equalsIgnoreCase("MergeIdentity")) {
                    String updTriggerSQL = null;

                    query = String.format("select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_%1$s'", tableName);
                    cursor = db.rawQuery(query, null);
                    if(cursor.moveToFirst()){
                        updTriggerSQL = cursor.getString(0);
                    }
                    cursor.close();

                    if(updTriggerSQL != null){
                        db.execSQL(String.format("drop trigger trMergeUpdate_%1$s;", tableName));
                        db.execSQL(String.format("update %1$s set MergeUpdate=0 where MergeUpdate > 0;", tableName));
                        db.execSQL(updTriggerSQL);
                    }
                }
            }

            db.execSQL("delete from MergeDelete");
            db.setTransactionSuccessful();
        }
        finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
            if(db != null && db.isOpen()){
                if(db.inTransaction()){
                    db.endTransaction();
                }
                db.close();
            }
        }
    }

    /**
     * Get changes for table from remote server for specific subscriber
     * @param subscriberId id of subscriber
     * @param tableName table name
     * @throws Exception
     */
    private void getRemoteChangesForTable(String subscriberId, String tableName) throws Exception {
        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;

        String requestUrl = String.format("%s/Sync/%s/%s", _serverURL, subscriberId, tableName);

        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        SQLiteSyncData[] syncDatas = new Gson().fromJson(resultString, SQLiteSyncData[].class);

        for(SQLiteSyncData syncData : syncDatas){
            if(syncData.SyncId > 0) {
                SQLiteDatabase db = null;

                try{
                    db = openOrCreateDatabase(_dbFileName, null);
                    db.beginTransaction();

                    if(syncData.TriggerInsertDrop.length() > 0){
                        db.execSQL(syncData.TriggerInsertDrop);
                    }
                    if(syncData.TriggerUpdateDrop.length() > 0){
                        db.execSQL(syncData.TriggerUpdateDrop);
                    }
                    if(syncData.TriggerDeleteDrop.length() > 0){
                        db.execSQL(syncData.TriggerDeleteDrop);
                    }

                    SQLiteSyncDataRecord[] records = syncData.getSQLiteSyncDataRecords();

                    for(SQLiteSyncDataRecord record : records){
                        switch (record.Action){
                            case 1:
                                db.execSQL(syncData.QueryInsert, record.Columns);
                                break;
                            case 2:
                                db.execSQL(syncData.QueryUpdate, record.Columns);
                                break;
                            case 3:
                                db.execSQL(syncData.QueryDelete + "?", record.Columns);
                                break;
                        }
                    }

                    if(syncData.TriggerInsert.length() > 0){
                        db.execSQL(syncData.TriggerInsert);
                    }
                    if(syncData.TriggerUpdate.length() > 0){
                        db.execSQL(syncData.TriggerUpdate);
                    }
                    if(syncData.TriggerDelete.length() > 0){
                        db.execSQL(syncData.TriggerDelete);
                    }

                    db.setTransactionSuccessful();
                }
                finally {
                    if(db != null && db.isOpen()){
                        if(db.inTransaction()){
                            db.endTransaction();
                        }
                        db.close();
                    }
                }

                commitSynchronization(syncData.SyncId);
            }
        }
    }

    /**
     * Send info to remote server about successful single table synchronization
     * @param syncId id of synchronization
     * @throws Exception
     */
    private void commitSynchronization(@NonNull int syncId) throws Exception {
        HttpURLConnection connection = null;
        InputStream resultStream = null;
        String resultString = null;

        String requestUrl = String.format("%s/CommitSync/%s", _serverURL, syncId);

        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();

            int status = connection.getResponseCode();

            switch (status){
                case HttpURLConnection.HTTP_OK:
                    resultStream = connection.getInputStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    break;
                default:
                    resultStream = connection.getErrorStream();
                    resultString = IOUtils.toString(resultStream, "UTF-8");
                    throw new Exception(resultString);
            }
        }
        finally {
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Class representing data structure of table changes received from remote server
     */
    private class SQLiteSyncData{
        private String Records;
        public String TableName;

        public String QueryInsert;
        public String QueryUpdate;
        public String QueryDelete;

        public String TriggerInsert;
        public String TriggerUpdate;
        public String TriggerDelete;

        public String TriggerInsertDrop;
        public String TriggerUpdateDrop;
        public String TriggerDeleteDrop;

        public int SyncId;
        public String SQLiteSyncVersion;

        public SQLiteSyncDataRecord[] getSQLiteSyncDataRecords() throws Exception {
            List<SQLiteSyncDataRecord> dataRecords = new ArrayList<>();

            Object recordsObject = XML.toJSONObject(Records).getJSONObject("records").get("r");

            JSONArray recordsJsonArray = new JSONArray();

            if(recordsObject instanceof JSONArray){
                recordsJsonArray = (JSONArray) recordsObject;
            }
            else if(recordsObject instanceof JSONObject) {
                recordsJsonArray.put(recordsObject);
            }

            for (int i = 0; i < recordsJsonArray.length(); i++) {
                JSONObject recordObject = recordsJsonArray.getJSONObject(i);
                int action = recordObject.getInt("a");
                Object columnsObject = recordObject.get("c");

                JSONArray columnsJsonArray = new JSONArray();
                if(columnsObject instanceof JSONArray) {
                    columnsJsonArray = (JSONArray) columnsObject;
                }
                else {
                    columnsJsonArray.put(columnsObject);
                }

                String[] columnsArray = new String[columnsJsonArray.length()];
                for(int j = 0; j < columnsJsonArray.length(); j++) {
                    columnsArray[j] = columnsJsonArray.getString(j);
                }

                dataRecords.add(new SQLiteSyncDataRecord(action, columnsArray));
            }

            return dataRecords.toArray(new SQLiteSyncDataRecord[dataRecords.size()]);
        }
    }

    /**
     * Class representing data structure of record changes received from remote server
     */
    private class SQLiteSyncDataRecord{
        public int Action;
        public String[] Columns;

        public SQLiteSyncDataRecord(int action,String[] columns){
            Action = action;
            Columns = columns;
        }
    }
    //endregion
}
