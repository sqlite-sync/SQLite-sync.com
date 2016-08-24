using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SQLite;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using Mono.Data.Sqlite;
using Newtonsoft.Json;


namespace SQLiteSyncCOMLibXamarin
{
    public class SQLiteSyncCOMClient
    {		
		SQLiteSyncCOMLibXamarin.Droid.demo_sqlite_sync_com.Sync wsClient = new SQLiteSyncCOMLibXamarin.Droid.demo_sqlite_sync_com.Sync();
        private string connString = null;

        public SQLiteSyncCOMClient(string connectionString, string url)
        {
			
            this.connString = connectionString;
            wsClient.Url = url;
        }

        public void ReinitializeDatabase(string subscriberId)
        {
            Dictionary<string, string> dbSchema = JsonConvert.DeserializeObject<Dictionary<string, string>>(wsClient.GetFullDBSchema(JsonConvert.SerializeObject(subscriberId)));
            using (SqliteConnection conn = new SqliteConnection(this.connString))
            {
                using (SqliteCommand cmd = new SqliteCommand())
                {
                    cmd.Connection = conn;
                    conn.Open();

                    SQLiteHelper sh = new SQLiteHelper(cmd);

                    sh.BeginTransaction();

                    try
                    {
                        foreach (KeyValuePair<string, string> entry in dbSchema)
                            if (!entry.Key.StartsWith("00000"))
                            {
                                sh.Execute(entry.Value);
                            }
                        sh.Commit();
                    }
                    catch (Exception ex)
                    {
                        sh.Rollback();
                        throw ex;
                    }

                    conn.Close();
                }
            }
        }

        private void SendChanges(string subscriberId)
        {
            using (SqliteConnection conn = new SqliteConnection(this.connString))
            {
                using (SqliteCommand cmd = new SqliteCommand())
                {
                    cmd.Connection = conn;
                    conn.Open();

                    SQLiteHelper sh = new SQLiteHelper(cmd);

                    DataTable tables = sh.Select("select tbl_Name from sqlite_master where type='table' and sql like '%RowId%';");

                    StringBuilder sqlitesync_SyncDataToSend = new StringBuilder();
                    sqlitesync_SyncDataToSend.Append("<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">");

                    foreach (DataRow table in tables.Rows)
                    {
                        string tableName = table["tbl_Name"].ToString();
                        if (tableName.ToLower() != "MergeDelete".ToLower())
                        {
                            try
                            {
                                sqlitesync_SyncDataToSend.Append("<tab n=\"" + tableName + "\">");

                                #region new records
                                DataTable newRecords = sh.Select("select * from " + tableName + " where RowId is null;");
                                sqlitesync_SyncDataToSend.Append("<ins>");
                                foreach (DataRow record in newRecords.Rows)
                                {
                                    sqlitesync_SyncDataToSend.Append("<r>");
                                    foreach (DataColumn column in newRecords.Columns)
                                    {
                                        if (column.ColumnName != "MergeUpdate")
                                        {
                                            sqlitesync_SyncDataToSend.Append("<" + column.ColumnName + ">");
                                            sqlitesync_SyncDataToSend.Append("<![CDATA[" + record[column.ColumnName].ToString() + "]]>");
                                            sqlitesync_SyncDataToSend.Append("</" + column.ColumnName + ">");
                                        }
                                    }
                                    sqlitesync_SyncDataToSend.Append("</r>");
                                }
                                sqlitesync_SyncDataToSend.Append("</ins>");
                                #endregion

                                #region updated records
                                DataTable updRecords = sh.Select("select * from " + tableName + " where MergeUpdate > 0 and RowId is not null;");
                                sqlitesync_SyncDataToSend.Append("<upd>");
                                foreach (DataRow record in updRecords.Rows)
                                {
                                    sqlitesync_SyncDataToSend.Append("<r>");
                                    foreach (DataColumn column in updRecords.Columns)
                                    {
                                        if (column.ColumnName != "MergeUpdate")
                                        {
                                            sqlitesync_SyncDataToSend.Append("<" + column.ColumnName + ">");
                                            sqlitesync_SyncDataToSend.Append("<![CDATA[" + record[column.ColumnName].ToString() + "]]>");
                                            sqlitesync_SyncDataToSend.Append("</" + column.ColumnName + ">");
                                        }
                                    }
                                    sqlitesync_SyncDataToSend.Append("</r>");
                                }
                                sqlitesync_SyncDataToSend.Append("</upd>");
                                #endregion

                                sqlitesync_SyncDataToSend.Append("</tab>");
                            }
                            catch (Exception ex)
                            {
                                throw ex;
                            }
                        }
                    }

                    #region deleted records
                    DataTable delRecords = sh.Select("select * from MergeDelete;");
                    sqlitesync_SyncDataToSend.Append("<delete>");
                    foreach (DataRow record in delRecords.Rows)
                    {
                        sqlitesync_SyncDataToSend.Append("<r>");
                        sqlitesync_SyncDataToSend.Append("<tb>" + record["TableId"].ToString() + "</tb>");
                        sqlitesync_SyncDataToSend.Append("<id>" + record["RowId"].ToString() + "</id>");
                        sqlitesync_SyncDataToSend.Append("</r>");
                    }
                    sqlitesync_SyncDataToSend.Append("</delete>");
                    #endregion

                    sqlitesync_SyncDataToSend.Append("</SyncData>");

                    wsClient.ReceiveData(subscriberId, sqlitesync_SyncDataToSend.ToString());

                    #region clear update marker
                    foreach (DataRow table in tables.Rows)
                    {
                        string tableName = table["tbl_Name"].ToString().ToLower();
                        if (tableName != "MergeDelete".ToLower() && tableName != "MergeIdentity".ToLower())
                        {
                            string updTriggerSQL = (string)sh.ExecuteScalar("select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_" + tableName + "'");
                            sh.Execute("drop trigger trMergeUpdate_" + tableName + ";");
                            sh.Execute("update " + tableName + " set MergeUpdate=0 where MergeUpdate > 0;");
                            sh.Execute(updTriggerSQL);
                        }

						if (tableName == "MergeIdentity".ToLower())
							sh.Execute("update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;");
                    }
                    #endregion

                    #region clear delete marker
                    sh.Execute("delete from MergeDelete");
                    #endregion

                    conn.Close();
                }
            }
        }

        private void GetChangesFromServer(string subscriberId)
        {
            using (SqliteConnection conn = new SqliteConnection(this.connString))
            {
                using (SqliteCommand cmd = new SqliteCommand())
                {
                    cmd.Connection = conn;
                    conn.Open();

                    SQLiteHelper sh = new SQLiteHelper(cmd);

                    DataTable tables = sh.Select("select tbl_Name from sqlite_master where type='table';");

                    foreach (DataRow table in tables.Rows)
                    {
                        try
                        {
                            sh.BeginTransaction();

                            List<DataObject> tablesData = JsonConvert.DeserializeObject<List<DataObject>>(wsClient.GetDataForSync(JsonConvert.SerializeObject(subscriberId), JsonConvert.SerializeObject(table["tbl_Name"].ToString())));
                            foreach (DataObject tableData in tablesData)
                                if (tableData.SyncId > 0)
                                {
                                    sh.Execute(tableData.TriggerDeleteDrop);
                                    sh.Execute(tableData.TriggerInsertDrop);
                                    sh.Execute(tableData.TriggerUpdateDrop);

                                    XmlDocument xmlRecords = new XmlDocument();
                                    xmlRecords.LoadXml(tableData.Records);
                                    XPathNavigator oRecordsPathNavigator = xmlRecords.CreateNavigator();
                                    XPathNodeIterator oRecordsNodesIterator = oRecordsPathNavigator.Select("/records/r");

                                    foreach (XPathNavigator oCurrentRecord in oRecordsNodesIterator)
                                    {
                                        string action = oCurrentRecord.GetAttribute("a", "");
                                        XmlDocument xmlRecord = new XmlDocument();
                                        xmlRecord.LoadXml("<?xml version=\"1.0\" encoding=\"utf-8\"?><columns>" + oCurrentRecord.InnerXml + "</columns>");
                                        XPathNavigator oColumnsPathNavigator = xmlRecord.CreateNavigator();
                                        XPathNodeIterator oColumnsNodesIterator = oColumnsPathNavigator.Select("/columns/c");
                                        int coumnsCount = oColumnsNodesIterator.Count;

                                        SqliteParameter[] parameters = new SqliteParameter[coumnsCount];
                                        int idx = 0;
                                        foreach (XPathNavigator oCurrentColumn in oColumnsNodesIterator)
                                        {
                                            SqliteParameter parameter = new SqliteParameter();
                                            parameter.Value = oCurrentColumn.InnerXml;
                                            parameters[idx] = parameter;
                                            idx++;
                                        }

                                        switch (action)
                                        {
                                            case "1"://insert
                                                sh.Execute(tableData.QueryInsert, parameters);
                                                break;
                                            case "2"://update
                                                sh.Execute(tableData.QueryUpdate, parameters);
                                                break;
                                            case "3"://delete
                                                sh.Execute(tableData.QueryDelete, parameters);
                                                break;
                                        }
                                    }

                                    sh.Execute(tableData.TriggerDelete);
                                    sh.Execute(tableData.TriggerInsert);
                                    sh.Execute(tableData.TriggerUpdate);

                                    wsClient.SyncCompleted(JsonConvert.SerializeObject(tableData.SyncId));
                                }

                            sh.Commit();

                        }
                        catch (Exception ex)
                        {
                            sh.Rollback();
                            throw ex;
                        }
                    }

                    conn.Close();
                }
            }
        }

        public void SendAndRecieveChanges(string subscriberId)
        {
            SendChanges(subscriberId);
            GetChangesFromServer(subscriberId);
        }

    }
}
