using System;
using System.Collections.Generic;
using System.Data;
using System.Text;
using System.Xml.XPath;
using AuroraSyncService;
using Newtonsoft.Json;
using SQLite.Net;
using Windows.Data.Xml.Dom;
using System.Threading;
using System.Net.Http;
using System.Threading.Tasks;
using System.Reflection;
using System.Net.Http.Headers;

namespace SQLiteSyncCOMCsharp
{
    public class SQLiteSyncCOMClient
    {
        private string connString = null;
        private string sqliteSyncServiceURL;
        public SQLiteSyncCOMClient(string connectionString, string url)
        {
            this.connString = connectionString;
            this.sqliteSyncServiceURL = url;
            if (!this.sqliteSyncServiceURL.EndsWith("/"))
                this.sqliteSyncServiceURL = this.sqliteSyncServiceURL + "/";
        }

        public async Task ReinitializeDatabase(string subscriberId)
        {
            var cts = new CancellationTokenSource();
            cts.CancelAfter(TimeSpan.FromSeconds(30));

            var httpClient = new HttpClient();
            var resourceUri = new Uri(sqliteSyncServiceURL + "Sync.asmx/GetFullDBSchema?subscriber=" + JsonConvert.SerializeObject(subscriberId));

            HttpResponseMessage response = await httpClient.GetAsync(resourceUri, cts.Token);
            XmlDocument xml = new XmlDocument();
            xml.LoadXml(await response.Content.ReadAsStringAsync());
            Dictionary<string, string> dbSchema = JsonConvert.DeserializeObject<Dictionary<string, string>>(xml.ChildNodes[1].InnerText);

            using (SQLiteConnection db = new SQLiteConnection(new SQLite.Net.Platform.WinRT.SQLitePlatformWinRT(), this.connString))
            {
                db.BeginTransaction();

                try
                {
                    foreach (KeyValuePair<string, string> entry in dbSchema)
                        if (!entry.Key.StartsWith("00000"))
                        {
                            db.Execute(entry.Value, string.Empty);
                        }
                    db.Commit();
                }
                catch (Exception ex)
                {
                    db.Rollback();
                    throw ex;
                }
                finally
                {
                    db.Close();
                }
            }
        }

        private async Task SendChanges(string subscriberId)
        {
            using (SQLiteConnection db = new SQLiteConnection(new SQLite.Net.Platform.WinRT.SQLitePlatformWinRT(), this.connString))
            {
                try
                {
                    db.BeginTransaction();
                    var tables = db.Query<SqliteTable>("select tbl_Name as TableName from sqlite_master where type='table' and sql like '%RowId%';");

                    StringBuilder sqlitesync_SyncDataToSend = new StringBuilder();
                    sqlitesync_SyncDataToSend.Append("<?xml version=\"1.0\" encoding=\"utf-8\"?><SyncData xmlns=\"urn:sync-schema\">");

                    foreach (SqliteTable table in tables)
                    {
                        string tableName = table.TableName;
                        if (tableName.ToLower() != "MergeDelete".ToLower())
                        {
                            try
                            {
                                sqlitesync_SyncDataToSend.Append("<tab n=\"" + tableName + "\">");
                                List<SQLiteConnection.ColumnInfo> columns = db.GetTableInfo(tableName);
                                Type dynamicEntityType = typeof(DynamicEntity);

                                var queryColumns = "";
                                for (int i = 0; i < columns.Count; i++)
                                    queryColumns += columns[i].Name + " as col" + i.ToString() + ",";

                                if (queryColumns.Length > 0)
                                    queryColumns = queryColumns.Substring(0, queryColumns.Length - 1);

                                #region new records
                                var newRecords = db.Query<DynamicEntity>("select " + queryColumns + " from " + tableName + " where RowId is null;");
                                sqlitesync_SyncDataToSend.Append("<ins>");
                                foreach (DynamicEntity record in newRecords)
                                {
                                    sqlitesync_SyncDataToSend.Append("<r>");
                                    for (int i = 0; i < columns.Count; i++)
                                    {
                                        if (columns[i].Name != "MergeUpdate")
                                        {
                                            PropertyInfo myPropInfo = dynamicEntityType.GetProperty("col" + i.ToString());
                                            sqlitesync_SyncDataToSend.Append("<" + columns[i].Name + ">");
                                            if (myPropInfo.GetValue(record) != null)
                                                sqlitesync_SyncDataToSend.Append("<![CDATA[" + myPropInfo.GetValue(record).ToString() + "]]>");
                                            else
                                                sqlitesync_SyncDataToSend.Append("<![CDATA[]]>");
                                            sqlitesync_SyncDataToSend.Append("</" + columns[i].Name + ">");
                                        }
                                    }
                                    sqlitesync_SyncDataToSend.Append("</r>");
                                }
                                sqlitesync_SyncDataToSend.Append("</ins>");
                                #endregion

                                #region updated records
                                var updRecords = db.Query<DynamicEntity>("select " + queryColumns + " from " + tableName + " where MergeUpdate > 0 and RowId is not null;");
                                sqlitesync_SyncDataToSend.Append("<upd>");
                                foreach (DynamicEntity record in updRecords)
                                {
                                    sqlitesync_SyncDataToSend.Append("<r>");
                                    for (int i = 0; i < columns.Count; i++)
                                    {
                                        if (columns[i].Name != "MergeUpdate")
                                        {
                                            PropertyInfo myPropInfo = dynamicEntityType.GetProperty("col" + i.ToString());
                                            sqlitesync_SyncDataToSend.Append("<" + columns[i].Name + ">");
                                            if (myPropInfo.GetValue(record) != null)
                                                sqlitesync_SyncDataToSend.Append("<![CDATA[" + myPropInfo.GetValue(record).ToString() + "]]>");
                                            else
                                                sqlitesync_SyncDataToSend.Append("<![CDATA[]]>");
                                            sqlitesync_SyncDataToSend.Append("</" + columns[i].Name + ">");
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
                    var delRecords = db.Query<DeletedRecords>("select * from MergeDelete;", string.Empty);

                    sqlitesync_SyncDataToSend.Append("<delete>");
                    foreach (DeletedRecords record in delRecords)
                    {
                        sqlitesync_SyncDataToSend.Append("<r>");
                        sqlitesync_SyncDataToSend.Append("<tb>" + record.TableId + "</tb>");
                        sqlitesync_SyncDataToSend.Append("<id>" + record.RowId + "</id>");
                        sqlitesync_SyncDataToSend.Append("</r>");
                    }
                    sqlitesync_SyncDataToSend.Append("</delete>");
                    #endregion

                    sqlitesync_SyncDataToSend.Append("</SyncData>");

                    var cts = new CancellationTokenSource();
                    cts.CancelAfter(TimeSpan.FromSeconds(30));
                    var httpClient = new HttpClient();
                    httpClient.DefaultRequestHeaders
                          .Accept
                          .Add(new MediaTypeWithQualityHeaderValue("application/json"));

                    HttpRequestMessage request = new HttpRequestMessage();
                    request.RequestUri = new Uri(sqliteSyncServiceURL + "Sync.asmx/ReceiveData");
                    request.Method = HttpMethod.Post;
                    request.Content = new StringContent("{ \"subscriber\": " + JsonConvert.SerializeObject(subscriberId) + ", \"data\": " + JsonConvert.SerializeObject(sqlitesync_SyncDataToSend.ToString()) + "  }", Encoding.UTF8, "application/json");
                    HttpResponseMessage sendResponse = await httpClient.SendAsync(request);
                    // wsClient.ReceiveData(subscriberId, sqlitesync_SyncDataToSend.ToString());

                    #region clear update marker
                    foreach (SqliteTable table in tables)
                    {
                        string tableName = table.TableName.ToLower();
                        if (tableName != "MergeDelete".ToLower() && tableName != "MergeIdentity".ToLower())
                        {
                            string updTriggerSQL = (string)db.ExecuteScalar<string>("select sql from sqlite_master where type='trigger' and name like 'trMergeUpdate_" + tableName + "'");
                            db.Execute("drop trigger trMergeUpdate_" + tableName + ";", string.Empty);
                            db.Execute("update " + tableName + " set MergeUpdate=0 where MergeUpdate > 0;", string.Empty);
                            db.Execute(updTriggerSQL, string.Empty);
                        }

                        if (tableName == "MergeIdentity".ToLower())
                            db.Execute("update MergeIdentity set MergeUpdate=0 where MergeUpdate > 0;", string.Empty);
                    }
                    #endregion

                    #region clear delete marker
                    db.Execute("delete from MergeDelete", string.Empty);
                    #endregion

                    db.Commit();
                }
                catch (Exception ex)
                {
                    db.Rollback();
                    throw ex;
                }
                finally
                {
                    db.Close();
                }
            }
        }

        private async Task GetChangesFromServer(string subscriberId)
        {
            using (SQLiteConnection db = new SQLiteConnection(new SQLite.Net.Platform.WinRT.SQLitePlatformWinRT(), this.connString))
            {
                var tables = db.Query<SqliteTable>("select tbl_Name as TableName from sqlite_master where type='table';");
                foreach (SqliteTable table in tables)
                    if (table.TableName.ToLower() != "MergeDelete".ToLower())
                    {
                        var cts = new CancellationTokenSource();
                        cts.CancelAfter(TimeSpan.FromSeconds(30));

                        var httpClient = new HttpClient();
                        var resourceUri = new Uri(sqliteSyncServiceURL + "Sync.asmx/GetDataForSync?subscriber=" + JsonConvert.SerializeObject(subscriberId) + "&table=" + JsonConvert.SerializeObject(table.TableName));

                        HttpResponseMessage response = await httpClient.GetAsync(resourceUri, cts.Token);
                        XmlDocument xml = new XmlDocument();
                        xml.LoadXml(await response.Content.ReadAsStringAsync());
                        List<DataObject> tablesData = JsonConvert.DeserializeObject<List<DataObject>>(xml.ChildNodes[1].InnerText);
                        if (tablesData != null)
                            foreach (DataObject tableData in tablesData)
                                if (tableData.SyncId > 0)
                                {
                                    try
                                    {
                                        db.BeginTransaction();

                                        if (!string.IsNullOrEmpty(tableData.TriggerDeleteDrop))
                                            db.Execute(tableData.TriggerDeleteDrop, string.Empty);
                                        if (!string.IsNullOrEmpty(tableData.TriggerInsertDrop))
                                            db.Execute(tableData.TriggerInsertDrop, string.Empty);
                                        if (!string.IsNullOrEmpty(tableData.TriggerUpdateDrop))
                                            db.Execute(tableData.TriggerUpdateDrop, string.Empty);

                                        XmlDocument xmlRecords = new XmlDocument();
                                        xmlRecords.LoadXml(tableData.Records);
                                        XmlNodeList records = xmlRecords.SelectNodes("/records/r");
                                        foreach (IXmlNode oCurrentRecord in records) // for each <testcase> node
                                        {
                                            string action = oCurrentRecord.Attributes.GetNamedItem("a").InnerText;
                                            XmlNodeList columns = oCurrentRecord.ChildNodes;

                                            int coumnsCount = columns.Count;

                                            object[] parameters = new object[coumnsCount];
                                            int idx = 0;
                                            foreach (IXmlNode oCurrentColumn in columns)
                                            {
                                                parameters[idx] = oCurrentColumn.InnerText;
                                                idx++;
                                            }

                                            switch (action)
                                            {
                                                case "1"://insert
                                                    db.Execute(tableData.QueryInsert, parameters);
                                                    break;
                                                case "2"://update
                                                    db.Execute(tableData.QueryUpdate, parameters);
                                                    break;
                                                case "3"://delete
                                                    db.Execute(tableData.QueryDelete + "?", parameters);
                                                    break;
                                            }
                                        }

                                        if (!string.IsNullOrEmpty(tableData.TriggerDelete))
                                            db.Execute(tableData.TriggerDelete, string.Empty);
                                        if (!string.IsNullOrEmpty(tableData.TriggerInsert))
                                            db.Execute(tableData.TriggerInsert, string.Empty);
                                        if (!string.IsNullOrEmpty(tableData.TriggerUpdate))
                                            db.Execute(tableData.TriggerUpdate, string.Empty);

                                        var httpClientCommit = new HttpClient();
                                        resourceUri = new Uri(sqliteSyncServiceURL + "Sync.asmx/SyncCompleted?syncId=" + tableData.SyncId.ToString());
                                        HttpResponseMessage responseCommit = await httpClientCommit.GetAsync(resourceUri, cts.Token);

                                        db.Commit();
                                    }
                                    catch (Exception ex)
                                    {
                                        db.Rollback();
                                        throw ex;
                                    }
                                }
                    }
                db.Close();
            }
        }

        public async Task SendAndRecieveChanges(string subscriberId)
        {
            await SendChanges(subscriberId);
            await GetChangesFromServer(subscriberId);
        }
    }
}
