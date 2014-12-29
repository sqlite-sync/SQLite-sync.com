Ext.define('SQLiteSyncDEMO.model.EntitiesModel', {
    extend: 'Ext.data.Model',

    config:{
        fields : [
            'entId',
            'entName',
            'entAddress',
            'entEnabled'
        ],
        proxy: {
            type: 'sqlitestorage',
            dbConfig: {
                tablename: 'Entities',
                dbConn: Ext.DbConnection,
                dbQuery: "SELECT * FROM Entities"
            },
            reader: {
                type: 'array',
                idProperty: 'entId'
            }
        }
    },
    writer: {
        type: 'array'
    }});