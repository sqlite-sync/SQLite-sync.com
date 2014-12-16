Ext.define('SQLiteSyncDEMO.store.UsersStore', {
    extend: 'Ext.data.Store',

    requires: [
        'SQLiteSyncDEMO.model.UsersModel'
    ],

    config: {
        model: 'SQLiteSyncDEMO.model.UsersModel',
        storeId: 'UsersStore'
    }
});