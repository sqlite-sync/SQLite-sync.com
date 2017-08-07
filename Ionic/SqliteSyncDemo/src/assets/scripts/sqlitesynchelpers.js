function sqlitesync_AddLog(logMsg){
    console.log(logMsg);
}

function sqlitesync_SyncEnd(){
    sqlitesync_AddAlert('Synchronization completed!');
    sqlitesync_AddAlert(sqlitesync_loading);
}

function sqlitesync_AddAlert(alertMsg){
    alert(alertMsg);
}
