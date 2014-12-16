Ext.define('SQLiteSyncDEMO.model.DocumentsModel', {
    extend: 'Ext.data.Model',

    config:{
        fields : [
            'docId',
            'docName',
            'docSize',
            'docDate',
            'docValue1'
        ],
        proxy: {
            type: 'sqlitestorage',
            dbConfig: {
                tablename: 'Documents',
                dbConn: Ext.DbConnection,
                dbQuery: "SELECT * FROM Documents"
            },
            reader: {
                type: 'array',
                idProperty: 'docId'
            }
        }
    },
    writer: {
        type: 'array'
    }
});