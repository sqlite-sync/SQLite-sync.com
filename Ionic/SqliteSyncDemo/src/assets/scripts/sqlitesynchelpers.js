function sqlitesync_AddLog(logMsg){
    console.log(logMsg);
}

function sqlitesync_SyncEnd(){
    sqlitesync_AddAlert('Synchronization completed!');
    sqlitesync_loading = false;
    resolve();
}

function sqlitesync_AddAlert(alertMsg){
    alert(alertMsg);
}