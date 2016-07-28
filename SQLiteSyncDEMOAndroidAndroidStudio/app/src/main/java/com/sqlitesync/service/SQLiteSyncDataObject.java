package com.sqlitesync.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by konradgardocki on 14.07.2016.
 */
public class SQLiteSyncDataObject {
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

    public SQLiteSyncDataRecord[] getSQLiteSyncDataRecordArray(){
        Object recordsObject;
        JSONArray recordsJsonArray = new JSONArray();
        SQLiteSyncDataRecord[] recordsArray = new SQLiteSyncDataRecord[0];
        JSONObject recordObject;
        Object columnsObject;
        JSONArray columnsJsonArray;
        String[] columnsArray;
        int action;
        try {
            recordsObject = XML.toJSONObject(Records).getJSONObject("records").get("r");
            if(recordsObject instanceof JSONArray)
                recordsJsonArray = (JSONArray) recordsObject;
            else if(recordsObject instanceof JSONObject)
                recordsJsonArray.put(recordsObject);
            recordsArray = new SQLiteSyncDataRecord[recordsJsonArray.length()];
            for (int i = 0; i < recordsJsonArray.length(); i++) {
                recordObject = recordsJsonArray.getJSONObject(i);
                action = recordObject.getInt("a");
                columnsObject = recordObject.get("c");
                columnsJsonArray = new JSONArray();
                if(columnsObject instanceof JSONArray)
                    columnsJsonArray = (JSONArray) columnsObject;
                else
                    columnsJsonArray.put(columnsObject);
                columnsArray = new String[columnsJsonArray.length()];
                for(int j = 0; j < columnsJsonArray.length(); j++)
                    columnsArray[j] = columnsJsonArray.getString(j);
                recordsArray[i] = new SQLiteSyncDataRecord(action,columnsArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return recordsArray;
    }

    public static SQLiteSyncDataObject[] arrayFromObjectString(Object value){
        SQLiteSyncDataObject[] Data = new Gson().fromJson(value.toString(), SQLiteSyncDataObject[].class);
        return Data;
    }

    public class SQLiteSyncDataRecord{
        public int Action;
        public String[] Columns;
        public SQLiteSyncDataRecord(int action,String[] columns){
            Action = action;
            Columns = columns;
        }
    }
}