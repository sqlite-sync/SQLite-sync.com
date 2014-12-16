Ext.define('SQLiteSyncDEMO.model.UsersModel', {
    extend: 'Ext.data.Model',

    config:{
        fields : [
            'usrId',
            'usrName',
            'usrLastName',
            'usrAge',
            'usrLogin',
            'usrPass',
            'usrSubscriberId'
        ],
        proxy: {
            type: 'sqlitestorage',
            dbConfig: {
                tablename: 'Users',
                dbConn: Ext.DbConnection,
                dbQuery: "SELECT * FROM Users"
            },
            reader: {
                type: 'array',
                idProperty: 'usrId'
            }
        }
    },
    writer: {
        type: 'array'
    }
});