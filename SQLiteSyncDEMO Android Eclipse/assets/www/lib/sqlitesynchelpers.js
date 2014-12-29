function sqlitesync_AddLog(logMsg){
    console.log(logMsg);
    /*paste your code here*/
    var logControl = Tools.getElement('dataview[id="lstLog"]');
    if(logControl != undefined && logControl != null)
        logControl.add({html: logMsg});
}

function sqlitesync_SyncEnd(){
    sqlitesync_AddLog('<p>Synchronization completed!</p>')
    Tools.showMask();
    Ext.getStore("DocumentsStore").load();
    Ext.getStore("EntitiesStore").load();
    Ext.getStore("UsersStore").load();
    Tools.hideMask();
    Ext.Msg.alert('SQLite-Sync.com', 'Synchronization was successful!' , Ext.emptyFn);
}
