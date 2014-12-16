Ext.define('Ext.data.proxy.SqliteStorage', {
    extend: 'Ext.data.proxy.Client',
    alias: 'proxy.sqlitestorage',
    alternateClassName: 'Ext.data.SqliteStorageProxy',

    
    constructor: function(config) {
        this.callParent([config]);

        //ensures that the reader has been instantiated properly
        this.setReader(this.reader);
        var me = this;
    },

    //inherit docs
    read: function(operation, callback, scope) {
        var me = this,
            fields = [],
            values = [];

        Ext.iterate(operation.getParams(),function(k,v){
            fields.push(k + ' = ?');
            values.push(v);
        });

        var sql = operation.config.query || me.config.dbConfig.dbQuery || 'SELECT * FROM '+me.config.dbConfig.tablename+'';

        if(fields.length)
        {
            sql = sql + ' WHERE ' + fields.join(' AND ');
        }

        var onSucess, onError;

        onSucess = function(tx, results) {
            me.applyDataToModel(tx, results, operation, callback, scope);
        };

        onError = function(tx, err) {
            me.throwDbError(tx, err);
        };

        me.queryDB(me.getDb(), sql, onSucess, onError,values);
    },

    /**
     *@private
     * Get Database instance
     */
    getDb : function(){
        if(database.Main.dbConn != undefined)
	        return database.Main.dbConn;
        else
            return null;
    },

     /**
     * execute sql statements
     * @param {Object} dbConn Database connection Value
     * @param {String} sql Sql Statement
     * @param {Function} successcallback  success callback for sql execution
     * @param {Function} errorcallback  error callback for sql execution
     * @param {Array}  params  sql statement parameters
     * @param {Function} callback  additional callback
     */
    queryDB: function(dbConn, sql, successcallback, errorcallback, params, callback) {
         var me = this;
         if(dbConn != undefined)
             dbConn.transaction(function(tx) {
                 if (typeof callback == 'function') {
                     callback.call(scope || me, results, me);
                 }
                 if(!params) params = [];
                 //console.log(sql+" " +JSON.stringify(params))
                 tx.executeSql(
                     sql, (params ? params : []), successcallback, errorcallback);
             });
    },
     /**
     * @private
     * Created array of objects, each representing field=>value pair.
     * @param {Object} tx Transaction
     * @param {Object} rs Response
     * @return {Array} Returns parsed data
     */
    parseData: function(tx, rs) {
         var rows = rs.rows,
             data = [],
             i = 0;
         for (; i < rows.length; i++) {
             data.push(rows.item(i));
         }
         return data;
    },

    applyData: function(data, operation, callback, scope) {
        var me = this;
        /*operation.resultSet = new Ext.data.ResultSet({
         records: data,
         total: data.length,
         loaded: true
         });*/
        operation.setSuccessful();
        operation.setCompleted();
        operation.setResultSet(Ext.create('Ext.data.ResultSet', {
            records: data,
            total  : data.length,
            loaded : true
        }));
        //console.log(operation);
        //finish with callback
        operation.setRecords(data);
        if (typeof callback == "function") {
            callback.call(scope || me, operation);
        }
    },

    applyDataToModel: function(tx, results, operation, callback, scope) {
        var me = this,
            Model = me.getModel(),
            fields  = Model.getFields().items,
            primarykey = Model.getIdProperty();
        //console.log(fields);
        var records = me.parseData(tx, results);
        //console.log(records.length);
        var storedatas = [];
        if (results.rows && records.length) {
            for (i = 0; i < results.rows.length; i++) {
                //console.log(records[i]);

                storedatas.push(new Model(records[i],records[i][primarykey]));
            }
            operation.setSuccessful();
        }
        me.applyData(storedatas, operation, callback, scope);
    },
    
    /**
     * Output Query Error
     * @param {Object} tx Transaction
     * @param {Object} rs Response
     */
    throwDbError: function(tx, err) {
        console.log(this.type + "----" + err.message);
    }
});

