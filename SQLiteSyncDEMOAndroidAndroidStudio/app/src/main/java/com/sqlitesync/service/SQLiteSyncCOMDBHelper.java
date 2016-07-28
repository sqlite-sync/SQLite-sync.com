package com.sqlitesync.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.openDatabase;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

/**
 * Created by konradgardocki on 15.07.2016.
 */
public class SQLiteSyncCOMDBHelper {
    private SQLiteDatabase _db;

    public SQLiteSyncCOMDBHelper(String dbFileName){
        _db = openOrCreateDatabase(dbFileName,null);
    }

    public void execSQL(String query){
        if(query.length() <= 0) return;

        _db.execSQL(query);
    }

    public void execSQL(String query, Object[] parameters){
        if(query.length() <= 0) return;

        try {
            _db.execSQL(query,parameters);
        }catch (SQLiteException exc){
            exc.printStackTrace();
        }
    }

    public void execSQL(String query, JSONArray jsonParameters) {
        List<String> parameters = new ArrayList<String>();
        for(int i = 0; i < jsonParameters.length(); i++){
            try {
                parameters.add(jsonParameters.getString(i));
            } catch (JSONException e) {}
        }
        this.execSQL(query,parameters.toArray());
    }

    public ResultSet getQueryResultSet(String query){
        if(query.length() <= 0) return new ResultSet();

        ResultSet resultSet = new ResultSet();
        ResultRow row;
        String columnName,cellValue;
        Cursor cursor = null;

        try {
            cursor = _db.rawQuery(query, null);
            resultSet.ColumnNames = cursor.getColumnNames();
            if (cursor.moveToFirst()) {
                do {
                    row = new ResultRow();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        columnName = cursor.getColumnName(i);
                        cellValue = cursor.getString(i);
                        row.Cells.add(new ResultCell(columnName, cellValue));
                    }
                    resultSet.Rows.add(row);
                } while (cursor.moveToNext());
            }
        }catch (SQLiteException exc){
            exc.printStackTrace();
        }finally {
            if(cursor != null) cursor.close();
        }

        return resultSet;
    }

    public class ResultSet{
        public List<ResultRow> Rows;
        public String[] ColumnNames;

        public ResultSet(){
            Rows = new ArrayList<ResultRow>();
        }
    }

    public class ResultCell{
        public String ColumnName;
        public Object Value;

        public ResultCell(String columnName, Object value){
            ColumnName = columnName;
            Value = value;
        }
    }

    public class ResultRow{
        public List<ResultCell> Cells;

        public ResultRow(){
            Cells = new ArrayList<ResultCell>();
        }

        public Object getCellValue(String columnName){
            for(ResultCell cell: Cells){
                if(cell.ColumnName.equalsIgnoreCase(columnName)) return  cell.Value;
            }
            return null;
        }

        public String getCellStringValue(String columnName) {
            return getCellValue(columnName).toString();
        }
    }
}
