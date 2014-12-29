Ext.define('SQLiteSyncDEMO.store.DocumentsStore', {
    extend: 'Ext.data.Store',

    requires: [
        'SQLiteSyncDEMO.model.DocumentsModel'
    ],

    config: {
        autoLoad: true,
        model: 'SQLiteSyncDEMO.model.DocumentsModel',
        storeId: 'DocumentsStore'
    }
});