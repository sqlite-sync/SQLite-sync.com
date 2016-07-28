package com.sqlitesync.sqlitesyncdemoandroidandroidstudio;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.sqlitesync.service.SQLiteSyncCOMCore;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private Context _context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _context = this;

        findViewById(R.id.btReinitialize).setOnClickListener(this);
        findViewById(R.id.btSendAndReceive).setOnClickListener(this);

        ((Spinner)findViewById(R.id.spinner)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.spinner)).setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                new String[]{"Pick a table to select records", "Documents", "Entities", "Users", "UserDocuments"}
        ));
    }

    @Override
    public void onClick(View view) {
        String subscriberId = ((EditText) findViewById(R.id.tbSubscriberId)).getText().toString();

        switch (view.getId()){
            case R.id.btReinitialize:
                ShowProgressBar();
                new SQLiteSyncCOMCore(
                        "/data/data/" + getPackageName() + "/sqlitesync.db",
                        "http://demo.sqlite-sync.com/sync.asmx"
                    ).ReinitializeDB(subscriberId, new SQLiteSyncCOMCore.SynchronizationCallback() {
                        @Override
                        public void onFinished(Exception exception) {
                            HideProgressBar();
                            if(exception == null)
                                ShowMessage("Reinitialize database done");
                            else
                                ShowMessage("Reinitialize database error: \n"+exception.getMessage());
                        }
                    });
                break;
            case R.id.btSendAndReceive:
                ShowProgressBar();
                new SQLiteSyncCOMCore(
                        "/data/data/" + getPackageName() + "/sqlitesync.db",
                        "http://demo.sqlite-sync.com/sync.asmx"
                    ).SendAndReceiveChanges(subscriberId, new SQLiteSyncCOMCore.SynchronizationCallback() {
                        @Override
                        public void onFinished(Exception exception) {
                            HideProgressBar();
                            if(exception == null)
                                ShowMessage("Send and receive changes done");
                            else
                                ShowMessage("Send and receive changes error: \n"+exception.getMessage());
                        }
                    });
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.spinner:
                if(i != 0) TableViewActivity.SelectTop100From(_context,adapterView.getSelectedItem().toString());
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void ShowMessage(String message){
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle("SQLite-sync DEMO");
        dlgAlert.setCancelable(false);
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dlgAlert.create().show();
    }

    private void ShowProgressBar(){
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    private void HideProgressBar(){
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }
}
