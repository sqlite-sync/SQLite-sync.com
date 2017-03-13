package com.sqlite_sync.demo;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TableFormActivity extends AppCompatActivity {

    public static void Edit(Context context, String tableName, String rowId){
        Intent i = new Intent(context, TableFormActivity.class);
        i.putExtra("tableName", tableName);
        i.putExtra("rowId", rowId);
        context.startActivity(i);
    }

    public static void Add(Context context, String tableName){
        Intent i = new Intent(context, TableFormActivity.class);
        i.putExtra("tableName", tableName);
        context.startActivity(i);
    }

    private String _tableName;
    private String _rowId;
    private List<TableColumn> _tableColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_form);

        _tableName = getIntent().getStringExtra("tableName");
        _rowId = getIntent().getStringExtra("rowId");

        ((TextView)findViewById(R.id.lbFormTitle))
                .setText(String.format("%s [%s]", _rowId == null ? "INSERT INTO" : "UPDATE", _tableName));

        findViewById(R.id.btFormSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    insertUpdateRecord();
                    finish();
                }
                catch (Exception exc){
                    showMessage(exc.getLocalizedMessage());
                }
            }
        });

        findViewById(R.id.btFormBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadForm();
    }

    private void loadForm(){
        _tableColumns = new ArrayList<>();

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try{
            db = openOrCreateDatabase("/data/data/" + getPackageName() + "/sqlitesync.db", 1, null);
            cursor = db.rawQuery(String.format("PRAGMA table_info(%s);", _tableName), null);

            int nameIdx = cursor.getColumnIndexOrThrow("name");
            int typeIdx = cursor.getColumnIndexOrThrow("type");

            while (cursor.moveToNext()){
                String name = cursor.getString(nameIdx);
                if(!name.equalsIgnoreCase("RowId") && !name.equalsIgnoreCase("MergeUpdate")){
                    _tableColumns.add(new TableColumn(name, cursor.getString(typeIdx)));
                }
            }

            if(_rowId != null){
                cursor.close();

                StringBuilder builder = new StringBuilder();
                builder.append("SELECT ");

                for(int i = 0; i < _tableColumns.size(); i++){
                    if(i != 0){
                        builder.append(",");
                    }
                    builder.append(_tableColumns.get(i).getName());
                }

                builder.append(" FROM ");
                builder.append(_tableName);
                builder.append(" WHERE RowId = ?;");

                cursor = db.rawQuery(builder.toString(), new String[]{ _rowId });

                if (cursor.moveToFirst()){
                    for(int i = 0; i < _tableColumns.size(); i++){
                        _tableColumns.get(i).setValue(cursor.getString(i));
                    }
                }
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

        LinearLayout form = (LinearLayout)findViewById(R.id.form_container);

        int index = -1;

        for(TableColumn column : _tableColumns){
            TextView label = new TextView(this);
            EditText editText = new EditText(this);

            label.setText(column.getName());
            editText.setText(column.getValue());

            if(column.getType().equalsIgnoreCase("INTEGER")){
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }

            form.addView(label, ++index);
            form.addView(editText, ++index);
        }
    }

    private void insertUpdateRecord(){
        SQLiteDatabase db = null;
        LinearLayout form = (LinearLayout)findViewById(R.id.form_container);
        ContentValues values = new ContentValues();

        for(int i = 0; i < _tableColumns.size(); i++){
            String value = ((EditText)form.getChildAt(i * 2 + 1)).getText().toString();
            if(value.length() > 0) {
                values.put(_tableColumns.get(i).getName(), value.trim());
            }
        }

        try{
            db = openOrCreateDatabase("/data/data/" + getPackageName() + "/sqlitesync.db", 1, null);

            if(_rowId == null){
                db.insertOrThrow(_tableName, null, values);
            }
            else{
                db.update(_tableName, values, "RowId = ?", new String[]{ _rowId });
            }
        }
        finally {
            if(db != null && db.isOpen()){
                db.close();
            }
        }
    }

    private void showMessage(String message){
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle("SQLite-sync Demo");
        dlgAlert.setCancelable(false);
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dlgAlert.create().show();
    }

    private class TableColumn {
        private String _name;
        private String _type;
        private String _value;

        public TableColumn(String name, String type){
            _name = name;
            _type = type;
        }

        public String getName(){
            return _name;
        }

        public String getType(){
            return _type;
        }

        public void setValue(String value){
            _value = value;
        }
        public String getValue(){
            return _value;
        }
    }
}
