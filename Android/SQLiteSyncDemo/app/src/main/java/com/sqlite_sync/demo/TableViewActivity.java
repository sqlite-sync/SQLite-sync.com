package com.sqlite_sync.demo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TableViewActivity extends AppCompatActivity implements View.OnClickListener, GridViewAdapter.OnRowActionListener {

    private String _tableName;

    public static void SelectFrom(Context context, String tableName){
        Intent i = new Intent(context, TableViewActivity.class);
        i.putExtra("tableName", tableName);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);

        findViewById(R.id.btBack).setOnClickListener(this);
        findViewById(R.id.btAddRecord).setOnClickListener(this);

        _tableName = getIntent().getStringExtra("tableName");
        ((TextView) findViewById(R.id.lbTableName)).setText(_tableName);
    }

    @Override
    public void onResume(){
        super.onResume();
        loadGridView();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btBack:
                finish();
                break;
            case R.id.btAddRecord:
                TableFormActivity.Add(this, _tableName);
                break;
        }
    }

    @Override
    public void onEdit(String rowId) {
        TableFormActivity.Edit(this, _tableName, rowId);
    }

    @Override
    public void onDelete(final String rowId) {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        deleteRecord(rowId);
                        loadGridView();
                    }
                    catch (Exception exc){
                        showMessage(exc.getLocalizedMessage());
                    }
                }
            })
            .setNegativeButton("No", null)
            .show();
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

    private void loadGridView() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String[] columns = new String[0];
        List<String[]> rows = new ArrayList<>();

        try{
            db = openOrCreateDatabase("/data/data/" + getPackageName() + "/sqlitesync.db", 1, null);
            cursor = db.rawQuery(String.format("Select * FROM %s;", _tableName), null);

            columns = cursor.getColumnNames();

            while (cursor.moveToNext()){
                List<String> row = new ArrayList<>();
                for(int i = 0; i < cursor.getColumnCount(); i++){
                    row.add(cursor.getString(i));
                }
                rows.add(row.toArray(new String[row.size()]));
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

        ((GridView) findViewById(R.id.gridView)).setAdapter(new GridViewAdapter(this, columns, rows.toArray(new String[rows.size()][]), this));
        ((GridView) findViewById(R.id.gridView)).setNumColumns(columns.length + 1);
    }

    private void deleteRecord(String rowId){
        SQLiteDatabase db = null;

        try{
            db = openOrCreateDatabase("/data/data/" + getPackageName() + "/sqlitesync.db", 1, null);
            db.delete(_tableName, "RowId = ?", new String[]{ rowId });
        }
        finally {
            if(db != null && db.isOpen()){
                db.close();
            }
        }
    }
}
