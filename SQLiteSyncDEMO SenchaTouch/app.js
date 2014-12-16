//@require @packageOverrides
Ext.Loader.setConfig({

});

Ext.application({
    models: [
        'DocumentsModel',
        'EntitiesModel',
        'UserDocumentsModel',
        'UsersModel'
    ],
    stores: [
        'DocumentsStore',
        'EntitiesStore',
        'UserDocumentsStore',
        'UsersStore'
    ],
    views: [
        'MyTabPanel'
    ],
    controllers: [
        'SQLiteSyncDEMOCtrl'
    ],
    name: 'SQLiteSyncDEMO',

    launch: function() {
        database.Main.conn();
        sqlitesync_DB = database.Main.dbConn;
        Ext.create('SQLiteSyncDEMO.view.MyTabPanel', {fullscreen: true});

        Ext.getStore('DocumentsStore').load();
        Ext.getStore('EntitiesStore').load();
        Ext.getStore('UsersStore').load();
    }

});
