Ext.define('SQLiteSyncDEMO.store.UserDocumentsStore', {
    extend: 'Ext.data.Store',

    requires: [
        'SQLiteSyncDEMO.model.UserDocumentsModel'
    ],

    config: {
        model: 'SQLiteSyncDEMO.model.UserDocumentsModel',
        storeId: 'UserDocumentsStore'
    }
});