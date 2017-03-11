function sqlitesync_AddLog(logMsg){
    console.log(logMsg);
    /*paste your code here*/
    $("#LogDiv").append(logMsg + "</br>");
}

function sqlitesync_SyncEnd(){
    sqlitesync_AddLog('<p>Synchronization completed!</p>')
    alert('Synchronization was successful!');
}
