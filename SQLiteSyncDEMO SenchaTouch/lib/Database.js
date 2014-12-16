Ext.ns('database');

database.Main = {
    /**
    * Variable to store database connection
    * @return {Object}
    */
    dbConn : null,

    /**
    * This method will create database if the mentioned DB is not yet created.
    */
    conn : function() {
        var shortName = 'sqlitesync';
        var version = '1.0';
        var displayName = 'sqlitesync';
        var maxSize = 5 * 1024 * 1024; // in bytes

        try{
            if (!window.openDatabase) {
                alert('Error creating database.');
            } else {
                this.dbConn = openDatabase(shortName, version, displayName, maxSize);
            }
        } catch (e){
            if(e==2){
                alert("Database version mismatch.");
            } else {
                alert("EROR: "+e+".");
            }
        }
    }
}