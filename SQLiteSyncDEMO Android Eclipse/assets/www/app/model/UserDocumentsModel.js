Ext.define('SQLiteSyncDEMO.model.UserDocumentsModel', {
    extend: 'Ext.data.Model',

    config:{
        fields : [
            'usdUsrId',
            'usdDocId'
        ],
        proxy: {
            type: 'sqlitestorage',
            dbConfig: {
                tablename: 'UserDocuments',
                dbConn: Ext.DbConnection,
                dbQuery: "SELECT * FROM UserDocuments"
            },
            reader: {
                type: 'array',
                idProperty: 'usdUsrId'
            }
        }
    },
    writer: {
        type: 'array'
    }
});