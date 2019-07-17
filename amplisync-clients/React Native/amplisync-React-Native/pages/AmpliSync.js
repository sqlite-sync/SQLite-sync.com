import React from 'react';
import { Button, Text, View, Alert } from 'react-native';
import Mytextinput from './components/Mytextinput';
import Mybutton from './components/Mybutton';
import { openDatabase } from 'react-native-sqlite-storage';
let sqlitesync_DB = openDatabase({ name: 'amplisync.db' });
window.DOMParser = require('xmldom').DOMParser;

let sqlitesync_SyncServerURL = '';
let sqlitesync_SyncTableList = [];
let sqlitesync_SyncGlobalTableIndex = 0;
let sqlitesync_SyncDataToSend;
let sqlitesync_syncPdaIdent = null;

export default class AmpliSync extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      syncServerURL: 'http://ampli1.amplifier.com.pl:8081/demo/API3/',
      syncPdaIdent: '1',
    };
  }

  sortByKey = (array) => {
    return array.sort(function (a, b) {
      let x = a[0]; let y = b[0];
      return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
  }


  reinitializeDatabase = () => {
    let that = this;
    const { syncServerURL } = this.state;
    const { syncPdaIdent } = this.state;
    sqlitesync_SyncServerURL = syncServerURL;
    sqlitesync_syncPdaIdent = syncPdaIdent;


    fetch(sqlitesync_SyncServerURL + "InitializeSubscriber/" + sqlitesync_syncPdaIdent)
      .then(res => res.json())
      .then((data) => {
        let responseReturn = Object.keys(data).sort(function (key1, key2) {
          key1 = key1.toLowerCase(), key2 = key2.toLowerCase();
          if (key1 < key2) return -1;
          if (key1 > key2) return 1;
          return 0;
        });

        console.log(responseReturn);
        console.log('Connected to server...');
        sqlitesync_DB.transaction(function (tx) {

          Object.keys(responseReturn)
            .map(function (v) {
              if (responseReturn[v] != "00000 SQLiteSync.com version")
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

  syncSendAndReceiveChanges = () => {

    let pointer = this;
    sqlitesync_SyncTableList = [];
    sqlitesync_SyncGlobalTableIndex = 0;
    sqlitesync_SyncDataToSend = "<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">";

    sqlitesync_DB.transaction(tx => {
      tx.executeSql("select * from sqlite_master where type='table' and sql like '%RowId%'", [], (tx, results) => {
        for (let tableIndex = 0; tableIndex < results.rows.length; ++tableIndex)
          if (results.rows.item(tableIndex)['tbl_name'] != "MergeDelete") {
            sqlitesync_SyncTableList.push({
              tableName: results.rows.item(tableIndex)['tbl_name']
            });
          }
      });
    }, function (error) {//error
      console.log('Error while building table list ' + error.message + ' (Code ' + error.code + ')');
    }, function () {
      pointer.getChangesForTable(sqlitesync_SyncTableList[sqlitesync_SyncGlobalTableIndex].tableName);
    });
  }

  getChangesForTable = (_tableName) => {
    let tableName = _tableName;
    let pointer = this;
    sqlitesync_DB.transaction(tx => {
      tx.executeSql("select * from sqlite_master where type='table' and tbl_name like ?", [tableName], (tx, results) => {
        for (let tableIndex = 0; tableIndex < results.rows.length; ++tableIndex) {

          let tableRow = results.rows.item(tableIndex);

          sqlitesync_SyncDataToSend += "<tab n=\"" + tableName + "\">";

          let columnParts = tableRow['sql'].replace(/^[^\(]+\(([^\)]+)\)/g, '$1').split(',');
          let columnNames = [];
          for (let col in columnParts) {
            if (typeof columnParts[col] === 'string')
              columnNames.push(columnParts[col].split(" ")[0].replace('[','').replace(']',''));
          }

          /*** new records ***/
          let selectForTable = "select * from " + tableName + " where RowId is null";

          tx.executeSql(selectForTable, [], function (tx, result) {
            let datasetTable = result.rows;

            if (datasetTable.length > 0) {
              console.log('Sending a new records for the table: ' + tableName + '');
            }
            sqlitesync_SyncDataToSend += "<ins>";
            for (let i = 0, row = null; i < datasetTable.length; i++) {
              row = datasetTable.item(i);
              sqlitesync_SyncDataToSend += "<r>";
              for (let j = 0; j < columnNames.length; j++) {
                if (columnNames[j].replace(/"/gi, '') != 'MergeUpdate') {
                  sqlitesync_SyncDataToSend += "<" + columnNames[j].replace(/"/gi, '') + ">";
                  sqlitesync_SyncDataToSend += "<![CDATA[" + row[columnNames[j].replace(/"/gi, '')] + "]]>";
                  sqlitesync_SyncDataToSend += "</" + columnNames[j].replace(/"/gi, '') + ">";
                }
              }
              sqlitesync_SyncDataToSend += "</r>";
            }
            sqlitesync_SyncDataToSend += "</ins>";
            /*** updated records ***/
            let selectForUpdateTable = "select * from " + tableName + " where MergeUpdate > 0 and RowId is not null";
            tx.executeSql(selectForUpdateTable, [], function (tx, result) {
              let datasetTable = result.rows;

              if (datasetTable.length > 0) {
                console.log('Sending updated records for the table: ' + tableName + '');
              }

              sqlitesync_SyncDataToSend += "<upd>";
              for (let i = 0, row = null; i < datasetTable.length; i++) {
                row = datasetTable.item(i);
                sqlitesync_SyncDataToSend += "<r>";
                for (let j = 0; j < columnNames.length; j++) {
                  if (columnNames[j].replace(/"/gi, '') != 'MergeUpdate') {
                    sqlitesync_SyncDataToSend += "<" + columnNames[j].replace(/"/gi, '') + ">";
                    sqlitesync_SyncDataToSend += "<![CDATA[" + row[columnNames[j].replace(/"/gi, '')] + "]]>";
                    sqlitesync_SyncDataToSend += "</" + columnNames[j].replace(/"/gi, '') + ">";
                  }
                }
                sqlitesync_SyncDataToSend += "</r>";
              }
              sqlitesync_SyncDataToSend += "</upd>";
              sqlitesync_SyncDataToSend += "</tab>";
            });
            /***/
          });
          /***/
        }
      });
    }, function (error) {//error
      console.log('Error while syncing with the server ' + error.message + ' (Code ' + error.code + ')');
    }, function () {
      sqlitesync_SyncGlobalTableIndex++;
      if (sqlitesync_SyncGlobalTableIndex < sqlitesync_SyncTableList.length)
        pointer.getChangesForTable(sqlitesync_SyncTableList[sqlitesync_SyncGlobalTableIndex].tableName);
      else
        pointer.prepareDeletedRecords();
    });
  }

  prepareDeletedRecords = () => {
    let pointer = this;
    sqlitesync_DB.transaction(tx => {
      tx.executeSql("select * from MergeDelete", [], (tx, result) => {
        datasetTable = result.rows;
        sqlitesync_SyncDataToSend += "<delete>";
        if (datasetTable.length > 0) {
          console.log('Sending records for delete');
        }
        for (let i = 0, row = null; i < datasetTable.length; i++) {
          row = datasetTable.item(i);
          sqlitesync_SyncDataToSend += "<r>";
          sqlitesync_SyncDataToSend += "<tb>" + row['TableId'] + "</tb>";
          sqlitesync_SyncDataToSend += "<id>" + row['RowId'] + "</id>";
          sqlitesync_SyncDataToSend += "</r>";
        }
        sqlitesync_SyncDataToSend += "</delete>";
      });
    }, function (error) {//error
      console.log('Error while getting deleted rows ' + error.message + ' (Code ' + error.code + ')');
    }, function () {
      pointer.sendDataToServer();
    });
  }

  sendDataToServer = () => {
    const { syncServerURL } = this.state;
    const { syncPdaIdent } = this.state;
    let pointer = this;
    sqlitesync_SyncDataToSend += "</SyncData>";
    console.log(sqlitesync_SyncDataToSend);

    fetch(syncServerURL + 'Send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json', 
      },
      body: JSON.stringify({
        subscriber: syncPdaIdent,
        content: sqlitesync_SyncDataToSend,
        version: '3'
      }),
    }).then((data) => {
      console.log(data);
      pointer.clearUpdateMarker();
      pointer.clearDeletedRecords();
      console.log('Sending finished');
      pointer.synchronizeDatabase();
    })
      .catch(console.log);
  }

  clearUpdateMarker = () => {
    var selectAllStatement = "select * from sqlite_master where type='table' and sql like '%RowId%'";
    sqlitesync_DB.transaction(function (tx) {
      tx.executeSql(selectAllStatement, [], function (tx, result) {
        var datasetTables = result.rows;
        for (var tableIndex = 0; tableIndex < datasetTables.length; tableIndex++) {
          var item = datasetTables.item(tableIndex);
          if (item['tbl_name'].toString().toLowerCase() == "mergeidentity") {
            tx.executeSql("update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;", [], function (transaction, result) { }, function (transaction, error) { console.log(error); });
          }

          if (item['tbl_name'] != "MergeDelete" && item['tbl_name'] != "MergeIdentity") {
            var selectForUpdateTable = "select *, ? as syncTableName from " + item['tbl_name'] + " where MergeUpdate > 0";
            tx.executeSql(selectForUpdateTable, [item['tbl_name']], function (tx, result) {
              if (result.rows.length > 0) {
                var syncTableName = result.rows.item(0)['syncTableName'];
                var selectTriggerStatement = "select * from sqlite_master where type='trigger' and name = 'trMergeUpdate_" + syncTableName + "'";
                tx.executeSql(selectTriggerStatement, [], function (tx, result) {
                  if (result.rows.length > 0) {
                    var syncTableName = result.rows.item(0)['tbl_name'];
                    var trigger = result.rows.item(0)['sql'];

                    tx.executeSql("drop trigger trMergeUpdate_" + syncTableName + ";", [], 
                      function (transaction, result) {},
                      function (transaction, error) {
                      }); 
                    tx.executeSql("update " + syncTableName + " set MergeUpdate=0 where MergeUpdate > 0;", [], 
                      function (transaction, result) {},
                      function (transaction, error) {
                      });
                    tx.executeSql(trigger, [], 
                      function (transaction, result) {},
                      function (transaction, error) {
                      });
                  }
                });
              }
            });
            /***/
          }
        }
      });
    });
  }

  clearDeletedRecords = () => {
    var selectAllStatement = "delete from MergeDelete";
    sqlitesync_DB.transaction(function (tx) {
      tx.executeSql(selectAllStatement, [], function (tx, result) {
      });
    });
  }


  synchronizeDatabase = () => {
    const { syncServerURL } = this.state;
    const { syncPdaIdent } = this.state;

    sqlitesync_DB.transaction(tx => {
      tx.executeSql("select tbl_Name from sqlite_master where type='table' and sql like '%RowId%'", [], (tx, results) => {
        for (let tableIndex = 0; tableIndex < results.rows.length; ++tableIndex)
          if (results.rows.item(tableIndex)['tbl_name'] != "MergeDelete") {
            let tableName = results.rows.item(tableIndex)['tbl_name'];
            console.log(tableName);
            fetch(syncServerURL + "Sync/" + syncPdaIdent + "/" + tableName)
              .then(res => res.json())
              .then((data) => {
                let responseReturn = data;

                let queryInsert = null;
                let queryUpdate = null;
                let queryDelete = null;
                let syncId = responseReturn[0].SyncId;

                if (syncId > 0) {
                  sqlitesync_DB.transaction(tx => {
                    console.log('Preparing changes for table ' + responseReturn[0].TableName);

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
                    if(tableName.toLowerCase() != 'mergeidentity'){
                      tx.executeSql(responseReturn[0].TriggerInsertDrop, null, null,
                        function (transaction, error) {
                        });
                      tx.executeSql(responseReturn[0].TriggerUpdateDrop, null, null,
                        function (transaction, error) {
                        });
                      tx.executeSql(responseReturn[0].TriggerDeleteDrop, null, null,
                        function (transaction, error) {
                        });                      
                    }
                    /****************************/

                    let record = xmlDoc.childNodes[1];

                    for (let i = 0; i < record.childNodes.length; i++) {

                      let rowValues = new Array();
                      let colCount = 0;

                      for (let ii = 0; ii < record.childNodes[i].childNodes.length; ii++) {
                        rowValues[ii] = record.childNodes[i].childNodes[ii].textContent;
                        colCount++;
                      }

                      let rowId = rowValues[colCount - 1];
                      let identityCol = rowValues[0];

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
                    if(tableName.toLowerCase() != 'mergeidentity'){
                      tx.executeSql(responseReturn[0].TriggerInsert, null, null,
                        function (transaction, error) {
                        });
                      tx.executeSql(responseReturn[0].TriggerUpdate, null, null,
                        function (transaction, error) {
                        });
                      tx.executeSql(responseReturn[0].TriggerDelete, null, null,
                        function (transaction, error) {
                        });
                    }
                    /****************************/
                  }, function (error) {//error
                    console.log('Error while syncing with the server ' + error.message + ' (Code ' + error.code + ')');
                  }, function () {
                    if (syncId > 0)
                      fetch(syncServerURL + "CommitSync/" + syncId);
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
          style={{ padding: 10 }}
        />
        <Mytextinput
          placeholder="Enter subscriber ID"
          value="1"
          onChangeText={syncPdaIdent => this.setState({ syncPdaIdent })}
          style={{ padding: 10 }}
        />
        <Mybutton
          title="Reinitialize"
          customClick={this.reinitializeDatabase.bind(this)}
        />
        <Mybutton
          title="Synchronize data"
          customClick={this.syncSendAndReceiveChanges.bind(this)}
        />
      </View>
    );
  }
}