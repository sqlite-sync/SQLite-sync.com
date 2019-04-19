var sqlitesync_SyncServerURL = '';
var sqlitesync_SyncTableList = [];
var sqlitesync_SyncTableCurrentIndex = 0;
var sqlitesync_SyncGlobalTableIndex = 0;
var sqlitesync_SyncDataToSend;
var sqlitesync_DB;

var sqlitesync_syncPdaIdent = null;

/**
 * Synchronize (send/recieve) changes with server, we start here with synchronization process
 */

function sqlitesync_SyncSendAndReceive(syncServer, syncPdaIdent) {
    sqlitesync_SyncServerURL = syncServer;
    sqlitesync_syncPdaIdent = syncPdaIdent;

    sqlitesync_SyncTableList = [];
    sqlitesync_SyncTableCurrentIndex = 0;

    sqlitesync_AddLog('Starting synchronization. Trying connect to the server...');
    sqlitesync_DB.transaction(function (transaction,results) {

        var syncMethod = sqlitesync_SyncServerURL + "Sync/" + syncPdaIdent + "/";

        transaction.executeSql("select tbl_Name, ? as syncMethod, ? as sqlitesync_syncPdaIdent from sqlite_master where type='table'", [syncMethod, sqlitesync_syncPdaIdent],
            function(transaction, result){

                for (var tableIndex = 0; tableIndex < result.rows.length; tableIndex++) {
					if(result.rows.item(tableIndex)['tbl_name'] != "__WebKitDatabaseInfoTable__" && result.rows.item(tableIndex)['tbl_name'] != "MergeDelete")
					{
						sqlitesync_SyncTableList.push({
							table: result.rows.item(tableIndex)['tbl_name'],
							syncMethod: result.rows.item(tableIndex)['syncMethod']+ result.rows.item(tableIndex)['tbl_name'],
							sqlitesync_syncPdaIdent: result.rows.item(tableIndex)['sqlitesync_syncPdaIdent']
						});
					}
                }
            },function(error){//error
                sqlitesync_AddLog('<p>Error while syncing with the server ' + error + '</p>');
            }, function(){
            });

    },function(error){//error
        sqlitesync_AddLog('<p>Error while syncing with the server ' + error + '</p>');
    }, function(){
		sqlitesync_SyncSendData();
    });

}

/**
 * Add table to synchronization
 * @param url server url
 * @param tableName table name
 */
function sqlitesync_AddTableToSync(url, tableName){
	$.ajax({
	    url: url + "AddTable/" + tableName,
	    method: 'GET',
	    scope:this,
	    cache : false,
	    timeout: 5 * 60 * 1000,
	    success: function(){

	    },
	    failure: function (result, request) {

	    }
	});
}

function sqlitesync_SyncTables(){

    var syncLogin = null;
    var syncPass = null;

    $.ajax({
        url: sqlitesync_SyncTableList[sqlitesync_SyncTableCurrentIndex].syncMethod, //Url of the Service
        method: 'GET',
        cache : false,
        scope:this,
        timeout: 5 * 60 * 1000,//10min
        success: function (response, status) { //Success Callback
			if(response.trim().length != 0)
			{
				var responseReturn = JSON.parse(response);
				var tableNameSync = '';
				var syncId = null;
				sqlitesync_DB.transaction(function (tx) {

					var queryInsert = null;
					var queryUpdate = null;
					var queryDelete = null;

					if (responseReturn[0].SyncId > 0) {
						tableNameSync = responseReturn[0].TableName;
						sqlitesync_AddLog('<p>Preparing changes for table <b>' + responseReturn[0].TableName + '</b></p>');
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

						for (var i = 0; i < xmlDoc.childNodes[0].childElementCount; i++) {

							var rowValues = new Array();
							var query = null;
							var colCount = 0;

							for (var ii = 0; ii < xmlDoc.childNodes[0].childNodes[i].childElementCount; ii++) {
								rowValues[ii] = xmlDoc.childNodes[0].childNodes[i].childNodes[ii].textContent;
								colCount++;
							}

							var rowId = rowValues[colCount - 1];
							var identityCol = rowValues[0];

							switch (xmlDoc.childNodes[0].childNodes[i].getAttribute("a")) {
								case "1":
									tx.executeSql(queryInsert,
										rowValues,
										function (transaction, result) {

										},
										function (transaction, error) {
											console.log(error);
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

					}

				}, function(error){//error
					sqlitesync_AddLog('<p>Error while syncing with the server ' + error + '</p>');
					sqlitesync_SyncTableCurrentIndex++
					if(sqlitesync_SyncTableCurrentIndex < sqlitesync_SyncTableList.length)
						sqlitesync_SyncTables();
					else
						sqlitesync_SyncEnd();

				}, function(){//success
					if(syncId > 0){
						$.ajax({
							url: sqlitesync_SyncServerURL + "CommitSync/"+syncId,
							method: 'GET',
							scope:this,
							cache : false,
							timeout: 5 * 60 * 1000,//10min
							success: function(){
								sqlitesync_AddLog('<p style=\"font-weight:bold; color:blue;\">Received the table '+tableNameSync+'.</p>');
								sqlitesync_SyncTableCurrentIndex++;

								if(sqlitesync_SyncTableCurrentIndex < sqlitesync_SyncTableList.length)
									sqlitesync_SyncTables();
								else
									sqlitesync_SyncEnd();
							},
							failure: function (result, request) {
								var statusCode = result.status;
								var responseText = result.responseText;
								sqlitesync_AddLog('<p>Error while syncing with the server ' + error + '</p>');
								sqlitesync_SyncTableCurrentIndex++

								if(sqlitesync_SyncTableCurrentIndex < sqlitesync_SyncTableList.length)
									sqlitesync_SyncTables();
								else
									sqlitesync_SyncEnd();
							}
						});
					} else{
						sqlitesync_SyncTableCurrentIndex++;

						if(sqlitesync_SyncTableCurrentIndex < sqlitesync_SyncTableList.length)
							sqlitesync_SyncTables();
						else
							sqlitesync_SyncEnd();
					}
				});
			} else {
				sqlitesync_SyncTableCurrentIndex++
				if(sqlitesync_SyncTableCurrentIndex < sqlitesync_SyncTableList.length)
					sqlitesync_SyncTables();
				else
					sqlitesync_SyncEnd();
			}
        },
        failure: function (result, request) {
            var statusCode = result.status;
            var responseText = result.responseText;
            sqlitesync_AddLog('<p>Error while syncing with the server ' + responseText + '</p>');
        }
    });
}

function sqlitesync_SyncSendData() {
    sqlitesync_AddLog('<b>Starting sending data</b>');

    sqlitesync_SyncGlobalTableIndex = 0;
    sqlitesync_SyncDataToSend = "<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">";
    sqlitesync_SyncSendTable(sqlitesync_SyncGlobalTableIndex);
}

function sqlitesync_SyncSendTable(tableIndex) {
    var selectAllStatement = "select * from sqlite_master where type='table' and sql like '%RowId%'";
    sqlitesync_DB.transaction(function (tx) {
        tx.executeSql(selectAllStatement, [], function (tx, result) {
            var datasetTables = result.rows;
            var item = datasetTables.item(tableIndex);
            if (item['tbl_name'] != "MergeDelete") {

                sqlitesync_SyncDataToSend += "<tab n=\"" + item['tbl_name'] + "\">";

                var columnParts = datasetTables.item(tableIndex).sql.replace(/^[^\(]+\(([^\)]+)\)/g, '$1').split(',');
                var columnNames = [];
                for (var col in columnParts) {
                    if (typeof columnParts[col] === 'string')
                        columnNames.push(columnParts[col].split(" ")[0]);
                }

                /*** nowe rekordy***/
                var selectForTable = "select * from " + item['tbl_name'] + " where RowId is null";
                var tableName = item['tbl_name'];
                tx.executeSql(selectForTable, [], function (tx, result) {
                    var datasetTable = result.rows;

                    if (datasetTable.length > 0){
                        sqlitesync_AddLog('Sending a new records for the table: ' + item['tbl_name'] + '');
                    }
                    sqlitesync_SyncDataToSend += "<ins>";
                    for (var i = 0, row = null; i < datasetTable.length; i++) {
                        row = datasetTable.item(i);
                        sqlitesync_SyncDataToSend += "<r>";
                        for (var j = 0; j < columnNames.length; j++) {
                            if (columnNames[j].replace(/"/gi, '') != 'MergeUpdate') {
                                sqlitesync_SyncDataToSend += "<" + columnNames[j].replace(/"/gi, '') + ">";
                                sqlitesync_SyncDataToSend += "<![CDATA[" + row[columnNames[j].replace(/"/gi, '')] + "]]>";
                                sqlitesync_SyncDataToSend += "</" + columnNames[j].replace(/"/gi, '') + ">";
                            }
                        }
                        sqlitesync_SyncDataToSend += "</r>";
                    }
                    sqlitesync_SyncDataToSend += "</ins>";
                    /*** zakutalizowane rekordy***/
                    var selectForUpdateTable = "select * from " + item['tbl_name'] + " where MergeUpdate > 0 and RowId is not null";
                    tx.executeSql(selectForUpdateTable, [], function (tx, result) {
                        datasetTable = result.rows;

                        if (datasetTable.length > 0){
                            sqlitesync_AddLog('Sending updated records for the table: ' + item['tbl_name'] + '');
                        }

                        sqlitesync_SyncDataToSend += "<upd>";
                        for (var i = 0, row = null; i < datasetTable.length; i++) {
                            row = datasetTable.item(i);
                            sqlitesync_SyncDataToSend += "<r>";
                            for (var j = 0; j < columnNames.length; j++) {
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
                        sqlitesync_SyncGlobalTableIndex++;
                        if (sqlitesync_SyncGlobalTableIndex < datasetTables.length)
                            sqlitesync_SyncSendTable(sqlitesync_SyncGlobalTableIndex);
                        else
                            sqlitesync_SyncSendTableDelete(tx);
                    });
                    /***/
                });
                /***/
            }
            else {
                sqlitesync_SyncGlobalTableIndex++;
                if (sqlitesync_SyncGlobalTableIndex < datasetTables.length)
                    sqlitesync_SyncSendTable(sqlitesync_SyncGlobalTableIndex);
                else
                    sqlitesync_SyncSendTableDelete(tx);
            }
        });
    });
}

/*
 * After synchronization, we need to clear marker for updated records to make sure that we will not send it again
 */
function sqlitesync_SyncClearUpdateMarker() {
    var selectAllStatement = "select * from sqlite_master where type='table' and sql like '%RowId%'";
    sqlitesync_DB.transaction(function (tx) {
        tx.executeSql(selectAllStatement, [], function (tx, result) {
            var datasetTables = result.rows;
            for(var tableIndex=0; tableIndex<datasetTables.length; tableIndex++){
	            var item = datasetTables.item(tableIndex);
				if (item['tbl_name'].toString().toLowerCase() == "mergeidentity") {
					
					tx.executeSql("update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;", [],function (transaction, result) {},function (transaction, error) {console.log(error);});
				}

	            if (item['tbl_name'] != "MergeDelete" && item['tbl_name'] != "MergeIdentity") {
	                var selectForUpdateTable = "select *, ? as syncTableName from " + item['tbl_name'] + " where MergeUpdate > 0";
	                tx.executeSql(selectForUpdateTable, [item['tbl_name']], function (tx, result) {
	                	if(result.rows.length > 0){
	                		var syncTableName = result.rows.item(0)['syncTableName'];
	                		var selectTriggerStatement = "select * from sqlite_master where type='trigger' and name = 'trMergeUpdate_"+syncTableName+"'";
	    	                tx.executeSql(selectTriggerStatement, [], function (tx, result) {
	    	                	if(result.rows.length > 0){
	    	                		var syncTableName = result.rows.item(0)['tbl_name'];
	    	                		var trigger = result.rows.item(0)['sql'];

	    	                		tx.executeSql("drop trigger trMergeUpdate_"+syncTableName+";", [],function (transaction, result) {

                                    },
                                    function (transaction, error) {
                                    });
	    	                		tx.executeSql("update " + syncTableName + " set MergeUpdate=0 where MergeUpdate > 0;", [],function (transaction, result) {

                                    },
                                    function (transaction, error) {
                                    });
	    	                		tx.executeSql(trigger, [],function (transaction, result) {

                                    },
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

/*
 * After synchronization, we need to clear marker for deleted records to make sure that we will not send it again
 */
function sqlitesync_SyncClearDeletedRecords() {
    var selectAllStatement = "delete from MergeDelete";
    sqlitesync_DB.transaction(function (tx) {
        tx.executeSql(selectAllStatement, [], function (tx, result) {
        });
    });
}

/*
 * Preparing and sending records deleted on device
 */
function sqlitesync_SyncSendTableDelete(tx) {
    var selectForDelete = "select * from MergeDelete";
    tx.executeSql(selectForDelete, [], function (tx, result) {
        datasetTable = result.rows;
        sqlitesync_SyncDataToSend += "<delete>";
        if (datasetTable.length > 0) {
            sqlitesync_AddLog('Sending records for deleting');
        }
        for (var i = 0, row = null; i < datasetTable.length; i++) {
            row = datasetTable.item(i);
            sqlitesync_SyncDataToSend += "<r>";
            sqlitesync_SyncDataToSend += "<tb>" + row['TableId'] + "</tb>";
            sqlitesync_SyncDataToSend += "<id>" + row['RowId'] + "</id>";
            sqlitesync_SyncDataToSend += "</r>";
        }
        sqlitesync_SyncDataToSend += "</delete>";

		sqlitesync_SyncSendToServer();
    });
}

/*
 * Send packge with changes to server
 */
function sqlitesync_SyncSendToServer() {
    sqlitesync_SyncDataToSend += "</SyncData>";

    $.ajax({
        url: sqlitesync_SyncServerURL + "Send",
        method: 'POST',
        headers: {
                'Content-Type':'application/json'
        },
        cache : false,
        scope:this,
        data: JSON.stringify({ "subscriber" : sqlitesync_syncPdaIdent, "content": sqlitesync_SyncDataToSend , "version" : "3" }),
        dataType: 'json',
        timeout: 5 * 60 * 1000,//10min
        success: function (response, status) { //Success Callback

        	sqlitesync_SyncClearUpdateMarker();
        	sqlitesync_SyncClearDeletedRecords();

            sqlitesync_AddLog('Sending finished');
            //after success we are getting changes from server
            sqlitesync_SyncTables();
        },
        failure: function (result, request) {
            var statusCode = result.status;
            var responseText = result.responseText;
            sqlitesync_AddLog('<p>Error while syncing with the server ' + responseText + '</p>');
        }
    });
}

function sqlitesync_SyncGetDBSchemaChanges() {
	sqlitesync_SyncSendData();
}

/*
 * Reinitialize subscriber
 */

function sqlitesync_ReinitializeDB(syncServer, syncPdaIdent) {
    sqlitesync_SyncServerURL = syncServer;
    sqlitesync_syncPdaIdent = syncPdaIdent;
    sqlitesync_AddLog('<p>Starting synchronization</p>');
    sqlitesync_AddLog('<p>Connecting to server...</p>');
    $.ajax({
        url: sqlitesync_SyncServerURL + "InitializeSubscriber/" + sqlitesync_syncPdaIdent,
        method: 'GET',
        scope: this,
        cache: false,
        timeout: 10 * 60 * 1000, //4min
        success: function (response, status) { //Success Callback
            var responseReturn = JSON.parse(response);

            sqlitesync_AddLog('<p>Connected to server...</p>');
            sqlitesync_DB.transaction(function (tx) {

                Object.keys(responseReturn)
                      .sort()
                      .forEach(function(v, i) {
                            tx.executeSql(responseReturn[v],
                                null,
                                function (transaction, result) {
                                    sqlitesync_AddLog('Creating object <b>' + v + '</b>...');
                                },
                                function (transaction, error) {
                                    console.log('Object ' + v  + '; ' + error.message + ' (Code ' + error.code + ')');
                                });
                       });

            }, function (error) {//error
                sqlitesync_AddLog('<p>Error while syncing with the server ' + error + '</p>');
            }, function () {
                sqlitesync_AddLog('<p style=\"font-weight:bold; color:green;\">Synchronization completed</p>');
            });

        },
        failure: function (result, request) {
            var statusCode = result.status;
            var responseText = result.responseText;
            sqlitesync_AddLog('<p>Error while syncing with the server ' + responseText + '</p>');
        }
    });
}
