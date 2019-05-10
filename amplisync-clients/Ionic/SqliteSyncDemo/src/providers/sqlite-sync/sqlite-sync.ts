import { Injectable } from '@angular/core';
import { Http, Headers, RequestOptions  } from '@angular/http';
import 'rxjs/add/operator/map';
import { SQLite, SQLiteObject } from '@ionic-native/sqlite';
import * as xml2js from "xml2js";
/*
  Generated class for the SqliteSyncProvider provider.

  See https://angular.io/guide/dependency-injection for more info on providers
  and Angular DI.
*/
@Injectable()
export class SqliteSyncProvider {

  constructor(public http: Http) {
  }

  openDatabase(){
    return new Promise((resolve,reject) => {
      let name = "sqlitesynccom_demo";
      let sqlite_db = new SQLite();
      sqlite_db.create({
        name: name,
        location: 'default'
      }).then((db: SQLiteObject) => {
        resolve(db);
      }, (err) => {
        reject(err);
      });
    });
  }


  
  getTables(){
    return new Promise((resolve,reject) => {
      this.openDatabase().then((db: SQLiteObject) => {
        db.executeSql("SELECT tbl_name FROM sqlite_master WHERE type='table' AND tbl_name != 'android_metadata' AND tbl_name != 'MergeDelete' AND tbl_name != 'MergeIdentity'", {})
        .then((data) => {
          let sqlitesync_tables = [];
          for(let i = 0; i < data.rows.length; i++)
            sqlitesync_tables.push(data.rows.item(i).tbl_name);
          resolve(sqlitesync_tables);
        })
        .catch((err) => {
          reject("Error while getting data from database!");
        })
      })
      .catch((err) => {
        reject("Error while connecting to local database!");
      })
    })
  }

  getTableColumns(tblName) {
    return new Promise((resolve,reject) => {
      this.openDatabase().then((db: SQLiteObject) => {
        db.executeSql("SELECT * FROM pragma_table_info('" + tblName + "')", [])
        .then((data) => {
          let rows = [];
          for(let i = 0; i < data.rows.length; i++)
            rows.push(data.rows.item(i));
          resolve(rows);
        })
        .catch((err) => {
          reject("Error while getting data from database!");
        });
      })
      .catch((err) => {
        reject("Error while connecting to local database!");
      })
    })
  }

  getDataFromTable(tblName){
    return new Promise((resolve,reject) => {
      this.openDatabase().then((db: SQLiteObject) => {
        db.executeSql("SELECT * FROM " + tblName, [])
        .then((data) => {
          let rows = [];
          for(let i = 0; i < data.rows.length; i++)
            rows.push(data.rows.item(i));
          resolve(rows);
        })
        .catch((err) => {
          reject("Error while getting data from table!");
        });
      })
      .catch((err) => {
        reject("Error while connecting to local database!");
      })
    })
  }

insertIntoTable(tblName, columns, values){
  return new Promise((resolve,reject) => {
    let cols = '';
    let vars = '';
    for(let col of columns){
      cols += col + ",";
      vars += "?,";
    }
    cols = cols.substr(0, cols.length - 1);
    vars = vars.substr(0, vars.length - 1);
    let query = "INSERT INTO " + tblName + " (" + cols + ")VALUES(" + vars + ");";
    this.openDatabase().then((db: SQLiteObject) => {
      db.executeSql(query, values).then(() => {
        resolve();
      }).catch((err) => {
        reject("Error while inserting record to table!");
      });
    }).catch((err) => {
      reject("Error while connecting to local database!");
    })
  });
}

updateRecord(tblName, columns, values){
  return new Promise((resolve,reject) => {
    let query = "UPDATE " + tblName + " SET ";
    for(let col of columns){
      query += col + "=?,";
    }
    query = query.substr(0, query.length - 1);
    query += " WHERE RowId = ?;";
    this.openDatabase().then((db: SQLiteObject) => {
      db.executeSql(query, values).then(() => {
        resolve();
      }).catch((err) => {
        reject("Error while updating record!");
      });
    }).catch((err) => {
      reject("Error while connecting to local database!");
    });
  });
}

deleteFromTable(tblName, row){
  return new Promise((resolve, reject) => {
    if(row != null){
      let query = "DELETE FROM " + tblName + " WHERE ";
      for (let col in row){
        query += col + "='" + row[col] + "' AND ";
      }
      query = query.substr(0, query.length - 4);
      query += ";";
      this.openDatabase().then((db: SQLiteObject) => {
        db.executeSql(query, []).then(() => {
          resolve();
        }).catch((err) => {
          reject("Error while deleting record from table!");
        })
      }).catch((err) => {
        reject("Error while connecting to local database!");
      });
    }
    else{
      reject("Error with record");
    }
  });
}



//SQLITE-SYNC

  AddTableToSynchronization(syncUrl, tblName){
    return new Promise((resolve, reject) => {
      let url = syncUrl + "/AddTable/" + tblName;
      this.http.get(url)
      .subscribe(() => {
        resolve();
      }, (err) => {
        reject("Error while adding table to synchronization");
      });
    });
  }

  RemoveTableFromSynchronization(syncUrl, tblName){
    return new Promise((resolve, reject) => {
      let url = syncUrl + "/RemoveTable/" + tblName;
      this.http.get(url)
      .subscribe(() => {
        resolve();
      }, (err) => {
        reject("Error while removing table from synchronization");
      });
    });
  }

  InitializeSubscriber(syncUrl, subscriberId){
    return new Promise((resolve,reject) => {
      let url = syncUrl + "/InitializeSubscriber/" + subscriberId;
      this.http.get(url)
        .subscribe((res) => {
          let data = res.json();
          this.openDatabase().then((db: SQLiteObject) => {
            db.transaction((tx: any) => {
              Object.keys(data)
                .sort()
                .forEach(function(v,i){
                  if(i !== 0){
                    tx.executeSql(data[v],[], function (transaction, result) {
                      console.log("Creating object " + v + "...");
                    }, function (transaction, error) {
                      console.log("Object " + v + ": " + error.message + " (Code " + error.code + ")");
                    });
                  }
                });
            })
            .then((res) => {
              this.getTables().then((data) => {
                resolve(data);
              })
              .catch((err) => {
                reject("Error while getting data from table!");
              });
            })
            .catch((err) => {
              reject("Error while reinitializing subscriber!");
            });
          });
        }, (err) => {
          reject("Error while requesting API!");
        });
    });
  }


  SynchronizeSubscriber(syncUrl, subscriberId){
    return new Promise((resolve,reject) => {
      this.SendLocalChanges(syncUrl, subscriberId).then(() => {
        this.clearChangesMarker().then(() => {
          this.openDatabase().then((db: SQLiteObject) => {
            let tables = [];
            db.executeSql("select tbl_Name from sqlite_master where type='table' and tbl_Name != 'android_metadata'", []).then((result) => {
              for(let i = 0; i < result.rows.length; i++)
                if (result.rows.item(i).tbl_name.toLowerCase() !== "MergeDelete".toLowerCase())
                  tables.push(result.rows.item(i).tbl_name);

                var promises = tables.map(tbl => {
                  return this.getRemoteChangesForTable(syncUrl, subscriberId, tbl);
                });
              Promise.all(promises).then(() => {
                resolve("sukces");
              }).catch((err) => {
                reject(err);
              })

            }).catch((err) => {
              reject("Error while getting tables from local database!");
            });
          }).catch((err) => {
            reject("Error while connecting to local database!");
          })

        }).catch((err) => {
          reject(err);
        })
      }).catch((err) => {
        reject(err);
      })
    });
  }



  SendLocalChanges(syncUrl, subscriberId){
    return new Promise((resolve,reject) => {
      let queries = [];
      let changes = "";
      let tables = [];
      this.openDatabase().then((db:SQLiteObject) => {
        let SelectAllTables = "SELECT tbl_name FROM sqlite_master WHERE type='table' AND sql LIKE '%RowId%' AND tbl_name != 'android_metadata';";
        db.executeSql(SelectAllTables, [])
          .then((result) => {
            for(let i = 0; i < result.rows.length; i++)
              tables.push(result.rows.item(i).tbl_name);

            changes += "<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">";

            for(let tableName of tables){
              if(tableName.toLowerCase() !== "MergeDelete".toLowerCase()){
                queries.push({
                  'insert': "SELECT * FROM " + tableName + " WHERE RowId IS NULL;",
                  'update': "SELECT * FROM " + tableName + " WHERE RowId IS NOT NULL AND MergeUpdate > 0;",
                  'table': tableName
                });
              }
            }

            db.transaction(function(tx){
              for(let query of queries){
                tx.executeSql(query.insert, [],
                  function(tx, result) {
                    changes += "<tab n=\"" + query.table + "\">";
                    changes += "<ins>";
                    for(let i = 0; i < result.rows.length; i++){
                      changes += "<r>";
                      for(let key in result.rows.item(i))
                        if(key.toLowerCase() !== "MergeUpdate".toLowerCase())
                          changes += "<" + key + "><![CDATA[" + result.rows.item(i)[key] + "]]></" + key + ">";
                      changes += "</r>";
                    }
                    changes += "</ins>";
                  }, null);
                tx.executeSql(query.update, [],
                  function(tx, result) {
                    changes += "<upd>";
                    for(let i = 0; i < result.rows.length; i++){
                      changes += "<r>";
                      for(let key in result.rows.item(i))
                        if(key.toLowerCase() !== "MergeUpdate".toLowerCase())
                          changes += "<" + key + "><![CDATA[" + result.rows.item(i)[key] + "]]></" + key + ">";
                      changes += "</r>";
                    }
                    changes += "</upd></tab>";
                  }, null);
              }
            }).then(() => {
              db.executeSql("SELECT TableId,RowId FROM MergeDelete;",[]).then((result) => {
                changes += "<delete>";
                for(let i = 0; i < result.rows.length; i++)
                  changes += "<r><tb>" + result.rows.item(i)["TableId"] + "</tb><id>" + result.rows.item(i)["RowId"] + "</id></r>";
                changes += "</delete></SyncData>";
                let url = syncUrl + "/Send";
                let headers = new Headers({
                  'Content-Type': 'application/json'
                });
                let options = new RequestOptions({headers: headers});
                let data = JSON.stringify({
                  "subscriber": subscriberId,
                  "content": changes,
                  "version": "3"
                });
                this.http.post(url, data, options)
                  .subscribe(() => {
                    resolve();
                  }, (err) => {
                    reject("Error while sending data to remote database.");
                  })
              });
            });

          }).catch((err) => {
          reject("Error while getting tables from local database!");
          })
      }).catch((err) => {
        reject("Error while connecting to database!");
      })
    });
  }

  clearChangesMarker(){
    return new Promise((resolve,reject) => {
      let query = "select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';";
      let tables = [];
      this.openDatabase().then((db: SQLiteObject) => {
        db.executeSql(query, []).then((result) => {
          for(let i = 0; i < result.rows.length; i++)
            tables.push(result.rows.item(i).tbl_name);
          db.transaction(function(tx){
            for(let tblName of tables){
              if(tblName.toLowerCase() === "MergeIdentity".toLowerCase()){
                tx.executeSql("UPDATE MergeIdentity SET MergeUpdate=0 WHERE MergeUpdate > 0;", [], null, null);
              }
              if(tblName.toLowerCase() !== "MergeDelete".toLowerCase() && tblName.toLowerCase() !== "MergeIdentity".toLowerCase()){
                query = "SELECT sql FROM sqlite_master WHERE type='trigger' AND name LIKE 'trMergeUpdate_" + tblName + "'";
                let updTriggerSQL = null;
                tx.executeSql(query, [],
                  function(transaction, result) {
                    updTriggerSQL = result.rows.item(0)[0];
                    if (updTriggerSQL != null){
                      tx.executeSql("DROP trigger trMergeUpdate_" + tblName, [], null, null);
                      tx.executeSql("UPDATE " + tblName + " SET MergeUpdate=0 WHERE MergeUpdate > 0;", [], null, null);
                      tx.executeSql(updTriggerSQL, [], null, null);
                    }
                  }, function(transaction, error){
                });
              }
            }
            tx.executeSql("DELETE FROM MergeDelete;",[],null,null);
          }).then(() => {
            resolve();
          }).catch((err) => {
            reject("Error while clearing local markers");
          });
        }).catch((err) => {
        reject("Error while getting tables from local database!");
        });
      }).catch((err) => {
        reject("Error while connecting to database!");
      });
    });
  }


  getRemoteChangesForTable(syncUrl, subscriberId, tableName) {
    return new Promise((resolve, reject) => {
      let url = syncUrl + "/Sync/" + subscriberId + "/" + tableName;
      this.http.get(url).map(data => data.json()).subscribe((data) => {
        this.ResponseToSqliteSyncObject(data).then((result) => {
          let SqliteSyncObject = JSON.parse(JSON.stringify(result));
          if(SqliteSyncObject.SyncId !== -1) {
            this.openDatabase().then((db: SQLiteObject) => {
              db.transaction(function(tx){
                if(SqliteSyncObject.TriggerInsertDrop.length > 0){
                  tx.executeSql(SqliteSyncObject.TriggerInsertDrop, [], null, null);
                }
                if(SqliteSyncObject.TriggerUpdateDrop.length > 0){
                  tx.executeSql(SqliteSyncObject.TriggerUpdateDrop, [], null, null);
                }
                if(SqliteSyncObject.TriggerDeleteDrop.length > 0){
                  tx.executeSql(SqliteSyncObject.TriggerDeleteDrop, [], null, null);
                }
                for(let record of SqliteSyncObject.Records){
                  switch(record.Action){
                    case '1':
                      tx.executeSql(SqliteSyncObject.QueryInsert, record.Columns, null, null);
                      break;
                    case '2':
                      tx.executeSql(SqliteSyncObject.QueryUpdate, record.Columns, null, null);
                      break;
                    case '3':
                      tx.executeSql(SqliteSyncObject.QueryDelete + "?", record.Columns, null, null);
                      break;
                  }
                }

                if(SqliteSyncObject.TriggerInsert.length > 0){
                    tx.executeSql(SqliteSyncObject.TriggerInsert, [], null, null);
                }
                if(SqliteSyncObject.TriggerUpdate.length > 0){
                    tx.executeSql(SqliteSyncObject.TriggerUpdate, [], null, null);
                }
                if(SqliteSyncObject.TriggerDelete.length > 0){
                    tx.executeSql(SqliteSyncObject.TriggerDelete, [], null, null);
                }

              }).then(() => {
                this.CommmitSynchronization(syncUrl, SqliteSyncObject.SyncId).then(() => {
                  resolve();
                }).catch((err) => {
                  reject(err);
                });
              }).catch((err) => {
                reject("Error while syncing databases!");
              });
            }).catch((err) => {
              reject("Error while connecting to database!");
            });
          }
          else{
            resolve();
          }
        });
      }, (err) => {
        reject("Error while connecting to server!");
      })

    });
  }

  CommmitSynchronization(syncUrl, syncId){

    return new Promise((resolve, reject) => {
      let url = syncUrl + "/CommitSync/" + syncId;
      this.http.get(url).subscribe(() => {
        resolve();
      }, (err) => {
        reject("Error while commiting synchronization");
      })
    })
  }

  ResponseToSqliteSyncObject(data){
    return new Promise((resolve) => {
      let SqliteSyncObject = {
        SyncId: data[0].SyncId,
        TableName: data[0].TableName,
        Records: [],
        QueryInsert: data[0].QueryInsert,
        QueryUpdate: data[0].QueryUpdate,
        QueryDelete: data[0].QueryDelete,
        TriggerInsert: data[0].TriggerInsert,
        TriggerUpdate: data[0].TriggerUpdate,
        TriggerDelete: data[0].TriggerDelete,
        TriggerInsertDrop: data[0].TriggerInsertDrop,
        TriggerUpdateDrop: data[0].TriggerUpdateDrop,
        TriggerDeleteDrop: data[0].TriggerDeleteDrop,
      }

      xml2js.parseString(data[0].Records, function (err, result) {
        if(result && result.records){
          for(let i = 0; i < result.records.r.length; i++){
            let record = {
              Action: result.records.r[i].$.a,
              Columns: []
            };
            for(let ii = 0; ii < result.records.r[i].c.length; ii++){
              record.Columns.push(result.records.r[i].c[ii]);
            }
            SqliteSyncObject.Records.push(record);
          }
          resolve(SqliteSyncObject);
        }
        else{
          resolve(SqliteSyncObject);
        }
      });
    })
  }



}
