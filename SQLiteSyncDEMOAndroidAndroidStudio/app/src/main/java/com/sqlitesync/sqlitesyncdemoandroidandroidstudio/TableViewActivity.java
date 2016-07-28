package com.sqlitesync.sqlitesyncdemoandroidandroidstudio;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sqlitesync.service.SQLiteSyncCOMDBHelper;

public class TableViewActivity extends AppCompatActivity implements View.OnClickListener {
    public static void SelectTop100From(Context context, String tableName){
        Intent i = new Intent(context, TableViewActivity.class);
        i.putExtra("tableName", tableName);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);

        findViewById(R.id.btBack).setOnClickListener(this);

        String tableName = getIntent().getStringExtra("tableName");
        ((TextView) findViewById(R.id.lbTableName)).setText(tableName);
        SQLiteSyncCOMDBHelper db = new SQLiteSyncCOMDBHelper("/data/data/" + getPackageName() + "/sqlitesync.db");
        SQLiteSyncCOMDBHelper.ResultSet resultSet = db.getQueryResultSet("Select * FROM " + tableName + " LIMIT 100;");
        if(resultSet.ColumnNames.length > 0) {
            ((GridView) findViewById(R.id.gridView)).setAdapter(new GridViewAdapter(this, resultSet));
            ((GridView) findViewById(R.id.gridView)).setNumColumns(resultSet.ColumnNames.length);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btBack:
                finish();
                break;
        }
    }
}
