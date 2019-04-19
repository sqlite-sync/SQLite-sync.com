using System;
using System.Collections.Generic;
using System.Text;

namespace System.Data.SQLite
{

    public class SQLiteColumnList : IList<SQLiteColumn>
    {
        List<SQLiteColumn> _lst = new List<SQLiteColumn>();

        private void CheckColumnName(string colName)
        {
            for (int i = 0; i < _lst.Count; i++)
            {
                if (_lst[i].ColumnName == colName)
                    throw new Exception("Column name of \"" + colName + "\" is already existed.");
            }
        }

        public int IndexOf(SQLiteColumn item)
        {
            return _lst.IndexOf(item);
        }

        public void Insert(int index, SQLiteColumn item)
        {
            CheckColumnName(item.ColumnName);

            _lst.Insert(index, item);
        }

        public void RemoveAt(int index)
        {
            _lst.RemoveAt(index);
        }

        public SQLiteColumn this[int index]
        {
            get
            {
                return _lst[index];
            }
            set
            {
                if (_lst[index].ColumnName != value.ColumnName)
                {
                    CheckColumnName(value.ColumnName);
                }

                _lst[index] = value;
            }
        }

        public void Add(SQLiteColumn item)
        {
            CheckColumnName(item.ColumnName);

            _lst.Add(item);
        }

        public void Clear()
        {
            _lst.Clear();
        }

        public bool Contains(SQLiteColumn item)
        {
            return _lst.Contains(item);
        }

        public void CopyTo(SQLiteColumn[] array, int arrayIndex)
        {
            _lst.CopyTo(array, arrayIndex);
        }

        public int Count
        {
            get { return _lst.Count; }
        }

        public bool IsReadOnly
        {
            get { return false; }
        }

        public bool Remove(SQLiteColumn item)
        {
            return _lst.Remove(item);
        }

        public IEnumerator<SQLiteColumn> GetEnumerator()
        {
            return _lst.GetEnumerator();
        }

        Collections.IEnumerator Collections.IEnumerable.GetEnumerator()
        {
            return _lst.GetEnumerator();
        }
    }

}
