Ext.define('SQLiteSyncDEMO.store.EntitiesStore', {
    extend: 'Ext.data.Store',

    requires: [
        'SQLiteSyncDEMO.model.EntitiesModel'
    ],

    config: {
        model: 'SQLiteSyncDEMO.model.EntitiesModel',
        storeId: 'EntitiesStore'
    }
});