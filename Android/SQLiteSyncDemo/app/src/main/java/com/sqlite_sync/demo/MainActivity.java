package com.sqlite_sync.demo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.sqlite_sync.SQLiteSync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        ((TextView)findViewById(R.id.tbSqlite_sync_url)).setText(preferences.getString("sqlite_sync_url", "http://demo.sqlite-sync.com:8081/SqliteSync/API3"));

        findViewById(R.id.btReinitialize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressBar();
                final Button button = (Button)view;
                button.setEnabled(false);

                String sqlite_sync_url = ((TextView)findViewById(R.id.tbSqlite_sync_url)).getText().toString();
                SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("sqlite_sync_url", sqlite_sync_url);

                SQLiteSync sqLite_sync = new SQLiteSync("/data/data/" + getPackageName() + "/sqlitesync.db",
                        sqlite_sync_url);

                sqLite_sync.initializeSubscriber(getSubscriberId(), new SQLiteSync.SQLiteSyncCallback() {
                    @Override
                    public void onSuccess() {
                        hideProgressBar();
                        button.setEnabled(true);
                        showMessage("Initialization finished successfully");
                    }

                    @Override
                    public void onError(Exception error) {
                        hideProgressBar();
                        button.setEnabled(true);
                        error.printStackTrace();
                        showMessage("Initialization finished with error: \n" + error.getMessage());
                    }
                });
            }
        });

        findViewById(R.id.btSendAndReceive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressBar();
                final Button button = (Button)view;
                button.setEnabled(false);

                String sqlite_sync_url = ((TextView)findViewById(R.id.tbSqlite_sync_url)).getText().toString();
                SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("sqlite_sync_url", sqlite_sync_url);

                SQLiteSync sqLite_sync = new SQLiteSync("/data/data/" + getPackageName() + "/sqlitesync.db",
                        sqlite_sync_url);

                sqLite_sync.synchronizeSubscriber(getSubscriberId(), new SQLiteSync.SQLiteSyncCallback() {
                    @Override
                    public void onSuccess() {
                        hideProgressBar();
                        button.setEnabled(true);
                        showMessage("Data synchronization finished successfully");
                    }

                    @Override
                    public void onError(Exception error) {
                        hideProgressBar();
                        button.setEnabled(true);
                        error.printStackTrace();
                        showMessage("Data synchronization finished with error: \n" + error.getMessage());
                    }
                });
            }
        });

        findViewById(R.id.btSelectFrom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> tables = new ArrayList<String>();

                SQLiteDatabase db = null;
                Cursor cursor = null;

                try{
                    db = openOrCreateDatabase("/data/data/" + getPackageName() + "/sqlitesync.db", 1, null);
                    cursor = db.rawQuery("select tbl_Name from sqlite_master where type='table'", null);
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

                new AlertDialog.Builder(MainActivity.this)
                        .setAdapter(new ArrayAdapter<String>(
                                MainActivity.this,
                                R.layout.spinner_item,
                                tables
                        ), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TableViewActivity.SelectFrom(MainActivity.this, (String)((AlertDialog)dialog).getListView().getAdapter().getItem(which));
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setTitle("SELECT * FROM...")
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private String getSubscriberId(){
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
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

    private void showProgressBar(){
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }
}
