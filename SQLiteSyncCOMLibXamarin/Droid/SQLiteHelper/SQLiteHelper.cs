// Version 1.2
// Date: 2014-03-27
// http://sh.codeplex.com
// Dedicated to Public Domain

using System;
using System.Collections.Generic;
using System.Text;
using System.Data;
using System.Globalization;
using Mono.Data.Sqlite;

namespace System.Data.SQLite
{
    public enum ColType
    {
        Text,
        DateTime,
        Integer,
        Decimal,
        BLOB
    }

    public class SQLiteHelper
    {
        SqliteCommand cmd = null;

        public SQLiteHelper(SqliteCommand command)
        {
            cmd = command;
        }

        #region DB Info

        public DataTable GetTableStatus()
        {
            return Select("SELECT * FROM sqlite_master;");
        }

        public DataTable GetTableList()
        {
            DataTable dt = GetTableStatus();
            DataTable dt2 = new DataTable();
            dt2.Columns.Add("Tables");
            for (int i = 0; i < dt.Rows.Count; i++)
            {
                string t = dt.Rows[i]["name"] + "";
                if (t != "sqlite_sequence")
                    dt2.Rows.Add(t);
            }
            return dt2;
        }

        public DataTable GetColumnStatus(string tableName)
        {
            return Select(string.Format("PRAGMA table_info(`{0}`);", tableName));
        }

        public DataTable ShowDatabase()
        {
            return Select("PRAGMA database_list;");
        }

        #endregion

        #region Query

        public void BeginTransaction()
        {
            cmd.CommandText = "begin transaction;";
            cmd.ExecuteNonQuery();
        }

        public void Commit()
        {
            cmd.CommandText = "commit;";
            cmd.ExecuteNonQuery();
        }

        public void Rollback()
        {
            cmd.CommandText = "rollback";
            cmd.ExecuteNonQuery();
        }

        public DataTable Select(string sql)
        {
            return Select(sql, new List<SqliteParameter>());
        }

        public DataTable Select(string sql, Dictionary<string, object> dicParameters = null)
        {
            List<SqliteParameter> lst = GetParametersList(dicParameters);
            return Select(sql, lst);
        }

        public DataTable Select(string sql, IEnumerable<SqliteParameter> parameters = null)
        {
            cmd.CommandText = sql;
            if (parameters != null)
            {
                foreach (var param in parameters)
                {
                    cmd.Parameters.Add(param);
                }
            }
            SqliteDataAdapter da = new SqliteDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);
            return dt;
        }

        public void Execute(string sql)
        {
            Execute(sql, new List<SqliteParameter>());
        }

        public void Execute(string sql, Dictionary<string, object> dicParameters = null)
        {
            List<SqliteParameter> lst = GetParametersList(dicParameters);
            Execute(sql, lst);
        }

        public void Execute(string sql, IEnumerable<SqliteParameter> parameters = null)
        {
            cmd.CommandText = sql;
            if (parameters != null)
            {
                if (cmd.Parameters.Count > 0)
                    cmd.Parameters.Clear();

                foreach (var param in parameters)
                {
                    cmd.Parameters.Add(param);
                }
            }
            cmd.ExecuteNonQuery();
        }

        public object ExecuteScalar(string sql)
        {
            cmd.CommandText = sql;
            return cmd.ExecuteScalar();
        }

        public object ExecuteScalar(string sql, Dictionary<string, object> dicParameters = null)
        {
            List<SqliteParameter> lst = GetParametersList(dicParameters);
            return ExecuteScalar(sql, lst);
        }

        public object ExecuteScalar(string sql, IEnumerable<SqliteParameter> parameters = null)
        {
            cmd.CommandText = sql;
            if (parameters != null)
            {
                if (cmd.Parameters.Count > 0)
                    cmd.Parameters.Clear();
                foreach (var parameter in parameters)
                {
                    cmd.Parameters.Add(parameter);
                }
            }
            return cmd.ExecuteScalar();
        }

        public dataType ExecuteScalar<dataType>(string sql, Dictionary<string, object> dicParameters = null)
        {
            List<SqliteParameter> lst = null;
            if (dicParameters != null)
            {
                lst = new List<SqliteParameter>();
                foreach (KeyValuePair<string, object> kv in dicParameters)
                {
                    lst.Add(new SqliteParameter(kv.Key, kv.Value));
                }
            }
            return ExecuteScalar<dataType>(sql, lst);
        }

        public dataType ExecuteScalar<dataType>(string sql, IEnumerable<SqliteParameter> parameters = null)
        {
            cmd.CommandText = sql;
            if (parameters != null)
            {
                foreach (var parameter in parameters)
                {
                    cmd.Parameters.Add(parameter);
                }
            }
            return (dataType)Convert.ChangeType(cmd.ExecuteScalar(), typeof(dataType));
        }

        public dataType ExecuteScalar<dataType>(string sql)
        {
            cmd.CommandText = sql;
            return (dataType)Convert.ChangeType(cmd.ExecuteScalar(), typeof(dataType));
        }

        private List<SqliteParameter> GetParametersList(Dictionary<string, object> dicParameters)
        {
            List<SqliteParameter> lst = new List<SqliteParameter>();
            if (dicParameters != null)
            {
                foreach (KeyValuePair<string, object> kv in dicParameters)
                {
                    lst.Add(new SqliteParameter(kv.Key, kv.Value));
                }
            }
            return lst;
        }

        public string Escape(string data)
        {
            data = data.Replace("'", "''");
            data = data.Replace("\\", "\\\\");
            return data;
        }

        public void Insert(string tableName, Dictionary<string, object> dic)
        {
            StringBuilder sbCol = new System.Text.StringBuilder();
            StringBuilder sbVal = new System.Text.StringBuilder();

            foreach (KeyValuePair<string, object> kv in dic)
            {
                if (sbCol.Length == 0)
                {
                    sbCol.Append("insert into ");
                    sbCol.Append(tableName);
                    sbCol.Append("(");
                }
                else
                {
                    sbCol.Append(",");
                }

                sbCol.Append("`");
                sbCol.Append(kv.Key);
                sbCol.Append("`");

                if (sbVal.Length == 0)
                {
                    sbVal.Append(" values(");
                }
                else
                {
                    sbVal.Append(", ");
                }

                sbVal.Append("@v");
                sbVal.Append(kv.Key);
            }

            sbCol.Append(") ");
            sbVal.Append(");");

            cmd.CommandText = sbCol.ToString() + sbVal.ToString();

            foreach (KeyValuePair<string, object> kv in dic)
            {
                cmd.Parameters.AddWithValue("@v" + kv.Key, kv.Value);
            }

            cmd.ExecuteNonQuery();
        }

        public void Update(string tableName, Dictionary<string, object> dicData, string colCond, object varCond)
        {
            Dictionary<string, object> dic = new Dictionary<string, object>();
            dic[colCond] = varCond;
            Update(tableName, dicData, dic);
        }

        public void Update(string tableName, Dictionary<string, object> dicData, Dictionary<string, object> dicCond)
        {
            if (dicData.Count == 0)
                throw new Exception("dicData is empty.");

            StringBuilder sbData = new System.Text.StringBuilder();

            Dictionary<string, object> _dicTypeSource = new Dictionary<string, object>();

            foreach (KeyValuePair<string, object> kv1 in dicData)
            {
                _dicTypeSource[kv1.Key] = null;
            }

            foreach (KeyValuePair<string, object> kv2 in dicCond)
            {
                if (!_dicTypeSource.ContainsKey(kv2.Key))
                    _dicTypeSource[kv2.Key] = null;
            }

            sbData.Append("update `");
            sbData.Append(tableName);
            sbData.Append("` set ");

            bool firstRecord = true;

            foreach (KeyValuePair<string, object> kv in dicData)
            {
                if (firstRecord)
                    firstRecord = false;
                else
                    sbData.Append(",");

                sbData.Append("`");
                sbData.Append(kv.Key);
                sbData.Append("` = ");

                sbData.Append("@v");
                sbData.Append(kv.Key);
            }

            sbData.Append(" where ");

            firstRecord = true;

            foreach (KeyValuePair<string, object> kv in dicCond)
            {
                if (firstRecord)
                    firstRecord = false;
                else
                {
                    sbData.Append(" and ");
                }

                sbData.Append("`");
                sbData.Append(kv.Key);
                sbData.Append("` = ");

                sbData.Append("@c");
                sbData.Append(kv.Key);
            }

            sbData.Append(";");

            cmd.CommandText = sbData.ToString();

            foreach (KeyValuePair<string, object> kv in dicData)
            {
                cmd.Parameters.AddWithValue("@v" + kv.Key, kv.Value);
            }

            foreach (KeyValuePair<string, object> kv in dicCond)
            {
                cmd.Parameters.AddWithValue("@c" + kv.Key, kv.Value);
            }

            cmd.ExecuteNonQuery();
        }

        public long LastInsertRowId()
        {
            return ExecuteScalar<long>("select last_insert_rowid();");
        }

        #endregion

        #region Utilities

        public void CreateTable(SQLiteTable table)
        {
            StringBuilder sb = new Text.StringBuilder();
            sb.Append("create table if not exists `");
            sb.Append(table.TableName);
            sb.AppendLine("`(");

            bool firstRecord = true;

            foreach (SQLiteColumn col in table.Columns)
            {
                if (col.ColumnName.Trim().Length == 0)
                {
                    throw new Exception("Column name cannot be blank.");
                }

                if (firstRecord)
                    firstRecord = false;
                else
                    sb.AppendLine(",");

                sb.Append(col.ColumnName);
                sb.Append(" ");

                if (col.AutoIncrement)
                {

                    sb.Append("integer primary key autoincrement");
                    continue;
                }

                switch (col.ColDataType)
                {
                    case ColType.Text:
                        sb.Append("text"); break;
                    case ColType.Integer:
                        sb.Append("integer"); break;
                    case ColType.Decimal:
                        sb.Append("decimal"); break;
                    case ColType.DateTime:
                        sb.Append("datetime"); break;
                    case ColType.BLOB:
                        sb.Append("blob"); break;
                }

                if (col.PrimaryKey)
                    sb.Append(" primary key");
                else if (col.NotNull)
                    sb.Append(" not null");
                else if (col.DefaultValue.Length > 0)
                {
                    sb.Append(" default ");

                    if (col.DefaultValue.Contains(" ") || col.ColDataType == ColType.Text || col.ColDataType == ColType.DateTime)
                    {
                        sb.Append("'");
                        sb.Append(col.DefaultValue);
                        sb.Append("'");
                    }
                    else
                    {
                        sb.Append(col.DefaultValue);
                    }
                }
            }

            sb.AppendLine(");");

            cmd.CommandText = sb.ToString();
            cmd.ExecuteNonQuery();
        }

        public void RenameTable(string tableFrom, string tableTo)
        {
            cmd.CommandText = string.Format("alter table `{0}` rename to `{1}`;", tableFrom, tableTo);
            cmd.ExecuteNonQuery();
        }

        public void CopyAllData(string tableFrom, string tableTo)
        {
            DataTable dt1 = Select(string.Format("select * from `{0}` where 1 = 2;", tableFrom));
            DataTable dt2 = Select(string.Format("select * from `{0}` where 1 = 2;", tableTo));

            Dictionary<string, bool> dic = new Dictionary<string, bool>();

            foreach (DataColumn dc in dt1.Columns)
            {
                if (dt2.Columns.Contains(dc.ColumnName))
                {
                    if (!dic.ContainsKey(dc.ColumnName))
                    {
                        dic[dc.ColumnName] = true;
                    }
                }
            }

            foreach (DataColumn dc in dt2.Columns)
            {
                if (dt1.Columns.Contains(dc.ColumnName))
                {
                    if (!dic.ContainsKey(dc.ColumnName))
                    {
                        dic[dc.ColumnName] = true;
                    }
                }
            }

            StringBuilder sb = new Text.StringBuilder();

            foreach (KeyValuePair<string, bool> kv in dic)
            {
                if (sb.Length > 0)
                    sb.Append(",");

                sb.Append("`");
                sb.Append(kv.Key);
                sb.Append("`");
            }

            StringBuilder sb2 = new Text.StringBuilder();
            sb2.Append("insert into `");
            sb2.Append(tableTo);
            sb2.Append("`(");
            sb2.Append(sb.ToString());
            sb2.Append(") select ");
            sb2.Append(sb.ToString());
            sb2.Append(" from `");
            sb2.Append(tableFrom);
            sb2.Append("`;");

            cmd.CommandText = sb2.ToString();
            cmd.ExecuteNonQuery();
        }

        public void DropTable(string table)
        {
            cmd.CommandText = string.Format("drop table if exists `{0}`", table);
            cmd.ExecuteNonQuery();
        }

        public void UpdateTableStructure(string targetTable, SQLiteTable newStructure)
        {
            newStructure.TableName = targetTable + "_temp";

            CreateTable(newStructure);

            CopyAllData(targetTable, newStructure.TableName);

            DropTable(targetTable);

            RenameTable(newStructure.TableName, targetTable);
        }

        public void AttachDatabase(string database, string alias)
        {
            Execute(string.Format("attach '{0}' as {1};", database, alias));
        }

        public void DetachDatabase(string alias)
        {
            Execute(string.Format("detach {0};", alias));
        }

        #endregion

    }
}