import React from 'react';
import { Button, Text, View, Alert } from 'react-native';
import Mytextinput from './components/Mytextinput';
import Mybutton from './components/Mybutton';
import { openDatabase } from 'react-native-sqlite-storage';
var sqlitesync_DB = openDatabase({ name: 'amplisync.db' });
window.DOMParser = require('xmldom').DOMParser;

var sqlitesync_SyncServerURL = '';
var sqlitesync_SyncTableList = [];
var sqlitesync_SyncTableCurrentIndex = 0;
var sqlitesync_SyncGlobalTableIndex = 0;
var sqlitesync_SyncDataToSend;
var sqlitesync_syncPdaIdent = null;

export default class AmpliSync extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
        syncServerURL: 'http://ampli1.amplifier.com.pl:8081/demo/API3/',
        syncPdaIdent: '1',
    };
  }

  sortByKey = (array) => {
        return array.sort(function(a, b)
        {
            var x = a[0]; var y = b[0];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
    }


  reinitializeDatabase = () => {
    var that = this;
    const { syncServerURL } = this.state;
    const { syncPdaIdent } = this.state;
    sqlitesync_SyncServerURL = syncServerURL;
    sqlitesync_syncPdaIdent = syncPdaIdent;


    fetch(sqlitesync_SyncServerURL + "InitializeSubscriber/" + sqlitesync_syncPdaIdent)
    .then(res => res.json())
    .then((data) => {      
      var responseReturn = Object.keys(data).sort(function(key1, key2){
                key1 = key1.toLowerCase(), key2 = key2.toLowerCase();
                if(key1 < key2) return -1;
                if(key1 > key2) return 1;
                return 0;
            });
            
      console.log(responseReturn);
      console.log('Connected to server...');
      sqlitesync_DB.transaction(function (tx) {

        Object.keys(responseReturn)
            .map(function(v) {                
                    if(responseReturn[v] != "00000 SQLiteSync.com version")
                        tx.executeSql(data[responseReturn[v]],
                            null,
                            function (transaction, result) {
                                console.log('Creating object ' + responseReturn[v] + '...');
                            },
                            function (transaction, error) {
                                console.log('Object ' + responseReturn[v] + '; ' + error.message + ' (Code ' + error.code + ')');
                            });
                });

      }, function (error) {//error
          console.log('Error while syncing with the server ' + error.message + ' (Code ' + error.code + ')');
      }, function () {
          console.log('Synchronization completed');
            Alert.alert(
              'Success',
              'Reinitialization completed',
              [
                {
                  text: 'Ok',                  
                },
              ],
              { cancelable: false }
            );          
      });

    })
    .catch(console.log);
  }

  synchronizeDatabase = () => {
    const { syncServerURL } = this.state;
    const { syncPdaIdent } = this.state;

    sqlitesync_DB.transaction(tx => {
        tx.executeSql("select tbl_Name from sqlite_master where type='table'", [], (tx, results) => {          
          for (let tableIndex = 0; tableIndex < results.rows.length; ++tableIndex) 
            if(results.rows.item(tableIndex)['tbl_name'] != "MergeDelete" && results.rows.item(tableIndex)['tbl_name'] != "sqlite_sequence")
            {
                var tableName = results.rows.item(tableIndex)['tbl_name'];
                console.log(tableName);
                fetch(syncServerURL + "Sync/" + syncPdaIdent + "/" + tableName)
                .then(res => res.json())
                .then((data) => {      
                    var responseReturn = data;

					var queryInsert = null;
					var queryUpdate = null;
                    var queryDelete = null;
                    var syncId = 0;

					if (responseReturn[0].SyncId > 0) {
                        sqlitesync_DB.transaction(tx => {
                            
                            console.log('Preparing changes for table ' + responseReturn[0].TableName);
                            syncId = responseReturn[0].SyncId;

                            if (window.DOMParser) {
                                parser = new DOMParser();
                                xmlDoc = parser.parseFromString(responseReturn[0].Records, "text/xml");
                            }
                            else // Internet Explorer
                            {
                                xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
                                xmlDoc.async = "false";
                                xmlDoc.loadXML(responseReturn[0].Records);
                            }

                            queryInsert = responseReturn[0].QueryInsert;
                            queryUpdate = responseReturn[0].QueryUpdate;
                            queryDelete = responseReturn[0].QueryDelete;
                            

                            /*************************/                        
                            tx.executeSql(responseReturn[0].TriggerInsertDrop, null, null,
                                function (transaction, error) {
                                });
                            tx.executeSql(responseReturn[0].TriggerUpdateDrop, null, null,
                                function (transaction, error) {
                                });
                            tx.executeSql(responseReturn[0].TriggerDeleteDrop, null, null,
                                function (transaction, error) {
                                });
                            /****************************/

                            var record = xmlDoc.childNodes[1];

                            for (var i = 0; i < record.childNodes.length; i++) {

                                var rowValues = new Array();							
                                var colCount = 0;

                                for (var ii = 0; ii < record.childNodes[i].childNodes.length; ii++) {
                                    rowValues[ii] = record.childNodes[i].childNodes[ii].textContent;
                                    colCount++;
                                }

                                var rowId = rowValues[colCount - 1];
                                var identityCol = rowValues[0];

                                switch (record.childNodes[i].getAttribute("a")) {
                                    case "1":
                                        tx.executeSql(queryInsert,
                                            rowValues,
                                            function (transaction, result) {

                                            },
                                            function (transaction, error) {
                                                console.log("insert " + error);
                                            });
                                        break;
                                    case "2":
                                        tx.executeSql(queryUpdate,
                                            rowValues,
                                            function (transaction, result) {

                                            },
                                            function (transaction, error) {
                                                console.log(error);
                                            });
                                        break;
                                    case "3":
                                        tx.executeSql(queryDelete + "'" + rowId + "'",
                                            null,
                                            null,
                                            function (transaction, error) {
                                                console.log(error);
                                            });
                                        break;
                                }
                            }

                            /*************************/
                            tx.executeSql(responseReturn[0].TriggerInsert, null, null,
                                function (transaction, error) {
                                });
                            tx.executeSql(responseReturn[0].TriggerUpdate, null, null,
                                function (transaction, error) {
                                });
                            tx.executeSql(responseReturn[0].TriggerDelete, null, null,
                                function (transaction, error) {
                                });
                            /****************************/
                        }, function (error) {//error
                            console.log('Error while syncing with the server ' + error.message + ' (Code ' + error.code + ')');
                        }, function () {
                            fetch(syncServerURL + "CommitSync/" + responseReturn[0].SyncId);
                            console.log('Table ' + tableName + ' has been synchronized!');      
                        });
                    }
                })
                .catch(console.log);                
            }
        });
      }, function (error) {//error
        console.log('Error while syncing with the server ' + error.message + ' (Code ' + error.code + ')');
        }, function () {
            console.log('Synchronization completed');        
        });
  }
 
  render() {
    return (
      <View style={{ backgroundColor: 'white', flex: 1 }}>
        <Mytextinput
          placeholder="Enter synchronization URL"
          value="http://ampli1.amplifier.com.pl:8081/demo/API3/"
          onChangeText={syncServerURL => this.setState({ syncServerURL })}
          style={{ padding:10 }}
        />
        <Mytextinput
          placeholder="Enter subscriber ID"          
          value="1"
          onChangeText={syncPdaIdent => this.setState({ syncPdaIdent })}
          style={{ padding:10 }}
        />
        <Mybutton
          title="Reinitialize"
          customClick={this.reinitializeDatabase.bind(this)}
        />
        <Mybutton
          title="Synchronize data"
          customClick={this.synchronizeDatabase.bind(this)}
        />
      </View>
    );
  }
}