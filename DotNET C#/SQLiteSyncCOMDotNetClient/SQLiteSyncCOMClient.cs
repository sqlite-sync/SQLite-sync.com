using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SQLite;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using AuroraSyncService;
using Newtonsoft.Json;
using RestSharp;
using System.Linq;

namespace SQLiteSyncCOMCsharp
{
    public class SQLiteSyncCOMClient
    {
        RestClient wsClient;
        private string connString = null;

        public SQLiteSyncCOMClient(string connectionString, string url)
        {
            this.connString = connectionString;
            wsClient = new RestClient(url);
        }

        public void ReinitializeDatabase(string subscriberId)
        {
            //getting data from server
            var request = new RestRequest("InitializeSubscriber/{subscriberUUID}", Method.GET);
            request.AddUrlSegment("subscriberUUID", subscriberId);
            request.AddHeader("Accept", "*/*");
            IRestResponse response = wsClient.Execute(request);
            Dictionary<string, string> dbSchema = JsonConvert.DeserializeObject<Dictionary<string, string>>(response.Content);
            
            //we need to sort by key
            var tmp = dbSchema.OrderBy(key => key.Key);
            var dbSchemaSorted = tmp.ToDictionary((keyItem) => keyItem.Key, (valueItem) => valueItem.Value);

            using (SQLiteConnection conn = new SQLiteConnection(this.connString))
            {
                using (SQLiteCommand cmd = new SQLiteCommand())
                {
                    cmd.Connection = conn;
                    conn.Open();

                    SQLiteHelper sh = new SQLiteHelper(cmd);

                    sh.BeginTransaction();

                    try
                    {
                        foreach (KeyValuePair<string, string> entry in dbSchemaSorted)
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
            using (SQLiteConnection conn = new SQLiteConnection(this.connString))
            {
                using (SQLiteCommand cmd = new SQLiteCommand())
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
                                            if (record[column.ColumnName].GetType().Name == "Byte[]")
                                                sqlitesync_SyncDataToSend.Append("<![CDATA[" + Convert.ToBase64String((byte[])record[column.ColumnName]) + "]]>");
                                            else
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

                    #region send changes to server
                    JsonObject inputObject = new JsonObject();
                    inputObject.Add("subscriber", subscriberId);
                    inputObject.Add("content", sqlitesync_SyncDataToSend.ToString());
                    inputObject.Add("version", "3");

                    System.Text.UTF8Encoding encoding = new System.Text.UTF8Encoding();
                    byte[] bytes = encoding.GetBytes(inputObject.ToString());

                    var request = new RestRequest("Send", Method.POST);
                    request.AddHeader("Content-Type", "application/json");
                    request.AddHeader("Accept", "*/*");
                    request.AddHeader("charset", "utf-8");
                    request.AddHeader("Content-Length", bytes.Length.ToString());

                    request.AddParameter("application/json; charset=utf-8", inputObject.ToString(), ParameterType.RequestBody);
                    request.RequestFormat = DataFormat.Json;
              
                    IRestResponse response = wsClient.Execute(request);
                    #endregion

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
            using (SQLiteConnection conn = new SQLiteConnection(this.connString))
            {
                using (SQLiteCommand cmd = new SQLiteCommand())
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

                            var request = new RestRequest("Sync/{subscriberUUID}/{tableName}", Method.GET);
                            request.AddUrlSegment("subscriberUUID", subscriberId);
                            request.AddUrlSegment("tableName", table["tbl_Name"].ToString());
                            request.AddHeader("Accept", "*/*");
                            IRestResponse response = wsClient.Execute(request);
                            List<DataObject> tablesData = JsonConvert.DeserializeObject<List<DataObject>>(response.Content);

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

                                        SQLiteParameter[] parameters = new SQLiteParameter[coumnsCount];
                                        int idx = 0;
                                        foreach (XPathNavigator oCurrentColumn in oColumnsNodesIterator)
                                        {
                                            SQLiteParameter parameter = new SQLiteParameter();
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

                                    request = new RestRequest("CommitSync/{syncId}", Method.GET);
                                    request.AddUrlSegment("syncId", tableData.SyncId.ToString());
                                    request.AddHeader("Accept", "*/*");
                                    IRestResponse responseCommit = wsClient.Execute(request);
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
